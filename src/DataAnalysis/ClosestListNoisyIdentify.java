package DataAnalysis;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import defectPrediction.ClassifiersRunner;
import defectPrediction.DataBuilder;
import defectPrediction.FeatureSelection;
import gitDataProcess.Util;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

public class ClosestListNoisyIdentify {
	private Instances data;
	private double[][] distMatric;
	private int n; //num of top similar instances
	private double rate; //the conflict rate for change label
	private double stopSim;
	private String outPath;
			
	public ClosestListNoisyIdentify(Instances data, String outPath) {
		this.data = data;
		this.outPath = outPath;
		int len = data.numInstances();
		if (data.numInstances() < 10000) {
			distMatric = new double[len][len];
			calcDistances();
		}
		n = 5;
		rate = 0.6;
		stopSim = 0.99;
	}
	
	public ClosestListNoisyIdentify(Instances data, String outPath, int n, double rate, double stopSim) {
		this.data = data;
		this.outPath = outPath;
		this.n = n;
		this.rate = rate;
		int len = data.numInstances();
		if (data.numInstances() < 10000) {
			distMatric = new double[len][len];
			calcDistances();
		}
		this.n = n;
		this.rate = rate;
		this.stopSim = stopSim;
		
	}
	
	private void calcDistances() {
		for (int i = 0; i < distMatric.length; i++) {
			for (int j = i+1; j < distMatric[0].length; j++) {
				distMatric[i][j] = euclideanDistance(data.instance(i), data.instance(j));
				distMatric[j][i] = distMatric[i][j];
			}
		}
	}
	
	private double getDistance(int i, int j) {
		Instance ins1 = data.instance(i);
		Instance ins2 = data.instance(j);
		if (distMatric == null) {
			return euclideanDistance(ins1, ins2);
		}else {
			return distMatric[i][j];
		}
	}
	
	public double euclideanDistance(Instance ins1, Instance ins2) {
		double res = 0;
		int attrNum = ins1.numAttributes();
		for (int i = 0; i < attrNum; i++) {
			if (ins1.attribute(i).type() != Attribute.NUMERIC || data.classIndex() == i) {
				break;
			}
			double diff = ins1.value(i) - ins2.value(i);
			res += diff * diff;		
		}
		res = Math.sqrt(res);
		return res;
	}
	
	private Set<Instance> identifyNoisyOneRound(Set<Instance> pre) {
		if (pre == null) {
			pre = new HashSet<>();
		}
		int num = data.numInstances();
		Set<Instance> noisyIns = new HashSet<>();
		for (int i = 0; i < num; i++) {
			Instance instance = data.instance(i);
			double[] dists = new double[num - pre.size()];
			int distsIndex = 0;
			for (int index = 0; index < num; index++) {
				if (index == i || pre.contains(data.instance(index))) {
					continue;
				}
				dists[distsIndex] =  getDistance(i, index);
			}
			Arrays.sort(dists);
			int bottomIndex = n >= dists.length ? dists.length : n;
			double bottomValue = dists[bottomIndex];
			int confCount = 0;
			int totalCount = 0;
			for (int j = 0; j < num; j++) {
				if (j == i || pre.contains(data.instance(j))) {
					continue;
				}
				double distance = getDistance(i, j);
				if (distance <= bottomValue) {
					totalCount++;
					if (instance.classValue() != data.instance(j).classValue()) {
						confCount++;
					}
				}
			}
			double confRate = (double)confCount / (double)totalCount;
			if (confRate >= rate) {
				noisyIns.add(instance);
			}
			noisyIns.addAll(pre);
		}
		return noisyIns;
	}
	
	public void identifyNoisyIter() {
		Set<Instance> resPre = null;
		Set<Instance> resCur = null;
		double simularity = 0;
		while (simularity < stopSim) {
			resPre = resCur;
			resCur = identifyNoisyOneRound(resPre);
			simularity = calSetSimularity(resPre, resCur);	
		}
		System.out.println("total size:" + data.numInstances());
		System.out.println("defect rate before change:" + DataBuilder.defectRate(data));
		for (Instance ins: resCur) {
			double classValue = ins.classValue();
			classValue = classValue == 0  ? 1 : 0;
			ins.setClassValue(classValue);
		}
		System.out.println("changed size:" + resCur.size());
		System.out.println("change rate: " + ((double)resCur.size()/(double)data.numInstances()));
		System.out.println("defect rate after change:" + DataBuilder.defectRate(data));
	}
	
	public void writeFile() {
		DataBuilder.writeArffFile(data, outPath);
	}
	
	public double calSetSimularity(Set<Instance> set1, Set<Instance> set2) {
		if (set1 == null || set2 == null) {
			return 0;
		}
		HashSet<Instance> join = (HashSet<Instance>)set1;
		join = (HashSet<Instance>)join.clone();
		join.retainAll(set2);
		int maxSize = set1.size() > set2.size() ? set1.size() : set2.size();		
		double res = (double)join.size() / (double)maxSize;
//		System.out.println("--------");
//		System.out.println("pre.size:" + set1.size());
//		System.out.println("cur.size:" + set2.size());
//		System.out.println("join.size:" + join.size());
		System.out.println("simularity:" + res);
		return res;
	}
	
	public static String[] getOutFilepath(String name, List<String> files) {
		String[] out = new String[files.size()];
		List<String> tags = files.stream()
				.map(filename -> filename.substring(0, filename.length() - 5).split("_")[2])
				.collect(Collectors.toList());
		for (int i = 1; i < files.size(); i++) {
			out[i] = ClassifiersRunner.dirPath + name + "/clni/" + name + "-" + tags.get(0) + "-" + tags.get(i) + ".arff";
		}		
		return out;
	}

	
	public static void runner(String name) {
		List<String> files = ClassifiersRunner.getVerFileList(name);
		System.out.println(files.size());
		Util.sortFileName(files);
		String[] outFilePath = getOutFilepath(name, files);
		Instances[] allVersData = new Instances[files.size()];
		System.out.println("==================" + name + "=============================");
		for (int i = 0; i < allVersData.length; i++) {
			System.out.println("---" + i + "---");
			String filename = ClassifiersRunner.dirPath + name + "/" + files.get(i);
			Instances verData = DataBuilder.readData(filename);
			allVersData[i] = FeatureSelection.delFilename(verData); 
			if (i > 0) {
				
				allVersData[i] = DataBuilder.mergeData(allVersData[i-1], allVersData[i]);
				Instances copy = DataBuilder.copyData(allVersData[i]);
				ClosestListNoisyIdentify clni = new ClosestListNoisyIdentify(copy, outFilePath[i]);
				clni.identifyNoisyIter();
				clni.writeFile();
				System.gc();
			}
		}
		
	}
	
	public static void main(String[] args) {
		String[] softwares = {
//				"xalan", 
//				"jmeter", 
				"camel", 
//				"celery", 
//				"kivy", 
//				"tensorflow", 
//				"zulip",						
//				"geode", "beam","cloudstack", "isis", 			
//				"okhttp", "mahout"
		};
		for (String name : softwares) {
			runner(name);	
		}
		
		
		
//		String pre = "E:/data/metric_arff1/jmeter/jmeter_metrics_";
//		String[] files = {
//				pre + "2.5.arff", pre + "2.6.arff", pre + "2.7.arff", pre + "2.8.arff",
//				pre + "2.9.arff",pre + "2.10.arff",pre + "2.11.arff",pre + "2.12.arff"
//		};
//		Instances[] data = new Instances[files.length];
//		for (int i = 0; i < data.length; i++) {
//			data[i] = DataBuilder.readData(files[i]);
//			System.out.println(DataBuilder.defectRate(data[i]));
//		}
		
//		System.out.println(data[0].numInstances());
//		int count = 0;
//		for (int i = 0; i < data[0].numInstances(); i++) {
//			Instance ins = data[0].instance(i);
//			if (ins.classValue() == 1) {
//				count++;
//			}
//		}
//		System.out.println(count);
//		Instances copy = DataBuilder.copyData(data[0]);
//		for (int i = 0; i < 100; i++) {
//			copy.instance(i).setClassValue(1);
//		}
//		int count1 = 0;
//		int count2 = 0;
//		for (int i = 0; i < data[0].numInstances(); i++) {
//			Instance ins = data[0].instance(i);
//			if (ins.classValue() == 1) {
//				count1++;
//			}
//			if (copy.instance(i).classValue() == 1) {
//				count2++;
//			}
//		}
//		System.out.println(count1);
//		System.out.println(count2);
	}
	
}
