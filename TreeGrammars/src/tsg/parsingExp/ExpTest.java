package tsg.parsingExp;

import java.io.File;

import tsg.corpora.Wsj;
import util.ArgumentReader;


public class ExpTest {

	public static void main(String args[]) throws Exception {
				
		int threads = ArgumentReader.readIntOption(args[0]);
		File trainingTreebankFile = ArgumentReader.readFileOptionNoSeparation(args[1]); 
		File testTreebankFile = ArgumentReader.readFileOptionNoSeparation(args[2]);
		File outputDir = ArgumentReader.readFileOptionNoSeparation(args[3]);		
				
		TSGparsingBitPar.main(
			new String[]{
				"-threads:" + threads,
				//"-restrictTestToFirst:10",
				"-runOnlyMPD:true",
				"-nBest:1",
				"-ukThreshold:4",					
				//"-sentenceLengthLimitTest:40",
				"-markoBinarize:true",
				"-markovH:0",
				"-markovH:0",
				trainingTreebankFile.toString(),
				testTreebankFile.toString(),
				"null",
				outputDir.toString()
			}
		);
		
		
	}
	
}
