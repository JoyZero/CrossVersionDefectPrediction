package DataAnalysis;


import gitDataProcess.Util;
import sun.awt.image.ImageWatched;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;

public class CSVSpliter {
    public static String[] softwares = {
            "xalan",
            "jmeter",
            "camel",
            "celery",
            "kivy", "tensorflow", "zulip",
    };

    public static String dirPath = "C:/Users/1/Desktop/metrics_csv/";

    public static String titleLine = null;

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

    public static String[] getOutPath(String filepath) {
        String[] temp = filepath.split("/");
        String suffix = temp[temp.length-1];
        String pre = filepath.substring(0, filepath.length() - suffix.length());
        String str = pre + "/y/" + suffix;
        pre = str.substring(0, str.length() - 4);
        String[] res = new String[2];
        res[0] = pre + "-new.csv";
        res[1] = pre + "-exist.csv";
        return res;
    }


    /**
     * read content of the file,
     * return as LinkedHashMap format,
     * key is filename, value is whole line
     *
     * @param filePath
     * @return file content
     */
    public static LinkedHashMap<String, String> readData(String filePath) {
        BufferedReader reader = null;
        LinkedHashMap<String, String> content = new LinkedHashMap<>();
        try {
            reader = new BufferedReader(new FileReader(filePath));
            //filter title line
            String line = reader.readLine().trim();
            titleLine = titleLine == null ? line : titleLine;
            //read content
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if ("".equals(line)) {
                    continue;
                }
                String name = line.split(",")[0];
                content.put(name, line);
            }
            reader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }

    /**
     * split each version data into 'new files' and 'exist files'
     * 'new files' means these files that never appear in all previous version
     *
     * @param name
     */
    public static void splitData(String name) {
        List<String> files = getVerFileList(name);
        int len = files.size();
        LinkedHashMap<String, String> preData = readData(files.get(0));
        for (int i = 1; i < len; i++) {
            LinkedHashMap<String, String> verData = readData(files.get(i));
            List<String> newFiles = new ArrayList<>();
            List<String> existFiles = new ArrayList<>();
            newFiles.add(titleLine);
            existFiles.add(titleLine);
            for (String key : verData.keySet()) {
                String value = verData.get(key);
                if (preData.containsKey(key)) {
                    existFiles.add(value);
                }else {
                    newFiles.add(value);
                    preData.put(key, value);
                }
            }
            String[] outPath = getOutPath(files.get(i));
            Util.writeFile(outPath[0], newFiles);
            Util.writeFile(outPath[1], existFiles);
        }
    }

    public static String getMergeFilePath(String name) {
        String res = dirPath + name + "/" + name + "-allFileIndex.txt";
        return res;
    }

    /**
     * Union of all version files
     * @param name
     */
    public static void mergeData(String name) {
        List<String> files = getVerFileList(name);
        int len = files.size();
        LinkedHashMap<String, String> preData = readData(files.get(0));
        List<String> allFiles = new ArrayList<>();
        for (String key : preData.keySet()) {
            allFiles.add(allFiles.size() + "," + key);
        }
        for (int i = 1; i < len; i++) {
            LinkedHashMap<String, String> verData = readData(files.get(i));
            for (String key : verData.keySet()) {
                if (!preData.containsKey(key)) {
                    allFiles.add(allFiles.size() + "," + key);
                    preData.put(key, verData.get(key));
                }
            }
        }
        String outPath = getMergeFilePath(name);
        Util.writeFile(outPath, allFiles);
    }

    /**
     * read the Union of all the version files
     * return as Hashtable (key is file name, value is index)
     * @param name
     * @return
     */
    public static Hashtable<String, Integer> readAllFiles(String name) {
        String filePath = getMergeFilePath(name);
        BufferedReader reader = null;
        Hashtable<String, Integer> allFiles = new Hashtable<>();
        try {
            reader = new BufferedReader(new FileReader(filePath));
            String line = null;
            //read content
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if ("".equals(line)) {
                    continue;
                }
                String[] temp = line.split(",");
                allFiles.put(temp[1], Integer.parseInt(temp[0]));
            }
            reader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return allFiles;
    }

    public static Hashtable<Integer, String> readAllFilesReverse(String name) {
        String filePath = getMergeFilePath(name);
        BufferedReader reader = null;
        Hashtable<Integer, String> allFiles = new Hashtable<>();
        try {
            reader = new BufferedReader(new FileReader(filePath));
            String line = null;
            //read content
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if ("".equals(line)) {
                    continue;
                }
                String[] temp = line.split(",");
                allFiles.put(Integer.parseInt(temp[0]), temp[1]);
            }
            reader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return allFiles;
    }

    public static String getYpath(String filepath) {
        String[] temp = filepath.split("/");
        String suffix = temp[temp.length-1];
        String pre = filepath.substring(0, filepath.length() - suffix.length());
        String str = pre + "/y/" + suffix;
        pre = str.substring(0, str.length() - 4);
        String res = pre + "-y.txt";
        return res;
    }

    public static void buildF(String name) {
        Hashtable<String, Integer> allFiles = readAllFiles(name);
        List<String> versions = getVerFileList(name);
        for (String verPath : versions) {
            LinkedHashMap<String, String> verData = readData(verPath);
            int[] Y = new int[allFiles.size()];
            for (String key : verData.keySet()) {
                int index = allFiles.get(key);
                String[] parts = verData.get(key).split(",");
                String label = parts[parts.length-1];
                int intLabel = label.equals("1") ? 1 : -1;
                Y[index] = intLabel;
            }
            String yPath = getYpath(verPath);
            String outContent = "" + Y[0];
            for (int i = 1; i < Y.length; i++) {
                outContent += "," + Y[i];
            }
            List<String> content = new ArrayList<>();
            content.add(outContent);
            Util.writeFile(yPath, content);
        }
    }

    public static String getIPath(String file1, String file2) {
        String tag1 = file1.substring(0, file1.length()-4).split("_")[3];
        String tag2 = file2.substring(0, file2.length()-4).split("_")[3];
        String[] temp = file1.split("/");
        String suff = temp[temp.length-1];
        String pre = file1.substring(0, file1.length() - suff.length());
        String name = suff.split("_")[0];
        String res = pre + "I/" + name + "_" + tag1 + "_" + tag2 + ".txt";
        return res;
    }

    public static void buildI(String name) {
        List<String> files = getVerFileList(name);
        Hashtable<String, Integer> allFiles = readAllFiles(name);
        for (int i = 0; i < files.size() - 1; i++) {
            for (int j = i + 1; j < files.size(); j++) {

                LinkedHashMap<String, String> verData1 = readData(files.get(i));
                LinkedHashMap<String, String> verData2 = readData(files.get(j));
                int[] vectorI = new int[allFiles.size()];
                for (String key : allFiles.keySet()) {
                    if (verData1.containsKey(key) && verData2.containsKey(verData2)) {
                        int index = allFiles.get(key);
                        vectorI[index] = 1;
                    }
                }
                String outContent = "" + vectorI[0];
                for (int vIndex = 1; vIndex < vectorI.length; vIndex++) {
                    outContent += "," + vectorI[vIndex];
                }
                List<String> content = new ArrayList<>();
                content.add(outContent);
                String iPath = getIPath(files.get(i), files.get(j));
                Util.writeFile(iPath, content);
            }
        }
    }

    public static String getLPath(String filePath, String simType) {
        String[] temp = filePath.split("/");
        String suffix = temp[temp.length-1];
        String pre = filePath.substring(0, filePath.length() - suffix.length());
        String floder = "";
        if (simType.equals(EUCLI_SIM)) {
            floder = "/L/";
        }else if (simType.endsWith(COS_SIM)) {
            floder = "/L1/";
        }
        String str = pre + floder + suffix;
        pre = str.substring(0, str.length() - 4);
        String res = pre + "-L.txt";
        return res;
    }

    public static List<String> formatMatrixL(double[][] matrix) {
        List<String> res = new ArrayList<>();
        DecimalFormat df = new DecimalFormat("0.000");
        for (int i = 0; i < matrix.length; i++) {
            String line = df.format(matrix[i][0]);
            for (int j = 1; j < matrix[i].length; j++) {
                line += "," + df.format(matrix[i][j]);
            }
            res.add(line);
        }
        return res;
    }

    public static void buildLPro(String name, String simType) {
        List<String> files = getVerFileList(name);
        for (String file : files) {
            System.out.println("-------" + file + "-----------");
            List<String> content = calculateLStr(name, file, simType);
            String lPath = getLPath(file, simType);
            Util.writeFile(lPath, content);
        }
    }

    public static List<String> calculateLStr(String name, String filename, String simType) {
        LinkedHashMap<String, String> dataMap =readData(filename);
        Hashtable<Integer, String> allFiles = readAllFilesReverse(name);
        int len = allFiles.size();
        List<String> res = new ArrayList<>();
        DecimalFormat df = new DecimalFormat("0.000");
        for (int i = 0; i < len; i++) {
            double[] row = new double[len];
            String file1 = allFiles.get(i);
            if (dataMap.containsKey(file1)) {
                for (int j = 0; j != i && j < len; j++) {
                    String file2 = allFiles.get(j);
                    if (dataMap.containsKey(file2)) {
                        row[j] = simularity(file1, file2, simType);
                    }
                }
            }
            String line = "" + df.format(row[0]);
            for (int rowIndex = 1; rowIndex < len; rowIndex++) {
                line += "," + df.format(row[rowIndex]);
            }
            res.add(line);
        }
        return res;
    }

    public static void buildL(String name, String simType) {
        List<String> files = getVerFileList(name);
        for (String file : files) {
            System.out.println("-------" + file + "-----------");
            double[][] matrixL = calculateL(name, file, simType);
            List<String> content = formatMatrixL(matrixL);
            String lPath = getLPath(file, simType);
            Util.writeFile(lPath, content);
        }
    }


    public static double[][] calculateL(String name, String filename, String simType) {
       LinkedHashMap<String, String> dataMap =readData(filename);
       Hashtable<Integer, String> allFiles = readAllFilesReverse(name);
       int len = allFiles.size();
       double[][] W = new double[len][len];
       for (int i = 0; i < len; i++) {
           String file1 = allFiles.get(i);
           if (!dataMap.containsKey(file1)) {
               continue;
           }
           for (int j = i + 1; j < len; j++) {
                String file2 = allFiles.get(j);
                if (!dataMap.containsKey(file2)) {
                    continue;
                }
                double similarity = simularity(file1, file2, simType);
                W[i][j] = similarity;
                W[j][i] = similarity;
           }
       }
       double[][] L = W2L(W);
       return L;
    }


    public static double[][] W2L(double[][] W) {
        int len = W.length;
        double[][] D = new double[len][len];
        //calculate D Matrix
        for (int i = 0; i < len; i++) {
            for (int j = 0; j < len; j++) {
                D[i][i] += W[i][j];
            }
        }
        // L = D - W
        for (int i = 0; i < len; i++) {
            for (int j = 0; j < len; j++) {
                W[i][j] = D[i][j] - W[i][j];
            }
        }
        return W;
    }

    public static final String EUCLI_SIM = "euclidean";
    public static final String COS_SIM = "cos";



    public static double simularity(String data1, String data2, String simType) {
        String[] parts1 = data1.split(",");
        String[] parts2 = data2.split(",");
        int len = parts1.length;
        double[] vector1 = new double[len-1];
        double[] vector2 = new double[len-1];
        for (int i = 1; i < len; i++) {
            vector1[i-1] = Double.parseDouble(parts1[i]);
            vector2[i-1] = Double.parseDouble(parts2[i]);
        }
        double res = 0;
        if (simType.equals(EUCLI_SIM)) {
            res = euclideanSim(vector1, vector2);
        }else if (simType.equals(COS_SIM)) {
            res = cosSim(vector1, vector2);
        }
        return res;
    }

    public static final double sigma = 1;

    public static double euclideanSim(double[] vector1, double[] vector2) {
        double simmularity = 0;
        for (int i = 0; i < vector1.length; i++) {
            double diff = vector1[i] - vector2[i];
            simmularity += diff * diff;
        }
        simmularity /= (sigma * sigma);
        simmularity = Math.exp(- simmularity);
        return simmularity;
    }

    public static double cosSim(double[] vector1, double[] vector2) {
        double product = 0;
        double abs1 = 0;
        double abs2 = 0;
        for (int i = 0; i < vector1.length; i++) {
            product += vector1[i] * vector2[i];
            abs1 += vector1[i] * vector1[i];
            abs2 += vector2[i] * vector2[i];
        }
        abs1 = Math.sqrt(abs1);
        abs2 = Math.sqrt(abs2);
        double res = product / (abs1 * abs2);
        return res;
    }




    public static void main(String[] args) {
        for (String name : softwares) {
            //splitData(name)
            //mergeData(name);
            //buildF(name);
            //buildI(name);
            System.out.println("==========" + name + "============");
            buildLPro(name, EUCLI_SIM);
        }
//        LinkedHashMap<String, Integer> test = new LinkedHashMap<>();
//        test.put("111", 1);
//        test.put("222", 2);
//        test.put("333", 3);
//        test.put("444", 4);
//        test.put("555", 5);
//        for (String key1 : test.keySet()) {
//            for (String key2 : test.keySet()) {
//                System.out.println("====" + key1);
//                System.out.println("****" + key2);
//            }
//        }

    }



}
