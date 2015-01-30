package tsg.utils;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;

import tsg.TSNodeLabel;
import util.file.FileUtil;

public class ReplaceLexicon {

	public static void main(String[] args) {
		File origLexFile = new File(args[0]);
		File changeLexFile = new File(args[1]);
		File outputFile = new File(args[2]);		
		
		Scanner scanOrigLexFile = FileUtil.getScanner(origLexFile);
		Scanner scanChangeLexFile = FileUtil.getScanner(changeLexFile);
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		int lineNumber = 0;
		
		while(scanOrigLexFile.hasNextLine()) {
			lineNumber++;
			String origLexLine = scanOrigLexFile.nextLine();
			String changeLexLine = scanChangeLexFile.nextLine();			
			changeLexLine = changeLexLine.replaceAll("\"", "''");
			TSNodeLabel origTree = null;
			try {
				origTree = new TSNodeLabel(origLexLine);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			TSNodeLabel changeTree = null;
			
			if (changeLexLine.equals("(TOP ())")) {
				changeTree = TSNodeLabel.defaultWSJparse(origTree.collectLexicalWords().toArray(new String[]{}), "TOP");				
			}
			else {
				try {
					changeTree = new TSNodeLabel(changeLexLine);
				} catch (Exception e) {
					System.err.println("In line " + lineNumber + ": " + changeLexLine);
					e.printStackTrace();
				}
			}
			ArrayList<TSNodeLabel> origLex = origTree.collectLexicalItems();
			ArrayList<TSNodeLabel> changeLex = changeTree.collectLexicalItems();
			Iterator<TSNodeLabel> origLexIter = origLex.iterator();
			Iterator<TSNodeLabel> changeLexIter = changeLex.iterator();
			while(origLexIter.hasNext()) {
				changeLexIter.next().label = origLexIter.next().label;
			}
			pw.println(changeTree.toString());
		}
		pw.close();
		
	}
	
	public static void main1(String[] args) {
		String a = "sdfa\"fdasd";
		a = a.replaceAll("\"", "''");
		System.out.println(a);
	}
	
}
