package tsg.mwe;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.BitSet;
import java.util.Scanner;

import tsg.ParenthesesBlockPennStd.WrongParenthesesBlockException;
import tsg.TSNodeLabel;
import util.Utility;

public class TSG_Frags_MWE_Eval_Inside {
	
	static String mwePrefix = "MW";
	static int minMweCountToPrint = 10;

	
	private static void eval(String workingDir) throws FileNotFoundException, WrongParenthesesBlockException {		
		File outputFile = new File(workingDir + "tsg_mwe_eval.txt");
		PrintWriter pw = new PrintWriter(outputFile);
		String logLikeDir = workingDir + "LogLike/Frags/";
		String mpiTotDir = workingDir + "MPI_tot/Frags/";
		String insideDir = workingDir + "Inside_ratio/Frags/";
		pw.println(
			"Ass.Meas." + "\t" +
			"Signature" + "\t" +
			"Sign.Length" + "\t" +
			"Tot.Frags" + "\t" +
			"Tot.MWE" + "\t" +
			"MWE%1" + "\t" +
			"MWE%1/2" + "\t" +
			"MWE%1/3" + "\t" +
			"MWE%1/4" + "\t" +
			"MWE%1/5" + "\t" +
			"MWE%1/6" + "\t" +
			"MWE%1/7" + "\t" +
			"MWE%1/8" + "\t" +
			"MWE%1/9" + "\t" +
			"MWE%1/10"
		);
		for(File logLikeFragFile : new File(logLikeDir).listFiles()) {
			String file_name = logLikeFragFile.getName();
			if (!file_name.startsWith("frags"))
				continue;
			evalFile(logLikeFragFile, pw, "LogLike");						
		}
		for(File mpiTotFragFile : new File(mpiTotDir).listFiles()) {
			String file_name = mpiTotFragFile.getName();
			if (!file_name.startsWith("frags"))
				continue;
			evalFile(mpiTotFragFile, pw, "MpiTot");						
		}
		for(File insideFragFile : new File(insideDir).listFiles()) {
			String file_name = insideFragFile.getName();
			if (!file_name.startsWith("frags"))
				continue;
			evalFile(insideFragFile, pw, "InsideRatio");						
		}
		pw.close();
		System.out.println("Output file: " + outputFile);
	}
	
	private static void evalFile(File evalFile, PrintWriter pw, String assMeas) throws FileNotFoundException, WrongParenthesesBlockException {
		String file_name = evalFile.getName();
		int secondUnderscoreIndex = Utility.indexOf(file_name, '_', 2);
		int dotIndex = file_name.indexOf('.');
		String sign = file_name.substring(secondUnderscoreIndex + 1, dotIndex);
		int signlength = Integer.parseInt(file_name.substring(secondUnderscoreIndex-1, secondUnderscoreIndex));
		Scanner scan = new Scanner(evalFile);
		BitSet mweIndex = new BitSet();
		int countFrags = 0;
		while(scan.hasNextLine()) {			
			String line = scan.nextLine();
			TSNodeLabel frag = TSNodeLabel.newTSNodeLabelStd(line);
			if (frag.label().startsWith(mwePrefix) || frag.prole()==1 && frag.daughters[0].label().startsWith(mwePrefix))
				mweIndex.set(countFrags);
			countFrags++;
		}
		int totMwe = mweIndex.cardinality();
		if (totMwe<minMweCountToPrint)
			return;
		double[] percentages = getPercentages(countFrags, mweIndex);
		pw.print(
				assMeas	+ "\t" + //"Ass.Meas." 
				sign + "\t" + //"Signature" 
				signlength + "\t" + //"Sign.Length" 
				countFrags + "\t" + //"Tot.Frags" 
				totMwe //"Tot.MWE"				
			);
		for(double p : percentages)
			pw.print("\t" + p);
		pw.println();
	}
	
	static double[] getPercentages(int totalFrags, BitSet mweIndex) {
		double[] result = new double[10];
		for(int i=1; i<11; i++) {
			int fragsPartition = totalFrags/i;
			BitSet mweIndexPartition = mweIndex.get(0, fragsPartition);
			double mweTotal = mweIndexPartition.cardinality();
			result[i-1] = mweTotal*100/fragsPartition;
		}
		return result;
	}

	public static void main(String[] args) throws FileNotFoundException, WrongParenthesesBlockException {
		//eval("/gardner0/data/TSG_MWE/Dutch/LassySmall/frag_MWE_AM_5_100/");
		//eval("/gardner0/data/TSG_MWE/Dutch/LassySmall/frag_MWE_AM_2_10/");
		eval("/gardner0/data/TSG_MWE/FTB/TreebankFrags/frag_MWE_AM_2_10_noPos/");
		//eval("/gardner0/data/TSG_MWE/Dutch/LassySmall/TreebankFrags/frag_MWE_AM_2_10_noPos/");
	}

	


}
