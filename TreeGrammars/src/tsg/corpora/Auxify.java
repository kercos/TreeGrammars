package tsg.corpora;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map.Entry;

import settings.Parameters;
import tsg.TSNodeLabel;
import util.Utility;
import util.file.FileUtil;

public class Auxify {
	
	public static String[] suffixes = 
		new String[]{
		"'D",
		"'LL",
		"'M",
		"'RE",
		"'S",
		"'VE"
		};
	
	public static String[] auxgs = 
		new String[]{
		"BEIN",
		"BEING",
		"HAVING"
		};
	
	public static String[] auxs = 
		new String[]{
		"AHM",
		"AM",
		"ARE",
		"ART",
		"BE",
		"BEEN",
		"CAN",
		"COULD",
		"DID",
		"DO",
		"DOES",
		"DONE",
		"DOO",
		"GET",
		"GOT",
		"HAD",
		"HAFTA",
		"HAS",
		"HATH",
		"HAVE",
		"IS",
		"KIN",
		"MAHT",
		"MAY",
		"MAYE",
		"MIGHT",
		"MUST",
		"NEED",
		"OUGHT",
		"OUGHTA",
		"SHALL",
		"SHOULD",
		"SHULD",
		"WAS",
		"WERE",
		"WHADDYA",
		"WILL",
		"WILLYA",
		"WOULD"		
	};
	
	public static String[] verbs = new String[]{
		"VB",
		"VBD",
		"VBG",
		"VBN",
		"VBP",
		"VBZ"
	};
	
	public static String[] modals = new String[]{
		"MD"
	};
	
	public static void orderArrays() {
		Arrays.sort(suffixes);
		Arrays.sort(auxgs);
		Arrays.sort(auxs);
		Arrays.sort(verbs);
		
		System.out.println("suffixes");
		for(String s : suffixes) System.out.println("\"" + s + "\",");
		System.out.println();
		
		System.out.println("auxgs");
		for(String s : auxgs) System.out.println("\"" + s + "\",");
		System.out.println();
		
		System.out.println("auxs");
		for(String s : auxs) System.out.println("\"" + s + "\",");
		System.out.println();
		
		System.out.println("verbs");
		for(String s : verbs) System.out.println("\"" + s + "\",");
		System.out.println();
	}
	
	public static boolean hasAuxSuf( String word ) {
	    int pos = word.indexOf('\'');
	    if(pos == -1) return false;
	    String apostrophe = word.substring(pos);
	    return Arrays.binarySearch(suffixes, apostrophe)>=0;	    
	}

	public static boolean isAux( String word ) {
		return Arrays.binarySearch(auxs, word)>=0;
	}
	
	public static boolean isAuxg( String word ) {
		return Arrays.binarySearch(auxgs, word)>=0;
	}
	
	public static boolean isVerb( String word ) {
		return Arrays.binarySearch(verbs, word)>=0;
	}
	
	public static String auxify(String pos, String trm) {
	  if (pos.startsWith("AUX")) return pos;
	  String trmU = trm.toUpperCase();
	  if( isVerb( pos ) ) {	      
	      if( isAux( trmU ) || hasAuxSuf( trmU ) ) return "AUX";
	      else if( isAuxg( trmU )) return "AUXG";		
	  }
	  return null;
	}
	
	public static String[] skipInCompounds = new String[]{"ADVP","RB","UCP"};
	
	public static boolean auxify(TSNodeLabel t) {
		if (t.isLexical) return false;
		boolean result = false;
		for(TSNodeLabel d : t.daughters) {
			if (auxify(d)) result = true;
		}
		if (!t.label.toStringWithoutSemTags().equals("VP")) return result;
		boolean sawVP = false;
		for(TSNodeLabel d : t.daughters) {
			String dLabel = d.label.toStringWithoutSemTags(); 
			if (dLabel.equals("VP")) {
				sawVP=true;
				continue;
			}
			else if (isVerb(dLabel) || Arrays.binarySearch(skipInCompounds, dLabel)>=0) continue;
			return result;
		}
		if (!sawVP) return result;
		for(TSNodeLabel d : t.daughters) {
			TSNodeLabel l = d.daughters[0];
			if (!l.isLexical) continue;
			String newPos = auxify(d.label(), l.label());
			if (newPos!=null) {
				d.relabel(newPos);
				result = true;
			}
		}	
		return result;
	}
	
	public static <T> void printHashTable(Hashtable<T,int[]> table) {
		for(T key : table.keySet()) {
			System.out.println("\t" + key + "\t" + table.get(key)[0]);
		}
	}
	
	public static void auxStatistics() throws Exception {
		//Arrays.sort(suffixes);
		//Arrays.sort(auxgs);
		//Arrays.sort(auxs);
		Arrays.sort(verbs);
		
		HashSet<String> allAuxWords = new HashSet<String>();
		allAuxWords.addAll(Arrays.asList(suffixes));
		allAuxWords.addAll(Arrays.asList(auxgs));
		allAuxWords.addAll(Arrays.asList(auxs));
		
		HashSet<String> verbsPos = new HashSet<String>();
		verbsPos.addAll(Arrays.asList(verbs));
		verbsPos.addAll(Arrays.asList(modals));
		
		Hashtable<String, Hashtable<String, int[]>> statTable = new Hashtable<String, Hashtable<String, int[]>>();
		for(String w : allAuxWords) {
			statTable.put(w, new Hashtable<String, int[]>());
		}
		
		File trainingCorpusFile = new File(Wsj.WsjOriginal + "wsj-complete.mrg");
		ArrayList<TSNodeLabel> trainingCorpus = Wsj.getTreebank(trainingCorpusFile);
		
		for(TSNodeLabel t : trainingCorpus) {
			ArrayList<TSNodeLabel> lex = t.collectLexicalItems();
			for (TSNodeLabel l : lex) {
				String word = l.label().toUpperCase();
				if (allAuxWords.contains(word)) {
					String pos = l.parent.label();
					if (verbsPos.contains(pos)) {
						Hashtable<String, int[]> wordStat = statTable.get(word);
						Utility.increaseInTableInt(wordStat, pos);
					}
				}
			}
		}
		
		for(Entry<String, Hashtable<String, int[]>> e : statTable.entrySet()) {
			String word = e.getKey();
			Hashtable<String, int[]> table = e.getValue();
			System.out.println(word);
			printHashTable(table);
			System.out.println("\n");
		}
		
	}
	
	public static void main1(String[] args) throws Exception {
		File inputFile = new File("/Users/fedja/Work/Code/TreeGrammars/Viewers/TDS/wsj-00_original.mrg");
		File outputFile = new File("/Users/fedja/Work/Code/TreeGrammars/Viewers/TDS/wsj-00_cleaned_auxify.mrg");
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		ArrayList<TSNodeLabel> constTreebank = Wsj.getTreebankReadableAndClean(inputFile);
		int sentenceIndex = 0;
		for(TSNodeLabel constTree : constTreebank) {
			//TSNodeLabel constTree = constTreebank.get(426);
			sentenceIndex++;
			if (auxify(constTree)) System.out.println(sentenceIndex);;
			pw.println(constTree.toString());
		}
		pw.close();
	}
	
	public static void main(String[] args) throws Exception {
		auxStatistics();
	}
	
	

}

