package tsg.corpora;

import java.io.*;
import java.util.*;

import settings.Parameters;
import tsg.TSNode;
import tsg.evalHeads.parcEval;
import util.*;
import util.file.FileUtil;


public class Parc extends ConstCorpus{
	
	private static final long serialVersionUID = 0L;
	public static String PARC_GOLD = Parameters.corpusPath + "PARC/parc700_headGold";
	public static String PARC_GOLD_NOTOP = Parameters.corpusPath + "PARC/parc700_headGold_noTop";
	public static String PARC_GOLD_CLEANED_NOTOP = 
		Parameters.corpusPath + "PARC/parc700_headGold_cleaned_noTop";
	
	/*
 		File trainingParcBinary = new File("/home/fsangati/CORPUS/PARC/00_21_upto40_parc");
		File parcGoldFile =  new File("/home/fsangati/CORPUS/PARC/parc700_headGold_noQuotations");
		Corpus gold = new Corpus(parcGoldFile, "parc");
		Corpus parcEval = Parameters.corpus.returnLast700();		
	 */
	
	public static void newParc() {
		File outputFileDependency = new File("/home/fsangati/CORPUS/PARC/parc700_readableNew");
		File outputFileHeadTrees = new File("/home/fsangati/CORPUS/PARC/parc700_headTreesNew");
		PrintWriter dependencyWriter = FileUtil.getPrintWriter(outputFileDependency);
		PrintWriter writerTrees = FileUtil.getPrintWriter(outputFileHeadTrees);
		String[] flatSentences = new String[700];
		TSNode[] trees = new TSNode[700];
		int[][][] wordBoundary = new int[700][][];
		int[][] indexConversion = new int[700][];
		int[][][] dependency = new int[700][][];
		readFlatSentences(flatSentences);
		readTrees(trees);
		readWordBoundary(wordBoundary, flatSentences, indexConversion);
		readDependency(dependency, indexConversion);
		int wrongAssignment = 0;
		for(int i=0; i<700; i++) {	
			//if (i==268) continue;
			String flatSentence = flatSentences[i];
			dependencyWriter.println(flatSentence);			
			TSNode tree = trees[i];
			String[] report = new String[]{""};
			boolean right = DepConstConverter.assignHeadsFromDependencyTable(tree, wordBoundary[i], dependency[i], report);
			dependencyWriter.print(report[0]);
			dependencyWriter.println();
			writerTrees.println(tree.toString(true, false));
			if (!right) wrongAssignment++;
		}
		dependencyWriter.close();
		writerTrees.close();
		System.out.println("Wrong assignments: " + wrongAssignment);
	}
	
	public static void readFlatSentences(String[] flatSentences) {
		File parcFileFlat = new File("/home/fsangati/CORPUS/PARC/sec23_selection_flat");
		try {
			Scanner scanFlat = new Scanner(parcFileFlat, "ISO-8859-1");
			for(int i=0; i<700; i++) flatSentences[i] = scanFlat.nextLine();
			scanFlat.close();			
		} catch (IOException e) {FileUtil.handleExceptions(e);}
	}
	
	public static void readTrees(TSNode[] trees) {
		File parcFileTree = new File("/home/fsangati/CORPUS/PARC/sec23_selection_compressed");
		try {
			Scanner scanTree = new Scanner(parcFileTree, "ISO-8859-1");
			for(int i=0; i<700; i++) trees[i] = new TSNode(scanTree.nextLine());			
			scanTree.close();			
		} catch (IOException e) {FileUtil.handleExceptions(e);}
	}
	
	public static void readWordBoundary(int[][][] wordBoundary, String[] flatSentences, int[][] indexConversion) {
		File parcFileWords = new File("/home/fsangati/CORPUS/PARC/parc700_WordList");
		LinkedList<int[]> wordBlocks = new LinkedList<int[]>();
		LinkedList<Integer> sentenceIndexConversion = new LinkedList<Integer>();
		try {
			int currentLine = 0, currentIndex = 0, blockNumber=0;
			String[] choices = flatSentences[0].split(" ");
			Scanner scanWordList = new Scanner(parcFileWords, "ISO-8859-1");
			while ( scanWordList.hasNextLine()) {
				String wordListLine = scanWordList.nextLine();
				wordListLine.trim();
				//word(1, 0, 'Meridian', [index-'5',proper-misc,pers-'3',num-sg]).
				int first_parenthesis = wordListLine.indexOf('(');
				int first_coma = wordListLine.indexOf(',');
				int second_coma = wordListLine.indexOf(',', first_coma+1);
				int third_coma = wordListLine.indexOf('[')-2;
				int lineIndex = Integer.parseInt(wordListLine.substring(first_parenthesis+1, first_coma)) - 1;				
				//int wordIndex = Integer.parseInt(wordListLine.substring(first_coma+2, second_coma));
				String word = wordListLine.substring(second_coma+2, third_coma).toLowerCase();
				if (word.charAt(0)=='\'' && word.charAt(word.length()-1)=='\'') word = word.substring(1, word.length()-1);
				word = word.replaceAll("\\\\\\'", " '").trim();
				String[] split = word.split("\\s+");
				int length = split.length;
				int[] indexesInBlock = new int[length];
				indexesInBlock[0] = -1;
				if (lineIndex!=currentLine || !scanWordList.hasNextLine()) {
					if (!scanWordList.hasNextLine()) {
						wordBlocks.add(indexesInBlock);
						sentenceIndexConversion.add(blockNumber);						
					}
					wordBoundary[currentLine] = wordBlocks.toArray(new int[][]{});
					indexConversion[currentLine] = Utility.intArrayConversion(sentenceIndexConversion);
					wordBlocks.clear();				
					sentenceIndexConversion.clear();
					currentLine = lineIndex;
					choices = flatSentences[currentLine].split(" ");
					currentIndex=0;
					blockNumber=0;
				}				
				String realWord = choices[currentIndex].toLowerCase().replaceAll("\\\\/", "/");
				boolean variation = variation(word, realWord);
				while (word.indexOf(realWord)==-1 && realWord.indexOf(word)==-1 && !variation) {
					currentIndex++;
					realWord = choices[currentIndex].toLowerCase().replaceAll("\\\\/", "/");;
					variation = variation(word, realWord);
				}
				if(length==1) { //common case										
					if (word.equals(realWord) || variation) { //most common
						sentenceIndexConversion.add(blockNumber);
						indexesInBlock[0] = currentIndex;
						currentIndex++;
						blockNumber++;
					}
					else {
						int index = realWord.indexOf(word);
						sentenceIndexConversion.add(blockNumber);
						if (index==0) {
							indexesInBlock[0] = currentIndex;							
						}						
						else if (index+word.length()==realWord.length()) {
							currentIndex++;
							blockNumber++;
						}
					}
				}
				else {
					for(int i = 0; i<length; i++) indexesInBlock[i] = currentIndex + i;						
					sentenceIndexConversion.add(blockNumber);
					currentIndex += length;
					blockNumber++;
				}
				if (indexesInBlock[0]!=-1) wordBlocks.add(indexesInBlock);
			}
			scanWordList.close();			
		} catch (IOException e) {FileUtil.handleExceptions(e);}
	}
	
	public static boolean variation(String word, String realWord) {
		if (word.equals("can") && realWord.equals("ca")) return true;
		if (word.equals("not") && realWord.equals("n't")) return true;
		if (word.equals("is") && realWord.equals("'s")) return true;
		if (word.equals("has") && realWord.equals("'s")) return true;
		if (word.equals("have") && realWord.equals("'ve")) return true;
		if (word.equals("will") && realWord.equals("wo")) return true;
		if (word.equals("will") && realWord.equals("'ll")) return true;
		if (word.equals("are") && realWord.equals("'re")) return true;
		return false;
	}
	
	public static void readDependency(int[][][] dependency, int[][] indexConversion) {
		File parcFileDependency = new File("/home/fsangati/CORPUS/PARC/parc700_DepList");
		try {			
			Scanner scanDependency = new Scanner(parcFileDependency, "ISO-8859-1");
			LinkedList<int[]> dependecyInSentece = new LinkedList<int[]>();
			int currentLine = 0;
			while ( scanDependency.hasNextLine()) {	
				String dependencyLine = scanDependency.nextLine();
				dependencyLine = dependencyLine.trim();
				//dependency(1, w(9), [inf_form], w(10)).
				int first_parenthesis = dependencyLine.indexOf('(');
				int first_coma = dependencyLine.indexOf(',');
				int second_coma = dependencyLine.indexOf(',', first_coma+1);
				int third_coma = dependencyLine.indexOf(',', second_coma+1);
				if (dependencyLine.charAt(first_coma+2)!='w' || dependencyLine.charAt(third_coma+2)!='w') continue;
				int lineIndex = Integer.parseInt(dependencyLine.substring(first_parenthesis+1, first_coma)) - 1;
				int firstWordIndex = Integer.parseInt(dependencyLine.substring(first_coma+4, second_coma-1));
				int secondWordIndex = Integer.parseInt(dependencyLine.substring(third_coma+4, dependencyLine.length()-3));
				if (lineIndex!=currentLine || !scanDependency.hasNextLine()) {
					if (!scanDependency.hasNextLine()) dependecyInSentece.add(new int[]{firstWordIndex, secondWordIndex});
					dependency[currentLine] = dependecyInSentece.toArray(new int[][]{});
					dependecyInSentece.clear();
					currentLine = lineIndex;
				}
				int[] dep = new int[2];
				dep[0] = indexConversion[currentLine][firstWordIndex];
				dep[1] = indexConversion[currentLine][secondWordIndex];
				dependecyInSentece.add(dep);
			}			
			scanDependency.close();			
		} catch (IOException e) {FileUtil.handleExceptions(e);}
	}

	/*public static void parcOld() {
		File inputFile = new File("/home/fsangati/CORPUS/Lemmatizer/lemmatizer.txt");
		Lemmatizer L = new Lemmatizer(inputFile);		
		File parcFile = new File("/home/fsangati/CORPUS/PARC/parc700-2006-05-30.fdsc");
		File parcFileFlat = new File("/home/fsangati/CORPUS/PARC/sec23_selection_flat");
		File parcFileTree = new File("/home/fsangati/CORPUS/PARC/sec23_selection_compressed");
		File outputFileDependency = new File("/home/fsangati/CORPUS/PARC/parc700_readable");		
		PrintWriter writer = Utility.getPrintWriter(outputFileDependency, Utility.defaultEncoding);
		File outputFileHeadTrees = new File("/home/fsangati/CORPUS/PARC/parc700_headTrees");
		PrintWriter writerTrees = Utility.getPrintWriter(outputFileHeadTrees, Utility.defaultEncoding);
		int wrongAssignment = 0;
		try {
			Scanner scan = new Scanner(parcFile, "ISO-8859-1");	
			Scanner scanFlat = new Scanner(parcFileFlat, "ISO-8859-1");
			Scanner scanTree = new Scanner(parcFileTree, "ISO-8859-1");
			while ( scan.hasNextLine()) {		
				String line = scan.nextLine();
				line = line.trim();
				if (line.length()==0) continue;
				if (line.startsWith("sentence_form(")) {
					String sentence = scanFlat.nextLine();			
					TreeNode tree = new TreeNode(scanTree.nextLine());
					writer.println(sentence);
					String[] words = sentence.split(" ");					
					while(scan.hasNextLine()) {
						line = scan.nextLine();
						line = line.trim();
						int tilde1Index = line.indexOf('~');
						if(tilde1Index==-1) {
							if (line.equals(")")) {
								tree.fixUnaryHeadConsistency();
								if (!tree.checkHeadConsistency(false)) {
									wrongAssignment++;
									//System.out.println(tree.toString(true, false));
								}
								writer.println();
								writerTrees.println(tree.toString(true, false));
								break;
							}
							continue;
						}
						int tilde2Index = line.indexOf('~', line.indexOf('~')+1);
						if (tilde2Index==-1) continue;
						// adjunct(pay~0, assume~7)
						String head = line.substring(line.indexOf('(')+1, tilde1Index);
						String dependent = line.substring(line.indexOf(", ")+2, tilde2Index);
						int indexes[] = L.getLexicon(head, dependent, words);
						if (indexes==null) {
							continue;
						}
						writer.println("\t" + words[indexes[0]] + " (" + indexes[0] + ")" + " --> " + words[indexes[1]] + " (" + indexes[1] + ")");
						tree.assignHeadsFromDependencyPair(indexes[0], indexes[1]);
					}
				}				
			}				
			scan.close();
			scanFlat.close();
			scanTree.close();
			writer.close();
		}
		catch (IOException e) {Utility.handleIO_exception(e);}
		System.out.println("Wrong assignments: " + wrongAssignment);		
	}*/
	
	public static void main(String args[]) {
		//newParc();
		training40PlusParc();
	}

	public static void training40PlusParc() {
		File trainingFile = new File(Parameters.corpusPath + "COLLINS_97/wsj-02-21.mrg");
		ConstCorpus trainingCorpus = new ConstCorpus(trainingFile,"collins97_02-21");
		trainingCorpus.removeTreesLongerThan(40, Wsj.nonCountCatInLength);
		File parcGoldFile =  new File("/home/fsangati/CORPUS/PARC/parc700_headGold");		
		ConstCorpus parc = new ConstCorpus(parcGoldFile, "parc");
		trainingCorpus.treeBank.addAll(parc.treeBank);
		Wsj.removeQuotations(trainingCorpus);
		String outputFile = "/home/fsangati/CORPUS/PARC/02_21_upto40_parc";		
		trainingCorpus.toBinaryFile(new File(outputFile + ".binary"));
		trainingCorpus.toFile_Complete(new File(outputFile + ".complete"), false);
	}
	
}
