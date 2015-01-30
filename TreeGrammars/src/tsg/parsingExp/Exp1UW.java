package tsg.parsingExp;

import java.io.File;

import tsg.corpora.Wsj;
import util.ArgumentReader;


public class Exp1UW {

	public static void main(String args[]) throws Exception {
				
		int threads = ArgumentReader.readIntOption(args[0]);
		String ukModel = ArgumentReader.readStringOption(args[1]);
		File trainingTreebankFile = ArgumentReader.readFileOptionNoSeparation(args[2]); 
		File testTreebankFile = ArgumentReader.readFileOptionNoSeparation(args[3]);
		File outputDir = ArgumentReader.readFileOptionNoSeparation(args[4]);		
		
		int[] ukList = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
		
		/*
 		File outputDir = new File("/Users/fedja/Work/Code/TreeGrammars/tmp/");		
		int threads = 2;		
		//int uk = 1;
		File trainingTreebankFile = new File(Wsj.WsjOriginalCleanedTopSemTagsOff + "wsj-02-21.mrg"); 
		File testTreebankFile = new File(Wsj.WsjOriginalCleanedTopSemTagsOff + "wsj-24.mrg");		
		*/
		
		for(int uk : ukList ) {
			TSGparsingBitPar.main(
				new String[]{
					"-threads:" + threads,
					//"-restrictTestToFirst:10",
					"-runOnlyMPD:true",
					"-nBest:1",
					"-ukThreshold:" + uk,
					"-ukModel:" + ukModel,					
					"-sentenceLengthLimitTest:40",
					"-markoBinarize:false",
					trainingTreebankFile.toString(),
					testTreebankFile.toString(),
					"null",
					outputDir.toString()
				}
			);
		}
		
		
		
	}
	
}
