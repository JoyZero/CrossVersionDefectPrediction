package DataAnalysis;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import defectPrediction.DataBuilder;
import defectPrediction.GeneralClassifier;
import gitDataProcess.Util;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

public class DataSpiliter {
	
	//public static String dirPath = "E:/data/metric-arff1/";
	public static String dirPath  = "E:/data/dataset/tera/";
	
	public static Instances emptyDataset(Instances data) {
		Instances res = new Instances(data);
		res.delete();
		return res;		
	}
	
	public static List<String> getVerFileList(String name) {
		List<String> files = new LinkedList<String>();
		String arffDirPath = dirPath + name + "/simpled/";
		File dir = new File(arffDirPath);
		if (!dir.exists() || !dir.isDirectory()) {
			System.out.println("directory error!");
			return files;
		}
		for (File subFile : dir.listFiles()) {
			String fileName = subFile.getName();
			if (fileName.endsWith(".arff")) {
				fileName = arffDirPath + fileName;
				files.add(fileName);
			}
		}
		return files;
	}
		
	public static Instances[] spilitData(Instances pre, Instances cur) {
		Instances newFiles = emptyDataset(cur);
		Instances existFiles = emptyDataset(cur);
		List<Instance> preList = new LinkedList<>();
		for (int i = 0; i < pre.numInstances(); i++) {
			preList.add(pre.instance(i));
		}
		for (int i = 0; i < cur.numInstances(); i++) {
			Instance instance = cur.instance(i);
			Attribute fileNameCur = cur.attribute(0);
			String fileNameCurStr = instance.stringValue(fileNameCur);
			boolean exist = false;
			for (int j = preList.size() - 1; j >= 0; j--) {
				Instance insPre = preList.get(j);
				Attribute fileNamePre = pre.attribute(0);
				String fileNamePreStr = insPre.stringValue(fileNamePre);
				if (fileNameCurStr.equals(fileNamePreStr)) {
					existFiles.add(instance);
					preList.remove(j);
					exist = true;
					break;
				}
			}
			if (!exist) {
				newFiles.add(instance);
			}
		}
		Instances[] res = {newFiles, existFiles};
		return res;
	}
	
	public static String[] outPath(String name, String filePath) {
		//String pre = dirPath + name + "/split/" + name + "_metrics_"; 
		String pre = dirPath + name + "/simpled/splited/" + name + "-";
		//String tag = Util.extractTag(filePath);
		String tag = Util.extractTag2(filePath);
		String newOut = pre + tag + "-newFiles.arff";
		String existOut = pre + tag + "-existFilse.arff";
		String[] res = {newOut, existOut};
		return res;
	}
	
	public static void splitMultiVersData(String name) {
		List<String> files =  getVerFileList(name);
		Util.sortFileName(files);
		Instances[] allVerData = new Instances[files.size()];
		for (int i = 0; i < allVerData.length; i++) {
			allVerData[i] = DataBuilder.readData(files.get(i)); 
			if (i >= 1) {
				Instances[] res = spilitData(allVerData[i-1], allVerData[i]);
				String[] outPath = outPath(name, files.get(i));
				System.out.println("---------" + files.get(i) + "-------");
				System.out.println("total size: " + allVerData[i].numInstances());
				System.out.println("new files: " + res[0].numInstances());
				System.out.println("exit files: " + res[1].numInstances());
				DataBuilder.writeArffFile(res[0], outPath[0]);
				DataBuilder.writeArffFile(res[1], outPath[1]);
			}
		}
	}

	public static String[] softwares = {
			"xalan", 
			"jmeter", 
			"camel", 
			"celery", 
			"kivy", "tensorflow", "zulip",						
//			"geode", "beam","cloudstack", "isis", 			
//			"okhttp", "mahout"
	};
	
	
	public static String[] tera = {
			"camel", "ivy", "jedit", "log4j", "lucene", "poi", 
			//"prop", 
			"synapse", "velocity", "xalan", "xerces"
	};

	
	public static void main(String[] args) {
		for (String name : tera) {
			System.out.println("=========================" + name + "=========================");
			splitMultiVersData(name);
		}
	}
	
}
