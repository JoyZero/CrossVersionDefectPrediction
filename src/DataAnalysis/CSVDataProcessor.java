package DataAnalysis;

import java.io.File;
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
				files.add(fileName);
			}
		}
		Util.sortFileName(files);
		return files;
	}
	
	public static void filterData(String filename) {
		
	}
	
}
