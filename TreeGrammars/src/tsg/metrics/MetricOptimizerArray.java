package tsg.metrics;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import tsg.TSNodeLabel;
import tsg.parsingExp.TSGparsingBitPar;
import util.file.FileUtil;

public class MetricOptimizerArray {
	
	public static double[] lambdaValues = new double[]{0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};
	public static boolean runOnlyMPD = false;
	public static boolean runOnlyShortestDerivation = false;	
	
	ParseMetricOptimizer[] MO;
	int lambdaValuesLength, arrayLength;
	
	public static double format(double d) {
		return Double.parseDouble(MCP.lambdaFormat.format(d));
	}
	
	public static void setLambdaValues(double min, double max, double gap) {		
		int length = (int)((max - min)/gap) + 1;
		lambdaValues = new double[length];
		double previous = lambdaValues[0] = format(min);
		for(int i=1; i<length-1; i++) {			
			previous += gap; 
			lambdaValues[i] = format(previous);
		}
		lambdaValues[length-1] = format(previous + gap);
	}

	public MetricOptimizerArray(TSGparsingBitPar TSGparsing) {		
		
		if (runOnlyMPD) {			
			arrayLength = 1;
			MO = new ParseMetricOptimizer[]{new MPD()};
			return;
		}
		if (runOnlyShortestDerivation) {			
			int[] nBestList = new int[]{5, 10, 20, 30, 50, 80, 100, 150, 200, 250, 300, 500, 600, 700, 800, 900, 1000};
			arrayLength = nBestList.length;
			MO = new ParseMetricOptimizer[arrayLength];
			for(int i=0; i<arrayLength; i++) {
				MO[i] = new SD(TSGparsing, nBestList[i]);				
			}
			return;
		}
		
		lambdaValuesLength = lambdaValues.length;
		arrayLength = lambdaValuesLength + 4;
		MO = new ParseMetricOptimizer[arrayLength];
		
		int j=0;
		for(int i=0; i<lambdaValuesLength; i++) {
			double lambda = lambdaValues[i];
			MO[j++] = new MCP(lambda);
		}
		MO[j++] = new MRuleSum();
		MO[j++] = new MRuleProduct();
		MO[j++] = new MPP();
		MO[j++] = new MPD();
	}
	
	/*
	public MetricOptimizerArray() {		
		
		lambdaValuesLength = 1;
		arrayLength = 1;
		MO = new ParseMetricOptimizer[]{new MCfgFrameP()};
		
	}
	*/
	
	public File[] makeFileOutputList(String fileNamePath, String dotExtension) {
		File[] result = new File[arrayLength];
		for(int i=0; i<arrayLength; i++) {
			result[i] = new File(fileNamePath + MO[i].identifier + dotExtension);
		}
		return result;
	}
	
	public String[] getIdentifiers() {
		String[] result = new  String[arrayLength];
		for(int i=0; i<arrayLength; i++) {
			result[i] = MO[i].identifier;
		}
		return result;
	}
	
	public void appendResult(File[] fileList) throws Exception {
		for(int i=0; i<arrayLength; i++) {
			File outputFile = fileList[i];
			FileOutputStream out = new FileOutputStream(outputFile, true); 
			OutputStreamWriter pw = new OutputStreamWriter(out,FileUtil.defaultEncoding);
			MO[i].prinBestTrees(pw);
			pw.close();
			out.close();			
		}
	}
	
	public void prepareNextSentence(String[] lexicalItems) {
		for(ParseMetricOptimizer mo : MO) {
			mo.prepareNextSentence(lexicalItems);
		}
	}
	
	public String[] getCurrentSentenceWords() {		
		return MO[0].getCurrentSentenceWords();
	}
	
	public void addNewDerivation(TSNodeLabel tree, double prob) {
		for(ParseMetricOptimizer mo : MO) {
			mo.addNewDerivation(tree.clone(), prob);
		}
	}
	
	public void storeCurrentBestParseTrees() throws Exception {
		for(ParseMetricOptimizer mo : MO) {
			mo.storeCurrentBestParseTrees();
		}
	}

	

	
	
	
}
