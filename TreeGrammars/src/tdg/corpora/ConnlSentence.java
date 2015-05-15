package tdg.corpora;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;

import util.Utility;
import util.file.FileUtil;

public class ConnlSentence {

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
	
	//baseIndex=1;
	//rootIndex=0;
	
	String[] forms, lemmas, cpostags, postags, feats, deprels, pdeprels;
	int[] heads, pheads;
	int length;
	
	public ConnlSentence(String[] form, String[] lemma, String[] cpostag, String[] postag,
			String[] feats, int[] heads, String[] deprel, 
			int[] phead, String[] pdeprel) {
		this.forms = form;
		this.lemmas = lemma;
		this.cpostags = cpostag;
		this.postags = postag;
		this.feats = feats;
		this.heads = heads;
		this.deprels = deprel;
		this.pheads = phead;
		this.pdeprels = pdeprel;
		length = this.forms.length;
	}
	
	public ConnlSentence(String[] lines) {
		length = lines.length;		
		for(int l=0; l<length; l++) {
			String line = lines[l];
			String[] fields = line.split("\t");
			this.forms[l] += fields[1];
			this.lemmas[l] += fields[2];
			this.cpostags[l] = fields[3];
			this.postags[l] = fields[4];
			this.feats[l] = fields[5];
			this.heads[l] = Integer.parseInt(fields[6]);
			this.deprels[l] = fields[7];
			this.pheads[l] = Integer.parseInt(fields[8]);
			this.pdeprels[l] = fields[9];
		}
	}
	
	public static String[] getNextConnlLinesSentence(Scanner connlScan) {
		String lines = "";
		String line = "";
			
		while ((line.equals("")) && connlScan.hasNextLine()) { //line.startsWith("#")
			line = connlScan.nextLine();
		};		
		if (line.equals("")) 
			return null;
		lines += line + "\n";		
		while(connlScan.hasNextLine()) {			
			line = connlScan.nextLine();
			if (line.equals("")) 
				break;
			lines += line + "\n";
		}				
		return lines.split("\n");
	}
	
	public static ArrayList<ConnlSentence> getTreebankFromFile(File connlFile, String encoding) {
		ArrayList<ConnlSentence> treebank = new ArrayList<ConnlSentence>();
		Scanner connlScan = FileUtil.getScanner(connlFile, encoding);
		String[] linesSentence; 
		do {
			linesSentence = ConnlSentence.getNextConnlLinesSentence(connlScan);
			if (linesSentence==null) break;
			int length = linesSentence.length;
			String words = "";
			String pos = "";
			String indexes = "";			
			for(int l=0; l<length; l++) {
				String line = linesSentence[l];
				String[] fields = line.split("\t");
				words += fields[1];
				pos += fields[4];
				indexes += Integer.parseInt(fields[6]);
				if (l!=length-1) {
					words += "\t";
					pos += "\t";
					indexes += "\t";
				}
			}
		} while(true);
		return treebank;
	}
	
	public static void anonimyzeConllWPHL(File inputFile, File outputFile) throws FileNotFoundException {
		Scanner scan = new Scanner(inputFile);
		PrintWriter pw = new PrintWriter(outputFile);
		boolean newSentence = true;
		int sentenceCount = 0;
		while(scan.hasNextLine()) {			
			String line = scan.nextLine();
			if (line.isEmpty()) {
				if (!newSentence) { //very first sentence
					pw.println();
				}
				newSentence = true;
				sentenceCount++;
				continue;
			}
			if (line.startsWith("#")) {
				//pw.println(line);
				continue;
			}
			String[] tabs = line.split("\t");
			//0: index
			//1: word
			//2: lemma
			//3: pos_coarse
			//4: pos_fine
			//5: morph
			//6: head
			//7: arclabel
			//8: ?
			//9: ?
			String[] newTabs = new String[]{
					tabs[0], tabs[1], "_", tabs[3], "_", "_", tabs[6], tabs[7], "_", "_"};
			String outputline = Utility.joinStringArrayToString(newTabs, "\t");
			pw.println(outputline);
			newSentence = false;
		}
		pw.close();
		System.out.println("Successfully processed " + sentenceCount + " sentences.");
	}
	
	public static void anonimyzeConll(File inputFile, File outputFile, File stanfordPosTags) throws FileNotFoundException {
		Scanner scan = new Scanner(inputFile);
		PrintWriter pw = new PrintWriter(outputFile);
		boolean newSentence = true;
		int sentenceCount = 0;
		ArrayList<ArrayList<String>> pos = stanfordPosTags==null ? null : getPos(stanfordPosTags);
		Iterator<ArrayList<String>> iterPos = pos==null ? null : pos.iterator();
		Iterator<String> iterIterPos = pos==null ? null : iterPos.next().iterator();
		while(scan.hasNextLine()) {			
			String line = scan.nextLine();
			if (line.isEmpty()) {
				if (!newSentence) { //very first sentence
					pw.println();
				}
				newSentence = true;
				iterIterPos = pos==null || !iterPos.hasNext() ? null : iterPos.next().iterator();
				sentenceCount++;
				continue;
			}
			if (line.startsWith("#")) {
				pw.println(line);
				continue;
			}
			String[] tabs = line.split("\t");
			//0: index
			//1: word
			//2: lemma
			//3: pos_coarse
			//4: pos_fine
			//5: morph
			//6: head
			//7: arclabel
			//8: ?
			//9: ?
			String outputline = pos==null ? 
					tabs[0] + "\t" + tabs[1] + "\t_\t_\t_\t_\t_\t_\t_\t_" :
					tabs[0] + "\t" + tabs[1] + "\t" + iterIterPos.next() + "\t_\t_\t_\t_\t_\t_\t_";	
			pw.println(outputline);
			newSentence = false;
		}
		pw.close();
		System.out.println("Successfully processed " + sentenceCount + " sentences.");
	}
	
	private static ArrayList<ArrayList<String>> getPos(File stanfordPosTags) throws FileNotFoundException {
		ArrayList<ArrayList<String>> result = new ArrayList<ArrayList<String>>();		
		Scanner scan = new Scanner(stanfordPosTags);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			ArrayList<String> linePos = new ArrayList<String>();
			String[] wordsPos = line.split(" ");
			for(String wp : wordsPos) {
				String p = wp.split("_")[1];
				linePos.add(p);
			}
			result.add(linePos);
		}
		return result;
	}
		
	public static void goldWithTestPosUTB() throws FileNotFoundException {
		String root = "/Volumes/HardDisk/Scratch/CORPORA/UniversalTreebank/langs/it/";
		for (String f : new String[]{"test","dev"}) {
			File inputFile = new File(root + "it-ud-" + f + ".conllx");
			//File outputFile = new File(root + "it-ud-" + f + ".conllu.blanks");
			//anonimyzeConll(inputFile, outputFile, null);

			File outputFile = new File(root + "it-ud-" + f + ".conllx.testPos");
			File posFile = new File(root + "it-ud-" + f + ".stanfordCoarsePoS.test");
			anonimyzeConll(inputFile, outputFile, posFile);
		}
	}
	
	public static void anonymizeUTB() throws FileNotFoundException {
		String root = "/Volumes/HardDisk/Scratch/CORPORA/UniversalTreebank/langs/it/";
		for (String f : new String[]{"train","test","dev"}) {
			File inputFile = new File(root + "it-ud-" + f + ".conllx");
			
			//File outputFile = new File(root + "it-ud-" + f + ".conllx.wphl");
			//anonimyzeConllWPHL(inputFile, outputFile);
			
			File outputFile = new File(root + "it-ud-" + f + ".blind.conllx");
			anonimyzeConll(inputFile, outputFile, null);
		}
	}

	
	public static void main(String[] args) throws FileNotFoundException {
		//goldWithTestPosUTB();
		anonymizeUTB();
	}
}
