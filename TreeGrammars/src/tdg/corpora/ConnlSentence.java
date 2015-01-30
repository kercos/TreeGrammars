package tdg.corpora;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

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
			
		while (line.equals("") && connlScan.hasNextLine()) {
			line = connlScan.nextLine();
		};		
		if (line.equals("")) return null;
		lines += line + "\n";		
		while(connlScan.hasNextLine()) {			
			line = connlScan.nextLine();
			if (line.equals("")) break;
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
}
