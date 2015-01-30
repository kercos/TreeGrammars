package tsg.parseEval;
import java.io.*;
import java.util.*;

import settings.Parameters;
import tsg.*;
import tsg.corpora.*;
import util.*;
import util.file.FileUtil;

import java.text.*;

public class EvalB {
	
	static String EvalBPath = Parameters.softwarePath + "EVALB/";
	static String EvalBApp = EvalBPath + "evalb";
	static String EvalBDefaultParam = EvalBPath + "new.prm";
	static String EvalbArgs = "-p " + EvalBDefaultParam;
	

	public EvalB(File goldFile, File parsedFile, File outputFile, boolean labeled) {
		String resultFolder = Parameters.resultsPath + "TSG/";
		String evalbApp = "/Users/fsangati/Work/SOFTWARE/EVALB/evalb";
		String evalbArgs = "-p " + resultFolder + "collins";
		evalbArgs += (labeled) ? ".prm" : ".UL.prm";
		try {
			Process p = Runtime.getRuntime().exec(evalbApp + " " + evalbArgs + " " + goldFile + " " + parsedFile);
			redirectOutput(p, outputFile, false);			
		}
		catch (Exception e) {
			FileUtil.handleExceptions(e);
        }
	}
	
	public EvalB(File goldFile, File parsedFile, File outputFile) {		
		try {
			Process p = Runtime.getRuntime().exec(EvalBApp + " " + EvalbArgs + " " + goldFile + " " + parsedFile);
			redirectOutput(p, outputFile, false);			
		}
		catch (Exception e) {
			FileUtil.handleExceptions(e);
        }
	}
	
	static final String RECALL = "Bracketing Recall";
	//static final String PRECISION = "Bracketing Precision";
	//static final String FMEASURE = "Bracketing FMeasure";
	
	public static String[] getAllRecallPrecisionF1(File evalBfile) {
		Scanner scan = FileUtil.getScanner(evalBfile);
		String[] result = new String[3];
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.startsWith(RECALL)) {
				result[0]= line.substring(line.indexOf('=')+1).trim();
				line = scan.nextLine();
				result[1]= line.substring(line.indexOf('=')+1).trim();
				line = scan.nextLine();
				result[2]= line.substring(line.indexOf('=')+1).trim();
				break;
			}
		}
		return result;
	}
	
	private static void redirectOutput(Process p, File outFile, boolean printProgress) {
	    try {            
	    	String s = null;
	    	BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
	        PrintWriter pw = FileUtil.getPrintWriter(outFile);

	        while ((s = stdInput.readLine()) != null) {
	        	pw.println(s);
	        }
	        pw.close();
	        stdInput.close();
	        p.getOutputStream().close();
	        p.getErrorStream().close();	    	
	    		    		        
	    } catch (IOException e) { FileUtil.handleExceptions(e); }
	}
	
	public static void main(String[] args) {
		String basePath = "/scratch/fsangati/RESULTS/TSG/DOP_SD_Reranker/";
		//new EvalB(new File(args[0]), new File(args[1]), new File(args[2]), Boolean.parseBoolean(args[3]));
		File gold = new File(basePath + "wsj-22_gold.mrg");
		File test = new File(basePath + "wsj-22_reranked_5best_PQ.mrg");
		File evalF = new File(basePath + "wsj-22_reranked_5best_PQ.evalB");
		new EvalB(gold, test, evalF, true);		
	}
	
}

