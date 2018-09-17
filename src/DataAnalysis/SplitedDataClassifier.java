package DataAnalysis;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;

import defectPrediction.ClassifiersRunner;
import defectPrediction.DataBuilder;
import defectPrediction.FeatureSelection;
import defectPrediction.GeneralClassifier;
import defectPrediction.Rebalance;
import defectPrediction.ResultEvaluator;
import gitDataProcess.Util;
import weka.classifiers.Evaluation;
import weka.core.Instance;
import weka.core.Instances;

public class SplitedDataClassifier {
//	public static String dirPath = "E:/data/metric-arff1/";
	public static String dirPath = "E:/data/dataset/tera/";
//	public static String dirPath = "C:/Users/1/Desktop/dataset/tera/";

	
	private Instances[] allVerData;
	private Instances[] newVerData;
	private Instances[] existVerData;
	private List<String> files;
	private String name;
	
	public SplitedDataClassifier(String name) {
		this.name = name;
		files = DataSpiliter.getVerFileList(name);
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
		List<Double> weights = getWeights(name);
		for (double weight : weights) {
			System.out.print(weight + " ");
		}
		System.out.println();
		GeneralClassifier[] classifiers = new GeneralClassifier[verNum-1];
		for (int sourIndex = 0; sourIndex < verNum - 1; sourIndex++) {
			//read data 
			Instances trainData = allVerData[sourIndex];							
			//build a classifier   
			classifiers[sourIndex] = new GeneralClassifier(trainData, model, options);		
			//evaluate on several version test data			
		}
		Instances testData = newVerData[verNum-1];
		
		double truePos = 0, trueNeg = 0, falsePos = 0, falseNeg = 0;
		for (int i = 0; i < testData.numInstances(); i++) {
			Instance instance = testData.instance(i);
			double weightedSum = 0;
			for (int j = 0; j < verNum - 1; j ++) {				
				double res = classifiers[j].classifyInstance(instance);
				//System.out.println("res: " + res + "  weight: " + weights.get(j));
				if (res == 0) {
					weightedSum += weights.get(j);
				}else if (res == 1) {
					weightedSum -= weights.get(j);
				}else {
					System.out.println("error!! ");
				}
			}
			//System.out.println(weightedSum);			
			double cls = weightedSum > 0 ? 1 : 0;
			double truth = instance.classValue();
			truth = truth == 0 ? 1 : 0;
			//System.out.println("classValue: " + truth);
			if (truth == 1) {
				if (cls == 1) {
					truePos += 1;
				}else if (cls == 0) {
					falseNeg += 1;
				}else {
					System.out.println("error!  1");
				}
			}else if (truth == 0) {
				if (cls == 0) {
					trueNeg += 1;
				}else if (cls == 1) {
					falsePos += 1;
				}else {
					System.out.println("error!   2");
				}
			}else {
				System.out.println("error!    3");
			}
		}
		double precision = ResultEvaluator.precision(truePos, trueNeg, falsePos, falseNeg);
		double recall = ResultEvaluator.recall(truePos, trueNeg, falsePos, falseNeg);
		double fmeasure = ResultEvaluator.fmeasure(truePos, trueNeg, falsePos, falseNeg);
		double gmean = ResultEvaluator.gMean(truePos, trueNeg, falsePos, falseNeg);
		double balance = ResultEvaluator.balance(truePos, trueNeg, falsePos, falseNeg);
		System.out.println(truePos + "  " + falsePos + "  " + trueNeg + "  " + falseNeg);
		System.out.println(precision);
		System.out.println(recall);
		System.out.println(fmeasure);
		System.out.println(gmean);
		System.out.println(balance);
		writeRes(model, truePos, trueNeg, falsePos, falseNeg);
		
		Instances merged = new Instances(allVerData[0]);
		for (int index = 1; index < allVerData.length-1; index++) {
			//merged data as train dataset
			merged = DataBuilder.mergeData(merged, allVerData[index]);						
		}
		int[] features = FeatureSelection.featureSelection(merged, featureSelection);
		Instances trainData = FeatureSelection.filterData(merged, features); 
		GeneralClassifier classifier = new GeneralClassifier(trainData, model, options);
		Evaluation eval = classifier.evalutate(testData);
		double tp = eval.numTruePositives(0);
		double tn = eval.numTrueNegatives(0);
		double fp = eval.numFalsePositives(0);
		double fn = eval.numFalseNegatives(0);
		writeRes2(model, tp, tn, fp, fn);
	}
	
	public void writeRes(String model, double truePos, double trueNeg, double falsePos, double falseNeg) {
		String filePath = dirPath + name + "/" + name + "-newRes-" + model + ".txt";
		String line = truePos + "," + falsePos + "," + trueNeg + "," + falseNeg;
		List<String> content = new ArrayList<>();
		content.add(line);
		Util.writeFile(filePath, content);
	}
	
	public void writeRes2(String model, double truePos, double trueNeg, double falsePos, double falseNeg) {
		String filePath = dirPath + name + "/" + name + "-newRes-" + model + "2.txt";
		String line = truePos + "," + falsePos + "," + trueNeg + "," + falseNeg;
		List<String> content = new ArrayList<>();
		content.add(line);
		Util.writeFile(filePath, content);
	}
	
	
	public static List<Double> getWeights(String name) {
		String dir = "C:/Users/Joey/Desktop/tera2/";
		String path = dir + name + "/" + name + "-w.txt";
		BufferedReader reader = null;
		List<Double> weights = new ArrayList<>();
		try {
			reader = new BufferedReader(new FileReader(path));
			String line = reader.readLine().trim();
			String[] parts = line.split(",");
			for (int i = 0; i < parts.length; i++) {
				weights.add(Double.parseDouble(parts[i]));
			}
			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return weights;
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
			GeneralClassifier.CLASSIFIER_NAIVE_BAYES,
			GeneralClassifier.CLASSIFIER_LOGISTIC,
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
			"camel", 
			//"ivy", 
			"jedit", "log4j", "lucene", "poi", 
			//"prop", 
			"synapse", "velocity", "xalan", "xerces"
	};
	
	public static void runner() {
		for (String name : tera) {
			System.out.println("=========" + name + "==========");
			SplitedDataClassifier sdc = new SplitedDataClassifier(name);
			//sdc.analyseVersionData();
			for (String model : models) {
				for (String attr : attrSele) {
					sdc.runAllCrossVersionPrediction(model, attr, null, null);
				}
			}
		}
	}
	
	public static void main(String[] args) {
		runner();
	}
}
