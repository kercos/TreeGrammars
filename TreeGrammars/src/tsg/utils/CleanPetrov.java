package tsg.utils;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

import tsg.Label;
import tsg.TSNodeLabel;
import util.file.FileUtil;

public class CleanPetrov {

	static Label topLabel = Label.getLabel("TOP");
	
	public static void main(String[] args) {	
		boolean separateOutput = args.length > 1;
		File inputFile = new File(args[0]);
		File outputFile = separateOutput ? new File(args[1]) : new File(args[0] + ".tmp");
		Scanner scan = FileUtil.getScanner(inputFile);
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		int index = 0;
		while(scan.hasNextLine()) {
			index++;
			String line = scan.nextLine();
			if (line.isEmpty()) 
				continue;			
			//line = AddTop.fixLine(line);
			line = line.replaceAll("\\\\", "");
			if (line.equals("(TOP ())")) {
				System.err.println("Ignoring error in line " + index + ": " + line);
				pw.println("(TOP null)");
				continue;
			}
			if (!line.startsWith("(")) {
				System.err.println("Parenthesis not fund in line " + index + ": '" 
						+ line + "'" + " --> replacing with (TOP null)" );
				pw.println("(TOP null)");
				continue;
			}
			TSNodeLabel t;			
			try {
				t = new TSNodeLabel(line);
			} catch (Exception e) {
				System.err.println("Found error in tree in line " + index + ": '" + line + "'");
				pw.println("(TOP null)");				
				System.err.println("Aborting");
				e.printStackTrace();
				return;
			}
			t = cleanPetrovTree(t);
			pw.println(t);
		}
		pw.close();
		if (!separateOutput)
			outputFile.renameTo(inputFile);
	}
	
	public static void main1(String[] args) throws Exception {
		String line = "( (S-0 (@S-26 (NP-30 (NP-28 (@NP-58 (DT-18 The) (NN-26 economy)) (POS-0 's)) (NN-32 temperature)) (VP-10 (MD-3 will) (VP-21 (VB-6 be) (VP-36 (@VP-15 (@VP-13 (@VP-13 (VBN-22 taken) (PP-7 (IN-28 from) (NP-21 (@NP-35 (JJ-56 several) (NN-10 vantage)) (NNS-7 points)))) (NP-15 (DT-13 this) (NN-40 week))) (,-0 ,)) (PP-29 (IN-24 with) (NP-49 (@NP-4 (@NP-16 (@NP-10 (@NP-16 (@NP-10 (NP-47 (NP-10 (NNS-37 readings)) (PP-1 (IN-32 on) (NP-1 (NN-49 trade)))) (,-0 ,)) (NP-3 (NN-48 output))) (,-0 ,)) (NP-3 (NN-48 housing))) (CC-6 and)) (NP-1 (NN-49 inflation)))))))) (.-0 .)) )";
		line = AddTop.fixLine(line);
		new TSNodeLabel(line);
	}
	
	public static TSNodeLabel cleanPetrovTree(TSNodeLabel t) {			
		t = undoBinarization(t);
		cleanPetrovRefinement(t);
		t.label = topLabel;
		return t;
	}
	
	
	static String regex = "\\-\\d+";
	
	public static void cleanPetrovRefinement(TSNodeLabel tree) {
		if (tree.isLexical) return;
		String transformedLabel = tree.label();
		int cutIndex = transformedLabel.indexOf('-');
		int cutIndex2 = transformedLabel.indexOf('=');
		final int cutIndex3 = transformedLabel.indexOf('^');
		if (cutIndex3 > 0 && (cutIndex3 < cutIndex2 || cutIndex2 == -1))
			cutIndex2 = cutIndex3;
		if (cutIndex2 > 0 && (cutIndex2 < cutIndex || cutIndex <= 0))
			cutIndex = cutIndex2;
		if (cutIndex > 0) {
			transformedLabel = transformedLabel.substring(0, cutIndex);			
			tree.relabel(transformedLabel);
		}
		if (transformedLabel.indexOf('-')!=-1) {
			transformedLabel = transformedLabel.replaceAll(regex, "");
			tree.relabel(transformedLabel);
		}
		for(TSNodeLabel d : tree.daughters) {
			cleanPetrovRefinement(d);			
		}
	}
	
	public static String cleanPetrovLabel(String label) {
		int cutIndex = label.indexOf('-');
		int cutIndex2 = label.indexOf('=');
		final int cutIndex3 = label.indexOf('^');
		if (cutIndex3 > 0 && (cutIndex3 < cutIndex2 || cutIndex2 == -1))
			cutIndex2 = cutIndex3;
		if (cutIndex2 > 0 && (cutIndex2 < cutIndex || cutIndex <= 0))
			cutIndex = cutIndex2;
		if (cutIndex > 0) {
			label = label.substring(0, cutIndex);						
		}
		if (label.indexOf('-')!=-1) {
			label = label.replaceAll(regex, "");
		}
		return label;
	}
	
	public static TSNodeLabel undoBinarization(TSNodeLabel t) {
		int prole = t.prole();
		TSNodeLabel unbinaryCopy = new TSNodeLabel(t.label, t.isLexical);
		if (prole==0) return unbinaryCopy;
		if (prole==1) {
			TSNodeLabel onlyDaughter = undoBinarization(t.firstDaughter());
			unbinaryCopy.daughters = new TSNodeLabel[]{onlyDaughter};			
			onlyDaughter.parent = unbinaryCopy;
			return unbinaryCopy;
		}
		
		TSNodeLabel firstDaughter = t.firstDaughter();
		if (firstDaughter.label().charAt(0)!='@') {
			TSNodeLabel[] newDaughters = new TSNodeLabel[prole];
			for(int i=0; i<prole; i++) {
				newDaughters[i] = undoBinarization(t.daughters[i]);				
			}			
			unbinaryCopy.assignDaughters(newDaughters);
			return unbinaryCopy;
		}
		
		ArrayList<TSNodeLabel> newDaughters = new ArrayList<TSNodeLabel>();
		TSNodeLabel currentLevel = t;
		do {
			newDaughters.add(undoBinarization(currentLevel.lastDaughter()));
			currentLevel = currentLevel.daughters[0];
		} while(currentLevel.label().charAt(0)=='@');
		newDaughters.add(undoBinarization(currentLevel));
		int newProle = newDaughters.size();
		unbinaryCopy.daughters = new TSNodeLabel[newProle];		
		int i=newProle-1;
		for(TSNodeLabel d : newDaughters) {
			unbinaryCopy.daughters[i--] = d;
			d.parent = unbinaryCopy;
		}
		return unbinaryCopy;
	}

	
	
}
