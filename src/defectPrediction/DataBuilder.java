package defectPrediction;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;

public class DataBuilder {
	public static Instances mergeData(Instances data1, Instances data2) {
		Instances res = new Instances(data1);
		for (int i = 0; i < data2.numInstances(); i++) {
			Instance ins = data2.instance(i);			
			res.add(ins);
		}
		return res;
	}
	
	public static Instances copyData(Instances data) {
		Instances res = new Instances(data);
		res.delete();
		for (int i = 0; i < data.numInstances(); i++) {
			Instance ins = data.instance(i);
			Instance insCopy = new Instance(ins);
			res.add(insCopy);
		}
		return res;
	}
	
	public static int[] mergeData(Instances data1, Instances data2, int[] indexes) {
		int[] resIndexes = new int[data1.numInstances() + data2.numInstances()];
		for (int i = 0; i < data1.numInstances(); i++) {
			resIndexes[i] = indexes == null ? i : indexes[i];
		}
		int count = data1.numInstances() - 1;
		for (int i = 0; i < data2.numInstances(); i++) {
			Instance instance = data2.instance(i);
			data1.add(instance);
			Attribute fileName = data1.attribute(0);
			resIndexes[count++] = fileName.addStringValue(instance.stringValue(0));
		}
		return resIndexes;
	}
	
	public static Instances mergeAllData(Instances[] data) {
		if (data == null || data.length == 0) {
			return null;
		}
		Instances res = new Instances(data[0]);
		for (int i = 1; i < data.length; i++) {
			res = mergeData(res, data[i]);
		}
		return res;
	}
	
	public static Instances readData(String filepath) {
		Instances res = null;
		try {
			BufferedReader reader = new BufferedReader(new FileReader(filepath));
			res = new Instances(reader);
			reader.close();
			res.setClassIndex(res.numAttributes() - 1);	
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return res;
	}
	
	public static Instances filterConflictInstance(Instances data, int[] indexes, boolean leaveOne) {
		Instances res = new Instances(data);
		res.delete();
		Map<String, List<Instance>> dataMap = new HashMap<>();
		for (int i = 0; i < data.numInstances(); i++) {
			Instance ins = data.instance(i);
			Attribute filename = data.attribute(0);
			String fileStr = filename.value(indexes[i]);
			if (!dataMap.containsKey(fileStr)) {
				List<Instance> value = new ArrayList<>();
				value.add(ins);			
				dataMap.put(fileStr, value);
			}else {
				dataMap.get(fileStr).add(ins);
			}
		}
		for (List<Instance> value : dataMap.values()) {
			if (value.size() == 1) {
				res.add(value.get(0));
			}else {
				if (leaveOne) {
					res.add(selectInstance(value));
				}else {
					List<Instance> list = selectInstance2(value);
					for (Instance ins : list) {
						res.add(ins);
					}
				}
				
			}
		}
		return res;
	}
	
	public static Instance selectInstance(List<Instance> instances) {
		int buggyCount = 0;
		Instance buggyIns = null;
		Instance cleanIns = null;
		for (Instance ins : instances) {
			Attribute cls = ins.classAttribute();
			String classStr = ins.stringValue(cls);
			if ("1".equals(classStr)) {
				buggyCount += 1;
				buggyIns = buggyIns == null ? ins : buggyIns;
			}else {
				cleanIns = cleanIns == null ? ins : cleanIns;
			}
		}
		int cleanCount = instances.size() - buggyCount;
		Instance res = buggyCount >= cleanCount ? buggyIns : cleanIns;
		return res;
	}
	
	public static List<Instance> selectInstance2(List<Instance> instances) {
		int buggyCount = 0;
		Instance buggyIns = null;
		Instance cleanIns = null;
		for (Instance ins : instances) {
			Attribute cls = ins.classAttribute();
			String classStr = ins.stringValue(cls);
			if ("1".equals(classStr)) {
				buggyCount += 1;
				buggyIns = buggyIns == null ? ins : buggyIns;
			}else {
				cleanIns = cleanIns == null ? ins : cleanIns;
			}
		}
		int cleanCount = instances.size() - buggyCount;
		List<Instance> res = new ArrayList<>();
		for (Instance instance : instances) {
			Instance newIns = new Instance(instance);
			newIns.setDataset(instance.dataset());
			if (buggyCount >= cleanCount) {
				newIns.setClassValue("1");
			}else {
				newIns.setClassValue("0");
			}
		}
		return res;
	}	
	
	public static void writeArffFile(Instances data, String path) {
		ArffSaver saver = new ArffSaver();
		saver.setInstances(data);
		try {
			saver.setFile(new File(path));
			saver.writeBatch();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public static double defectRate(Instances data) {
		int count = 0;
		for (int i = 0; i < data.numInstances(); i++) {
			Instance instance = data.instance(i);
			Attribute attr = instance.classAttribute();
			String classStr = instance.stringValue(attr);
			if (classStr.equals("1")) {
				count++;
			}
		}
		return (double)count / (double)data.numInstances();
	}
	
	public static void main(String[] args) {
		String pre = "E:/data/metric_arff1/jmeter/jmeter_metrics_";
		String[] files = {
				pre + "2.5.arff", pre + "2.6.arff", pre + "2.7.arff", pre + "2.8.arff",
				pre + "2.9.arff", pre + "2.10.arff", pre + "2.11.arff", pre + "2.12.arff"
		};
		Instances[] data = new Instances[files.length];
		for (int i = 0; i < data.length; i++) {
			data[i] = DataBuilder.readData(files[i]);
			System.out.println(DataBuilder.defectRate(data[i]));
		}
		
//		int[] index = null;
//		for (int i = 1; i < data.length-1; i++) {
//			System.out.println("ins num pre: " + data[0].numInstances());
//			index = mergeData(data[0], data[i], index);
//			System.out.println("ins num after: " + data[0].numInstances());
//			filterConflictInstance(data[0], index, false);
//		}
		
	}
	
	
}
