package DataAnalysis;

import java.util.ArrayList;
import java.util.List;

import defectPrediction.ClassifiersRunner;
import defectPrediction.DataBuilder;
import defectPrediction.FeatureSelection;
import defectPrediction.GeneralClassifier;
import defectPrediction.Rebalance;
import defectPrediction.ResultEvaluator;
import gitDataProcess.Util;
import weka.classifiers.Evaluation;
import weka.core.Instances;

public class SplitedDataAnalysis {
	
//	public static String dirPath = "E:/data/metric-arff1/";
//	public static String dirPath = "E:/data/dataset/tera/";
	public static String dirPath = "C:/Users/1/Desktop/dataset/tera/";

	
	private Instances[] allVerData;
	private Instances[] newVerData;
	private Instances[] existVerData;
	private List<String> files;
	private String name;
	
	public SplitedDataAnalysis(String name) {
		this.name = name;
		files =  DataSpiliter.getVerFileList(name);
		Util.sortFileName(files);
		allVerData = new Instances[files.size()];
		newVerData = new Instances[files.size()];
		existVerData = new Instances[files.size()];
		for (int i = 0; i < allVerData.length; i++) {
			allVerData[i] = DataBuilder.readData(files.get(i));
			allVerData[i]= FeatureSelection.delFilename(allVerData[i]);
			if (i == 0) {
				continue;
			}
			String[] outPath = DataSpiliter.outPath(name, files.get(i));
			newVerData[i] = DataBuilder.readData(outPath[0]);
			newVerData[i] = FeatureSelection.delFilename(newVerData[i]); 
			existVerData[i] = DataBuilder.readData(outPath[1]);
			existVerData[i] = FeatureSelection.delFilename(existVerData[i]);
 		}
	}
	
	public void analyseVersionData() {
		for (int i = 1; i < files.size(); i++) {
			String verPath = files.get(i);
			String tag = Util.extractTag2(verPath);
			System.out.println("------" + tag + "------");
			dataAnalysis(allVerData[i], newVerData[i], existVerData[i]);
			
		}
	}
	
	public void dataAnalysis(Instances data, Instances newFiles, Instances existFiles) {
		System.out.println("total file num: " + data.numInstances());
		System.out.println("new file num: " + newFiles.numInstances());
		System.out.println("exist file num:" + existFiles.numInstances());
		double tatolDefectRate = DataBuilder.defectRate(data);
		double newDefectRate = DataBuilder.defectRate(newFiles);
		double existDefectRate = DataBuilder.defectRate(existFiles);
		System.out.println("tatal file defect rate: " + tatolDefectRate);
		System.out.println("new file defect rate: " + newDefectRate);
		System.out.println("exist file defect rate: " + existDefectRate);
	}
	
	public void runAllCrossVersionPrediction(String model, String featureSelection, 
			String rebalance, String[] options) {			
		int verNum = files.size();		
		Evaluation[][] evals = new Evaluation[verNum-1][verNum-1];
		Evaluation[][] newEvals = new Evaluation[verNum-1][verNum-1];
		Evaluation[][] existEvals = new Evaluation[verNum-1][verNum-1];
		for (int sourIndex = 0; sourIndex < verNum - 1; sourIndex++) {
			//read data 
			Instances trainData = allVerData[sourIndex];			
			//select features
			int[] features = FeatureSelection.featureSelection(trainData, featureSelection);
			trainData = FeatureSelection.filterData(trainData, features);				
			//build a classifier   
			GeneralClassifier classifier = new GeneralClassifier(trainData, model, options);		
			//evaluate on several version test data
			for (int targIndex = sourIndex + 1; targIndex < verNum; targIndex++) {
				//filter data
				Instances testData = allVerData[targIndex];
				testData = FeatureSelection.filterData(testData, features);
				Instances testDataNew = newVerData[targIndex];
				testDataNew = FeatureSelection.filterData(testDataNew, features);
				Instances testDataExist = existVerData[targIndex];
				testDataExist = FeatureSelection.filterData(testDataExist, features);				
				evals[sourIndex][targIndex-1] = classifier.evalutate(testData);
				newEvals[sourIndex][targIndex-1] = classifier.evalutate(testDataNew);
				existEvals[sourIndex][targIndex-1] = classifier.evalutate(testDataExist);	
			}
		}
		Instances merged = new Instances(allVerData[0]);
		for (int index = 1; index < allVerData.length-1; index++) {
			//merged data as train dataset
			merged = DataBuilder.mergeData(merged, allVerData[index]);
			int[] features = FeatureSelection.featureSelection(merged, featureSelection);
			Instances trainData = FeatureSelection.filterData(merged, features); 
			GeneralClassifier classifier = new GeneralClassifier(trainData, model, options);
			//test all data
			Instances testData = allVerData[index+1];
			testData = FeatureSelection.filterData(testData, features);
			Evaluation eval = classifier.evalutate(testData);
			evals[evals.length-1][index] = eval;
			//test new files
			Instances testDataNew = newVerData[index+1];
			testData = FeatureSelection.filterData(testDataNew, features);
			Evaluation eval2 = classifier.evalutate(testDataNew);
			newEvals[evals.length-1][index] = eval2;
			//test exist files
			Instances testDataExist = existVerData[index+1];
			testDataExist = FeatureSelection.filterData(testDataExist, features);
			Evaluation eval3 = classifier.evalutate(testDataExist);
			existEvals[evals.length-1][index] = eval3;
			
		}
		formatResult(evals, "", name, model, featureSelection, rebalance, files, options, null);
		formatResult(newEvals, "-new", name, model, featureSelection, rebalance, files, options, null);
		formatResult(existEvals, "-exist", name, model, featureSelection, rebalance, files, options, null);
	}
	
	
	private static void formatResult(Evaluation[][] evals, String tag, String name, String model, 
			String featureSelection, String rebalance, List<String> files, String[] options, String[][] bestParams) {
		int rowNum = evals.length;
		int colNum = evals[0].length;
		Double[][] bugPrec = new Double[rowNum][colNum];
		Double[][] bugRecall = new Double[rowNum][colNum];
		Double[][] bugF = new Double[rowNum][colNum];
		Double[][] gMean = new Double[rowNum][colNum];
		Double[][] balance = new Double[rowNum][colNum];
		List<String> allResults = new ArrayList<>();
		List<String> tags = Util.extractTags2(files);
		tags.add("allData");
		if (options != null) {
			String paramStr = ClassifiersRunner.paramStr(options);
			allResults.add(paramStr);
		}
 		for (int sourIndex = 0; sourIndex < rowNum; sourIndex++) {
			for (int targIndex = 0; targIndex < colNum; targIndex++) {
				if (sourIndex <= targIndex) {
					Evaluation eval = evals[sourIndex][targIndex];
					if (eval == null) {
						continue;
					}
					String resultTitle = "=== " + name + "  " + tags.get(sourIndex) + "-->" + tags.get(targIndex) + " ===";;
					String resultStr = GeneralClassifier.toDetailResult(eval, resultTitle);
					//result process
					allResults.add(resultStr);
					bugPrec[sourIndex][targIndex] = eval.precision(0);
					bugRecall[sourIndex][targIndex] = eval.recall(0);
					bugF[sourIndex][targIndex] = eval.fMeasure(0);
					double tp = eval.numTruePositives(0);
					double tn = eval.numTrueNegatives(0);
					double fp = eval.numFalsePositives(0);
					double fn = eval.numFalseNegatives(0);
					gMean[sourIndex][targIndex] = ResultEvaluator.gMean(tp, tn, fp, fn);
					balance[sourIndex][targIndex] = ResultEvaluator.balance(tp, tn, fp, fn);
				}
			}
		}
		writeResults(name, model, featureSelection, tags, 
				bugPrec, bugRecall, bugF, gMean, balance, allResults, options, bestParams, tag);
	}
	
	private static void writeResults(String name, String model, String featureSelection, List<String> tags,
			Double[][] bugPrec, Double[][] bugRecall, Double[][] bugF, Double[][] gMean, Double[][] balance,
			List<String> allResults, String[] params, String[][] bestParams, String tag) {
		String detailResultPath = getFilePath(name, "detail", model, featureSelection, null, params, bestParams == null ? false : true, tag);
		String summaryResultPath = getFilePath(name, "summary", model, featureSelection, null, params, bestParams == null ? false : true, tag);
		Util.writeFile(detailResultPath, allResults);
		List<String> tables = new ArrayList<>();		
		tables.add(ClassifiersRunner.markdownTableFormatDouble(bugPrec, tags, "Precision", null));
		tables.add(ClassifiersRunner.markdownTableFormatDouble(bugRecall, tags, "Recall", null));
		tables.add(ClassifiersRunner.markdownTableFormatDouble(bugF, tags, "Fmeasure", null));
		tables.add(ClassifiersRunner.markdownTableFormatDouble(gMean, tags, "gMean", null));
		tables.add(ClassifiersRunner.markdownTableFormatDouble(balance, tags, "balance", null));
		if (bestParams != null) {
			tables.add(ClassifiersRunner.markdownTableFormatStr(bestParams, tags, "bestParams"));
		}
		Util.writeFile(summaryResultPath, tables);
	}
	
	public static String getFilePath(String name, String type, String model, String fSelection, String rebalance, 
			String[] params, boolean isBest, String tag) {
		String fs = fSelection == null ? "" : fSelection;
		String rb = rebalance == null ? "" : rebalance;
		String modelStr = "-" + model;
		//feature string 
		switch (fs) {
			case FeatureSelection.CFS_ATTRIBUTE_SELECTION:{
				fs = "-cfs";
				break;
			}case FeatureSelection.FILTER_ATTRIBUTE_SELECTION: {
				fs = "-filter";
				break;
			}default: {
				fs = "";
				break;
			}
		}
		//rebalance string
		switch (rb) {
			case Rebalance.REBALANCE_SMOTE: {
				rb = "-smote";
				break;
			}default: {
				rb = "";
				break;
			}	
		}
		String dir = dirPath + name + "/simpled/splited/";
		String paramStr = "";
		if (params != null) {
			dir += "params-out/";
			paramStr = ClassifiersRunner.paramStr(params);
		}
		String bestStr = "";
		if (isBest) {
			bestStr = "-BEST";
		}
		String res = dir + name +  "-" + type + modelStr + fs + rb + paramStr + bestStr + tag + ".txt";
		return res; 
	}
	
	public static String[] models = {
			GeneralClassifier.CLASSIFIER_RANDOM_FOREST,
//			GeneralClassifier.CLASSIFIER_NAIVE_BAYES,
//			GeneralClassifier.CLASSIFIER_LOGISTIC,
//			GeneralClassifier.CLASSIFIER_SVM
		};
		public static String[] attrSele = {
			null,
//			FeatureSelection.CFS_ATTRIBUTE_SELECTION,
//			FeatureSelection.CLASSIFIER_ATTRIBUTE_SELECTION,
//			FeatureSelection.FILTER_ATTRIBUTE_SELECTION
		}; 
	
		public static String[] softwares = {
				"xalan", 
				"jmeter", 
				"camel", 
				"celery", 
				"kivy", "tensorflow", "zulip",						
//				"geode", "beam","cloudstack", "isis", 			
//				"okhttp", "mahout"
		};

	public static String[] tera = {
			"camel", "ivy", "jedit", "log4j", "lucene", "poi", 
			//"prop", 
			"synapse", "velocity", "xalan", "xerces"
	};
	
	public static void runner() {
		for (String name : tera) {
			System.out.println("=========" + name + "==========");
			SplitedDataAnalysis sda = new SplitedDataAnalysis(name);
			sda.analyseVersionData();
			for (String model : models) {
				for (String attr : attrSele) {
					sda.runAllCrossVersionPrediction(model, attr, null, null);
				}
			}
		}
	}
	
	public static void main(String[] args) {
		runner();
	}
}
