package tsg.parsingExp;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

import tsg.TSNodeLabel;
import tsg.corpora.Wsj;
import tsg.mb.TreeMarkoBinarization;
import util.file.FileUtil;


public class ExpBitPar {
	
	public static ArrayList<TSNodeLabel> getSample(ArrayList<TSNodeLabel> treebank, int sampleSize) {
		ArrayList<TSNodeLabel> result = new ArrayList<TSNodeLabel>(sampleSize);
		Random random = new Random();
		int size = treebank.size();
		HashSet<Integer> chosen = new HashSet<Integer>();
		while(result.size()<sampleSize) {
			Integer index = random.nextInt(size);
			if (chosen.contains(index)) continue;
			chosen.add(index);
			result.add(treebank.get(index));			
		}
		return result;
	}

	public static void main(String args[]) throws Exception {
		
		int nBest = 1;
		int sampleSize = 10;		
		
		int threads = 1;
		File trainingTreebankFile = new File(Wsj.WsjOriginalCleanedTopSemTagsOff + "wsj-02-21.mrg"); 		
		String outputDir = "tmp/bitparTest/";
		
		ArrayList<TSNodeLabel> treebankTrainOriginal = TSNodeLabel.getTreebank(trainingTreebankFile);
		
		File outputShortTreebankTrain = new File(outputDir + "treebankTrain.mrg");
		File outputShortTreebankTest = new File(outputDir + "treebankTest.mrg");
		
		Random random = new Random();
					
		for(int c=0; c<1000; c++) {						
			
			ArrayList<TSNodeLabel> treebankTrainSample = getSample(treebankTrainOriginal, sampleSize);
			TSNodeLabel.printTreebankToFile(outputShortTreebankTrain, treebankTrainSample, false, false);
							
			ArrayList<TSNodeLabel> treebankTestSample = new ArrayList<TSNodeLabel>();
			int randomIndex = random.nextInt(sampleSize);
			treebankTestSample.add(treebankTrainSample.get(randomIndex));			
			TSNodeLabel.printTreebankToFile(outputShortTreebankTest, treebankTestSample, false, false);
			
			String[] localOutputDirs = new String[]{outputDir + "one/", outputDir + "two/"};		
						
			File[] outputsBitPar = new File[]{
				new File(localOutputDirs[0] + "BitParWorkingDir/" + "outputBitPar_" + nBest + "best.txt"),
				new File(localOutputDirs[1] + "BitParWorkingDir/" + "outputBitPar_" + nBest + "best.txt")
				//new File(localOutputDirs[0] + "BITPAR_MPD.mrg"),
				//new File(localOutputDirs[1] + "BITPAR_MPD.mrg"),	
			};
			
			for(int i=0; i<2; i++) {
				
				//TreeMarkoBinarization.parentLabelSeparator = i==0 ? "@" : "@@";
				
				FileUtil.removeAllFileAndDirInDir(new File(localOutputDirs[i]));				
				String localOutputDir = localOutputDirs[i]; 				
				
				TSGparsingBitPar.main(
					new String[]{
						"-createNewDir:false",	
						"-threads:" + threads,
						//"-restrictTestToFirst:10",
						"-runOnlyMPD:true",
						"-nBest:" + nBest,
						"-ukThreshold:-1",					
						//"-sentenceLengthLimitTest:40",
						"-markoBinarize:true",
						"-markovV:2",
						"-markovH:1",
						"-sortGrammarFile:" + (i==0 ? "true" : "false"),						
						outputShortTreebankTrain.toString(),
						outputShortTreebankTest.toString(),
						"null",
						localOutputDir.toString()
					}
				);				
			
			}
			
			if (FileUtil.differ(outputsBitPar[0],outputsBitPar[1], "(")) {
				System.out.println("Differ!");
				break;
			}
				
		}		
		
	}
	
}
