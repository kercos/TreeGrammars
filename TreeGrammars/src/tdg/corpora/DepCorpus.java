package tdg.corpora;

import java.io.File;
import java.io.PrintWriter;
import java.util.*;

import tdg.TDNode;
import util.*;
import util.file.FileUtil;

public class DepCorpus {
	
	public static String ukTag = "*UNKNOWN*";
	
	public static ArrayList<TDNode> readTreebankFromFileYM(File inputFile, int LL) {
		return readTreebankFromFileYM(inputFile, LL, false);
	}
	
	public static ArrayList<TDNode> readTreebankFromFileYM(File inputFile, int LL, boolean convertNumber) {
		int baseIndex = 1;
		int rootIndex = -1;
		ArrayList<TDNode> treebank = new ArrayList<TDNode>();
		Scanner scan = FileUtil.getScanner(inputFile);
		String sentenceStructure = "";
		int senteceWords = 0;
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.length()==0) {
				sentenceStructure = sentenceStructure.trim();
				if (sentenceStructure.length()>0 && senteceWords<=LL) {					
					TDNode newTDN = TDNode.from(sentenceStructure, baseIndex, rootIndex, TDNode.FORMAT_MALT_TAB);
					if (convertNumber) newTDN.replaceNumbers("<num>");
					treebank.add(newTDN);
				}
				sentenceStructure = "";
				senteceWords = 0;
			}
			else {
				sentenceStructure += line + "\n";
				senteceWords++;
			}
		}
		if (sentenceStructure.length()>0 && senteceWords<=LL) {
			TDNode newTDN = TDNode.from(sentenceStructure, baseIndex, rootIndex, TDNode.FORMAT_MALT_TAB);
			if (convertNumber) newTDN.replaceNumbers("<num>");
			treebank.add(newTDN);
		}
		scan.close();
		return treebank;
	}
	
	public static ArrayList<TDNode> readTreebankFromFileMALT(File inputFile, int LL) {
		//int baseIndex = 1;
		//int rootIndex = 0;
		ArrayList<TDNode> treebank = new ArrayList<TDNode>();
		Scanner scan = FileUtil.getScanner(inputFile);
		String sentenceStructure = "";
		int senteceWords = 0;
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.length()==0) {
				sentenceStructure = sentenceStructure.trim();
				if (sentenceStructure.length()>0 && senteceWords<=LL) {					
					treebank.add(TDNode.from(sentenceStructure, TDNode.FORMAT_MALT_TAB));
				}
				sentenceStructure = "";
				senteceWords = 0;
			}
			else {
				String[] lineSplit = line.split("\t");
				line = lineSplit[0] + "\t" + lineSplit[1] + "\t" + lineSplit[2];
				sentenceStructure += line + "\n";
				senteceWords++;
			}
		}
		if (sentenceStructure.length()>0 && senteceWords<=LL) {					
			treebank.add(TDNode.from(sentenceStructure, TDNode.FORMAT_MALT_TAB));
		}
		scan.close();
		return treebank;
	}
	
	public static ArrayList<TDNode> readTreebankFromFileMST(File inputFile, int LL, 
			boolean hasLabels, boolean convertNumbers) {
		int baseIndex = 1;
		int rootIndex = 0;
		ArrayList<TDNode> treebank = new ArrayList<TDNode>();
		Scanner scan = FileUtil.getScanner(inputFile);
		while(scan.hasNextLine()) {
			String words = scan.nextLine();
			if (words.length()==0) continue;
			String[] wordsArray = words.split("\t");			
			String postags = scan.nextLine();
			String labels = null;
			if (hasLabels) labels = scan.nextLine();
			String indexes = scan.nextLine();
			if (wordsArray.length>LL) continue;
			TDNode newTDN = new TDNode(wordsArray, postags.split("\t"), 
					Utility.parseIndexList(indexes.split("\t")), baseIndex, rootIndex);
			if (convertNumbers) newTDN.replaceNumbers("<num>");
			treebank.add(newTDN);
		}
		scan.close();
		return treebank;
	}
	
	public static Duet<ArrayList<TDNode>,ArrayList<String>> readTreebankFromFileMSTComments(
			File inputFile, int LL, boolean labeling,boolean convertNumbers) {
		int baseIndex = 1;
		int rootIndex = 0;
		ArrayList<TDNode> treebank = new ArrayList<TDNode>();
		ArrayList<String> comments = new ArrayList<String>();
		Scanner scan = FileUtil.getScanner(inputFile);
		while(scan.hasNextLine()) {			
			String comment = scan.nextLine();
			if (comment.length()==0) continue;
			comments.add(comment);
			String words = scan.nextLine();
			String[] wordsArray = words.split("\t");			
			String postags = scan.nextLine();
			String labels = (labeling) ? scan.nextLine() : null;
			String indexes = scan.nextLine();
			if (wordsArray.length>LL) continue;
			TDNode newTDN = new TDNode(wordsArray, postags.split("\t"), 
					Utility.parseIndexList(indexes.split("\t")), baseIndex, rootIndex);
			if (convertNumbers) newTDN.replaceNumbers("<num>");
			treebank.add(newTDN);
		}
		scan.close();
		return new Duet<ArrayList<TDNode>,ArrayList<String>>(treebank, comments);
	}
	
	public static ArrayList<TDNode> readTreebankFromFile(File inputFile) {
		return readTreebankFromFileYM(inputFile, Integer.MAX_VALUE);
	}
	
	public static void convertConll07ToUlab(File inputFile, File outputFile) {		
		Scanner scan = FileUtil.getScanner(inputFile);
		PrintWriter out = FileUtil.getPrintWriter(outputFile);
		String sentenceStructure = "";
		int senteceWords = 0;
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.length()==0) {
				sentenceStructure = sentenceStructure.trim();
				if (sentenceStructure.length()>0) {
					TDNode t = TDNode.from(sentenceStructure, TDNode.FORMAT_CONNL);
					out.println(t.toStringMSTulab(false));					
				}				
				else out.println();
				sentenceStructure = "";
				senteceWords = 0;
			}
			else {
				sentenceStructure += line + "\n";
				senteceWords++;
			}
		}
		scan.close();
		out.close();
	}

	public static ArrayList<TDNode> readTreebankFromFileConll07(File inputFile, int LL) {
		//int baseIndex = 1;
		//int rootIndex = 0;
		ArrayList<TDNode> treebank = new ArrayList<TDNode>();
		Scanner scan = FileUtil.getScanner(inputFile);
		String sentenceStructure = "";
		int senteceWords = 0;
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.length()==0) {
				sentenceStructure = sentenceStructure.trim();
				if (sentenceStructure.length()>0 && senteceWords<=LL) {					
					treebank.add(TDNode.from(sentenceStructure, TDNode.FORMAT_CONNL));
				}
				sentenceStructure = "";
				senteceWords = 0;
			}
			else {
				//String[] lineSplit = line.split("\t");
				//        word                 postag               index
				//line = lineSplit[1] + "\t" + lineSplit[3] + "\t" + lineSplit[6];
				sentenceStructure += line + "\n";
				senteceWords++;
			}
		}
		if (sentenceStructure.length()>0 && senteceWords<=LL) {					
			treebank.add(TDNode.from(sentenceStructure, TDNode.FORMAT_MALT_TAB));
		}
		scan.close();
		return treebank;
	}
	
	public static void toFileMSTulab(File outputFile, ArrayList<TDNode> treebank, boolean blind) {
		PrintWriter out = FileUtil.getPrintWriter(outputFile);
		int i=0;
		for(TDNode t : treebank) {
			out.println(t.toStringMSTulab(blind)+"\n");
			i++;
		}
		out.close();
	}
	
	public static void toFileMALTulab(File outputFile, ArrayList<TDNode> treebank, boolean blind) {
		PrintWriter out = FileUtil.getPrintWriter(outputFile);
		for(TDNode t : treebank) {
			out.println(t.toStringMALTulab(0, blind));
		}
		out.close();
	}
	
	public static Hashtable<String, Integer> getLexiconFreq(ArrayList<TDNode> treebank) {
		Hashtable<String, Integer>  lexFreqTable = new Hashtable<String, Integer>();
		for(TDNode t : treebank) {
			t.updateLexiconFreqTable(lexFreqTable);
		}
		return lexFreqTable;
	}
	
	public static Set<String> extractLexiconWithoutUnknown(ArrayList<TDNode> corpus, int uk_limit) {
		Hashtable<String, Integer> lexiconFreq = DepCorpus.getLexiconFreq(corpus);
		System.out.println("Total lex: " + lexiconFreq.size());
		Utility.removeInTable(lexiconFreq, uk_limit);
		System.out.println("Total lex after unknown removal: " + lexiconFreq.size());
		Set<String> lexicon = lexiconFreq.keySet();
		DepCorpus.renameUnknownWords(corpus, lexicon);
		return lexicon;
	}
	
	public static Hashtable<String, Integer> getHeadLRNullStatTable(
			ArrayList<TDNode> treebank, int SLP_type) {
		Hashtable<String, Integer> table = new Hashtable<String, Integer>();
		for(TDNode t : treebank) {
			t.updateHeadLRNullFreqTable(table, SLP_type);
		}
		return table;
	}
	
    public static Hashtable<String, Integer> getHeadFreqTable(
            ArrayList<TDNode> treebank, int SLP_type) {
    	Hashtable<String, Integer> table = new Hashtable<String, Integer>();
    	for(TDNode t : treebank) {
            t.updateHeadFreqTable(table, SLP_type);
    	}
    return table;
}

	
	/**
	 * Replace words occuring a umber of times <= uk_limit
	 * @param treebank
	 * @param ukTag
	 * @param uk_limit
	 */
	public static void renameUnknownWords(ArrayList<TDNode> treebank, Set<String>  lexicon) {
		for(TDNode t : treebank) {
			t.renameUnknownWords(lexicon, ukTag);
		}
	}
		
	public static void posConversion(ArrayList<TDNode> treebank, 
			Hashtable<String, String> posConvTable, 
			Set<String> applicable) {
		for(TDNode t : treebank) {
			t.posConversion(posConvTable, applicable);
		}
	}
	
	public static void addEmptyDaughters(ArrayList<TDNode> treebank) {
		for(TDNode t : treebank) {
			t.addEmptyDaughters();
		}
	}
	
	public static void removeEmptyDaughters(ArrayList<TDNode> treebank) {
		for(TDNode t : treebank) {
			t.removeEmptyDaughters();
		}
	}
	
	public static void addEOS(ArrayList<TDNode> treebank) {
		for(TDNode t : treebank) {
			t.addEOS();
		}
	}
	
	public static boolean checkIfPresentEOS(ArrayList<TDNode> trainingCorpus) {
		TDNode first = trainingCorpus.get(0);
		return (first.hasEOS());
	}

	
	public static void removeEOS(ArrayList<TDNode> treebank) {
		for(TDNode t : treebank) {
			t.removeEOS();
		}
	}

	public static void removeCoordinationSentences(ArrayList<TDNode> treebank) {
		for(ListIterator<TDNode> iter = treebank.listIterator(); iter.hasNext();) {
			TDNode t = iter.next();
			if (t.isCoordinationSentence()) iter.remove();			
		}		
	}
	
	public static void toAdWait(ArrayList<TDNode> treebank, File outputFile) {
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		for(TDNode t : treebank) {
			pw.println(t.toStringAdWait());
		}
		pw.close();
	}

	public static void fromAdWait(ArrayList<TDNode> depCorpus, File goldFileAdWait,
			File goldFileDepMxpost) {
		PrintWriter pw = FileUtil.getPrintWriter(goldFileDepMxpost);
		Scanner adWaitScanner = FileUtil.getScanner(goldFileAdWait);
		for(TDNode t : depCorpus) {
			String adWaitLine = adWaitScanner.nextLine();
			String[] adWaitLineSplit = adWaitLine.split("\\s");
			TDNode[] structure = t.getStructureArray();
			for(int i=0; i<adWaitLineSplit.length; i++) {
				String adWaitLexPos = adWaitLineSplit[i];
				String wordPos = adWaitLexPos.substring(adWaitLexPos.indexOf('_') + 1);
				structure[i].postag = wordPos;				
			}
			pw.println(t.toStringMSTulab(false) + "\n");
		}	
		pw.close();
	}

	public static void markTerminalNodes(ArrayList<TDNode> trainingCorpus) {
		for(TDNode t : trainingCorpus) {
			t.markTerminalNodesPOS();
		}
	}

	public static void markVerbDomination(ArrayList<TDNode> trainingCorpus) {
		for(TDNode t : trainingCorpus) {
			t.markVerbDominationPOS();
		}
	}

	public static void markFirstLevelNodes(ArrayList<TDNode> trainingCorpus, int limit) {
		for(TDNode t : trainingCorpus) {
			t.markFirstLevelNodesPOS(0, limit);
		}
	}

	public static boolean isConnlFormat(File inputFile) {
		Scanner scanner = FileUtil.getScanner(inputFile);
		String firstLine = "";
		while(scanner.hasNextLine() && firstLine.equals("")) {
			firstLine = scanner.nextLine();			
		}
		scanner.nextLine(); //String secondLine = 
		String thirdLine = scanner.nextLine();
		String forthLine = scanner.nextLine();
		if (Utility.isIntegerList(thirdLine.split("\t")) ||
			Utility.isIntegerList(forthLine.split("\t"))) return false;
		return true;
	}
	
}
