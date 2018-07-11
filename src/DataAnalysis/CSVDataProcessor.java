package DataAnalysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import gitDataProcess.Util;

public class CSVDataProcessor {
	public static final String dirPath = "E:/data/dataset/tera/";
	
	public static List<String> getVerFileList(String name) {
		List<String> files = new LinkedList<String>();
		String arffDirPath = dirPath + name + "/";
		File dir = new File(arffDirPath);
		if (!dir.exists() || !dir.isDirectory()) {
			System.out.println("directory error!");
			return files;
		}
		for (File subFile : dir.listFiles()) {
			String fileName = subFile.getName();
			if (fileName.endsWith(".csv")) {
				fileName = dirPath + name + "/" + fileName;
				files.add(fileName);
			}
		}
		Util.sortFileName(files);
		return files;
	}
	
	public static String simpledCSVFilePath(String filePath) {
		String[] splited = filePath.split("/");
		String shortName = splited[splited.length-1];
		String software = splited[splited.length-2];
		String res = dirPath + software +  "/simpled/" + shortName;
		return res;
	}
	
	public static void filterData(String filename) {
		BufferedReader reader = null;
		List<String> content = new ArrayList<String>();
		try {
			reader = new BufferedReader(new FileReader(filename));
			String line = reader.readLine().trim();
			line = filterLine(line, true);
			content.add(line);
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if ("".equals(line)) {
					continue;
				}
				line = filterLine(line, false);
				content.add(line);
			}
			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String outFilePath = simpledCSVFilePath(filename);
		Util.writeFile(outFilePath, content);	
	}
	
	private static String filterLine(String line, boolean isTitle) {	
		String[] parts = line.split(",");		
		int len = parts.length;
		if (!isTitle && !"0".equals(parts[len-1])) {
			parts[len-1] = "1";
		}
		String res = parts[2];
		for (int i = 3; i < len; i++) {
			res += "," + parts[i];
		}
		return res;	
	}
	
	public static String[] softwares = {
			"camel", "ivy", "jedit", "log4j", "lucene", "poi", "prop", 
			"synapse", "velocity", "xalan", "xerces"
	};
	
	public static void main(String[] args) {
		for (String name : softwares) {
			List<String> files = getVerFileList(name);
			for (String filename : files) {
				filterData(filename);
			}
		}
	}
	
}
