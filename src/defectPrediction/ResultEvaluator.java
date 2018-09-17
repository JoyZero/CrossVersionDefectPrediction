package defectPrediction;


public class ResultEvaluator {
	
	public static double precision(double truePos, double trueNeg, double falsePos, double falseNeg) {
		double res  = truePos / (truePos + falsePos);
		return res;
	}
	
	public static double recall(double truePos, double trueNeg, double falsePos, double falseNeg) {
		double res = truePos / (truePos + falseNeg);
		return res;
	}
	
	public static double fmeasure(double truePos, double trueNeg, double falsePos, double falseNeg) {
		double precision = precision(truePos, trueNeg, falsePos, falseNeg);
		double recall = recall(truePos, trueNeg, falsePos, falseNeg);
		double res = (2* precision * recall) / (precision + recall);
		return res;
	}
	
	
	public static double gMean(double truePos, double trueNeg, double falsePos, double falseNeg) {
		double res = (trueNeg / (trueNeg + falsePos)) 
				* (truePos / (truePos + falseNeg));
		res = Math.sqrt(res);
		return res;
	}
	
	public static double balance(double truePos, double trueNeg, double falsePos, double falseNeg) {
		double pd = truePos / (truePos + falseNeg);
		double pf = falsePos / (falsePos + trueNeg);
		double balance = 1 - Math.sqrt(((0 - pf) * (0 - pf) + (1 - pd) * (1 - pd)) / 2);
		return balance;
	}
}
