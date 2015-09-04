package tdg.corpora;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Scanner;

import tdg.TDNode;
import util.Utility;
import util.file.FileUtil;

public class ConnlToUlab {
	/*
	1  	 ID  	 Token counter, starting at 1 for each new sentence.
	2  	 FORM  	 Word form or punctuation symbol. 		
	3 	LEMMA 	Lemma or stem (depending on particular data set) of word form, or an underscore if not available.
	4 	CPOSTAG 	Coarse-grained part-of-speech tag, where tagset depends on the language.
	5 	POSTAG 	Fine-grained part-of-speech tag, where the tagset depends on the language, or identical to the coarse-grained part-of-speech tag if not available.
	6 	FEATS 	Unordered set of syntactic and/or morphological features (depending on the particular language), separated by a vertical bar (|), or an underscore if not available.
	7 	HEAD 	Head of the current token, which is either a value of ID or zero ('0'). Note that depending on the original treebank annotation, there may be multiple tokens with an ID of zero.
	8 	DEPREL 	Dependency relation to the HEAD. The set of dependency relations depends on the particular language. Note that depending on the original treebank annotation, the dependency relation may be meaningfull or simply 'ROOT'.
	9 	PHEAD 	Projective head of current token, which is either a value of ID or zero ('0'), or an underscore if not available. Note that depending on the original treebank annotation, there may be multiple tokens an with ID of zero. The dependency structure resulting from the PHEAD column is guaranteed to be projective (but is not available for all languages), whereas the structures resulting from the HEAD column will be non-projective for some sentences of some languages (but is always available).
	10 	PDEPREL Dependency relation to the PHEAD, or an underscore if not available. The set of dependency relations depends on the particular language. Note that depending on the original treebank annotation, the dependency relation may be meaningfull or simply 'ROOT'. 
	*/
	
	static boolean coarsePos = false;
	
	//baseIndex=1;
	//rootIndex=0;
	
	public static void convertToUlab(File connlFile, File outputFile, String encoding) {
		Scanner connlScan = FileUtil.getScanner(connlFile, encoding);
		PrintWriter outputWriter = FileUtil.getPrintWriter(outputFile, encoding);
		String[] linesSentence; 
		do {
			linesSentence = ConnlX.getNextConnlLinesSentence(connlScan);
			if (linesSentence==null) break;
			int length = linesSentence.length;
			String words = "";
			String pos = "";
			String indexes = "";			
			for(int l=0; l<length; l++) {
				String line = linesSentence[l];
				String[] fields = line.split("\t");
				words += fields[1];
				String posCheck = fields[4];
				pos += (coarsePos || posCheck.equals("_") ? fields[3] : posCheck);
				indexes += fields[6];
				if (l!=length-1) {
					words += "\t";
					pos += "\t";
					indexes += "\t";
				}
			}
			outputWriter.println(words);
			outputWriter.println(pos);
			outputWriter.println(indexes);
			outputWriter.println();
		} while(true);
		outputWriter.close();
	}
	
	public static void convertToLab(File connlFile, File outputFile, String encoding) {
		Scanner connlScan = FileUtil.getScanner(connlFile, encoding);
		PrintWriter outputWriter = FileUtil.getPrintWriter(outputFile, encoding);
		String[] linesSentence; 
		do {
			linesSentence = ConnlX.getNextConnlLinesSentence(connlScan);
			if (linesSentence==null) break;
			int length = linesSentence.length;
			String words = "";
			String pos = "";
			String labs = "";
			String indexes = "";			
			for(int l=0; l<length; l++) {
				String line = linesSentence[l];
				String[] fields = line.split("\t");
				words += fields[1];
				String posCheck = fields[4];
				pos += (coarsePos || posCheck.equals("_") ? fields[3] : posCheck);
				labs += fields[7]; //DEPREL
				indexes += fields[6];
				if (l!=length-1) {
					words += "\t";					
					pos += "\t";
					labs += "\t";
					indexes += "\t";
				}
			}
			outputWriter.println(words);
			outputWriter.println(pos);
			outputWriter.println(labs);
			outputWriter.println(indexes);
			outputWriter.println();
		} while(true);
		outputWriter.close();
	}
	
	public static ArrayList<MstSentenceUlab> convertInMstSentenceUlabTreebank(File connlFile, String encoding) {
		Scanner connlScan = FileUtil.getScanner(connlFile, encoding);
		ArrayList<MstSentenceUlab> result = new ArrayList<MstSentenceUlab>();
		String[] linesSentence; 
		int sentenceIndex = 0;
		do {
			sentenceIndex++;
			//System.out.println(sentenceIndex);
			linesSentence = ConnlX.getNextConnlLinesSentence(connlScan);
			if (linesSentence==null) break;
			int length = linesSentence.length;
			String words = "";
			String pos = "";
			String indexes = "";			
			for(int l=0; l<length; l++) {
				String line = linesSentence[l];
				String[] fields = line.split("\t");
				words += fields[1];
				String posCheck = fields[4];
				pos += (posCheck.equals("_") ? fields[3] : posCheck);
				indexes += Integer.parseInt(fields[6]);
				if (l!=length-1) {
					words += "\t";
					pos += "\t";
					indexes += "\t";
				}
			}
			result.add(
					new MstSentenceUlab(words.split("\t"), 
							pos.split("\t"), Utility.parseIndexList(indexes.split("\t")))
			);
		} while(true);
		return result;
	}
	
	public static MstSentenceUlab getNextConnlLinesSentenceInMstUlab(Scanner connlScan) {
		String[] linesSentence = ConnlX.getNextConnlLinesSentence(connlScan);
		if (linesSentence==null) return null;
		int length = linesSentence.length;
		String words = "";
		String pos = "";
		int[] indexes = new int[length];			
		for(int l=0; l<length; l++) {
			String line = linesSentence[l];
			String[] fields = line.split("\t");
			words += fields[1];
			String posCheck = fields[4];
			pos += (posCheck.equals("_") ? fields[3] : posCheck);			 
			indexes[l] = Integer.parseInt(fields[6]);
			if (l!=length-1) {
				words += "\t";
				pos += "\t";
			}
		}
		MstSentenceUlab s = 
			new MstSentenceUlab(words.split("\t"), pos.split("\t"), indexes);
		return s;
	}
	
	public static void extractMultipleRootsCases(File connlFile, File outputFile, String encoding) {
		Scanner connlScan = FileUtil.getScanner(connlFile, encoding);
		PrintWriter outputWriter = FileUtil.getPrintWriter(outputFile, encoding);
		MstSentenceUlab sentence; 
		do {
			sentence = getNextConnlLinesSentenceInMstUlab(connlScan);
			if (sentence==null) break;			
			if (sentence.hasMultipleRoots()) outputWriter.println(sentence.toString());
		} while(true);
		outputWriter.close();
	}
	
	public static void extractNonProjectiveCases(File connlFile, File outputFile, String encoding) {
		Scanner connlScan = FileUtil.getScanner(connlFile, encoding);
		PrintWriter outputWriter = FileUtil.getPrintWriter(outputFile, encoding);
		MstSentenceUlab sentence; 
		do {
			sentence = getNextConnlLinesSentenceInMstUlab(connlScan);
			if (sentence==null) break;			
			if (!sentence.isProjective()) outputWriter.println(sentence.toString());
		} while(true);
		outputWriter.close();
	}
	
	public static void extractLoopsCases(File connlFile, File outputFile, String encoding) {
		Scanner connlScan = FileUtil.getScanner(connlFile, encoding);
		PrintWriter outputWriter = FileUtil.getPrintWriter(outputFile, encoding);
		MstSentenceUlab sentence; 
		do {
			sentence = getNextConnlLinesSentenceInMstUlab(connlScan);
			if (sentence==null) break;			
			if (sentence.hasLoops()) outputWriter.println(sentence.toString());
		} while(true);
		outputWriter.close();
	}
	

	
	public static int[] getRoots(int[] indexes) {
		int numberOfRoots = 0;	
		for(int i : indexes) {
			if (i==0) numberOfRoots++;			
		}		
		int[] rootIndexes = new int[numberOfRoots];
		int j=0;
		for(int i=0; i<indexes.length; i++) {
			if (indexes[i]==0) {
				rootIndexes[j] = i;
				j++;
			}
		}
		return rootIndexes;
	}
	
	public static void main(String[] args) {
		coarsePos = true;
		String rootPath = "/Volumes/HardDisk/Scratch/CORPORA/UniversalTreebank/langs/it/may_15/";
		File itDevX = new File(rootPath + "data/it-ud-dev.conllx");
		File itDevUlab = new File(rootPath + "mst/it-ud-dev.ulab");
		File itDevLab = new File(rootPath + "mst/it-ud-dev.lab");
		File itTestX = new File(rootPath + "data/it-ud-test.conllx");
		File itTestUlab = new File(rootPath + "mst/it-ud-test.ulab");
		File itTestLab = new File(rootPath + "mst/it-ud-test.lab");
		File itTrainX = new File(rootPath + "data/it-ud-train.conllx");
		File itTrainUlab = new File(rootPath + "mst/it-ud-train.ulab");
		File itTrainLab = new File(rootPath + "mst/it-ud-train.lab");
		
		convertToUlab(itDevX, itDevUlab, "UTF-8");
		convertToUlab(itTestX, itTestUlab, "UTF-8");
		convertToUlab(itTrainX, itTrainUlab, "UTF-8");
		convertToLab(itDevX, itDevLab, "UTF-8");
		convertToLab(itTestX, itTestLab, "UTF-8");
		convertToLab(itTrainX, itTrainLab, "UTF-8");
	}
}
