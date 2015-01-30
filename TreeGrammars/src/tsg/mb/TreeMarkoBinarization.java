package tsg.mb;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Scanner;

import tsg.Label;
import tsg.TSNodeLabel;
import util.ArgumentReader;
import util.BoundedLinkedList;
import util.file.FileUtil;

public abstract class TreeMarkoBinarization {
	
	static char artificialNodesBracketsStart = '<';
	static char artificialNodesBracketsEnd = '>';
	public static String parentLabelSeparator = "@";
	static char catSeparationV = '|';
	static char catSeparationH = '_';
	static char headSeparation = '#';	
		
	public static int markH=2, markV=2;	
	
	public static void setHV(int h, int v) {
		markH = h;
		markV = v;
	}
	
	public static TreeMarkoBinarization getMarkoBinarizer(String type) {
		if (type.equals("Petrov_left"))
			return new TreeMarkoBinarizationLeft_Petrov();
		if (type.equals("Right_LC"))
			return new TreeMarkoBinarizationRight_LC();
		if (type.equals("Right_LC_LeftCorner"))
			return new TreeMarkoBinarizationRight_LC_LeftCorner();
		if (type.equals("Right_LC_nullary"))
			return new TreeMarkoBinarizationRight_LC_nullary();			
		return null;
	}
	
	public ArrayList<TSNodeLabel> markoBinarizeTreebank(ArrayList<TSNodeLabel> treebank) {
		ArrayList<TSNodeLabel> result = new ArrayList<TSNodeLabel>(treebank.size()); 
		for(TSNodeLabel t : treebank) {			
			result.add(performMarkovBinarization(t));
		}
		return result;
	}
	
	public void markoBinarizeFile(File inputFile, File outputFile) throws Exception {
		Scanner scan = FileUtil.getScanner(inputFile);
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			TSNodeLabel t = new TSNodeLabel(line);
			TSNodeLabel z = performMarkovBinarization(t);
			pw.println(z);
		}
		
		pw.close();
	}
	
	public void undoMarkoBinarizeFile(File inputFile, File outputFile) throws Exception {
		Scanner scan = FileUtil.getScanner(inputFile);
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			TSNodeLabel t = new TSNodeLabel(line);
			TSNodeLabel z = undoMarkovBinarization(t);
			pw.println(z);
		}
		
		pw.close();
	}
	
	public void checkConversionConsistencyOnFile(File inputFile) throws Exception {
		Scanner scan = FileUtil.getScanner(inputFile);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			TSNodeLabel t = new TSNodeLabel(line);
			TSNodeLabel tclone = t.clone();
			TSNodeLabel y = performMarkovBinarization(t);
			TSNodeLabel z = undoMarkovBinarization(y);
			if (!tclone.equals(z)) {
				System.err.println("Tree Markov Binarization Inconsitency!");
				System.err.println("\tOriginal:          " + tclone.toStringTex(false, false));
				System.err.println("\tTransformed:       " + y.toStringTex(false, false));
				System.err.println("\tBack transformed : " + z.toStringTex(false, false));
				return;
			}				
		}
	}
	
	public abstract String getDescription();
	public abstract TSNodeLabel performMarkovBinarization(TSNodeLabel t);
	public abstract TSNodeLabel undoMarkovBinarization(TSNodeLabel t);
	
	public static TSNodeLabel performParentAnnotation(TSNodeLabel t) {
		if (markV<=1) return t;
		return performRealParentAnnotation(t);
	}
	
	private static TSNodeLabel performRealParentAnnotation(TSNodeLabel t) {
		
		if (t.isLexical) {
			return new TSNodeLabel(t.label, true);
		}
		
		Label resultLabel = null;
		if (t.parent==null) {
			resultLabel = t.label;
		}
		else {
			StringBuilder newLabelSB = new StringBuilder();
			TSNodeLabel parent = t.parent;
			for(int i=1; i<markV; i++) {
				String prependString = catSeparationV + parent.label(); 
				newLabelSB.insert(0, prependString);
				parent = parent.parent;
				if (parent==null) break;
			}
			newLabelSB.insert(0,t.label());
			resultLabel = Label.getLabel(newLabelSB.toString());
		}
		
		TSNodeLabel result = new TSNodeLabel(resultLabel, false);		
		int prole = t.prole();
		TSNodeLabel[] newDaughters = new TSNodeLabel[prole];
		result.daughters = newDaughters;
		if (!t.isTerminal()) {
			for(int i=0; i<prole; i++) {
				TSNodeLabel d = performRealParentAnnotation(t.daughters[i]);
				newDaughters[i] = d;
				d.parent = result;
			}
		}
		return result;
	}
	
	public static TSNodeLabel undoParentAnnotation(TSNodeLabel t) {
		if (markV<=1) return t;
		return undoParentAnnotationReal(t);
	}

	private static TSNodeLabel undoParentAnnotationReal(TSNodeLabel t) {
		if (t.isLexical) {
			return new TSNodeLabel(t.label, true); 
		}
		String labelString = t.label();
		int index = labelString.indexOf(catSeparationV);
		Label newLabel = index==-1 ? t.label : Label.getLabel(labelString.substring(0, index));
		TSNodeLabel result = new TSNodeLabel(newLabel, false);
		if (!t.isTerminal()) {
			int prole = t.prole();
			TSNodeLabel[] newDaughters = new TSNodeLabel[prole];
			result.daughters = newDaughters;
			for(int i=0; i<prole; i++) {
				TSNodeLabel d = undoParentAnnotationReal(t.daughters[i]);
				newDaughters[i] = d;
				d.parent = result;
			}
		}
		return result;
	}
	
	public static Label buildArtificialLabel(String parentLabel,  
			BoundedLinkedList<String> previousSiblings) {
		
		StringBuilder sb = new StringBuilder(parentLabel);
		sb.append(parentLabelSeparator);
		//sb.append(parentLabelSeparator);
		
		Iterator<String> iter = previousSiblings.getList().iterator();
		while(iter.hasNext()) {
			String s = iter.next();
			sb.append(s);
			if (iter.hasNext())
				sb.append(catSeparationH);
		}			
		
	    return Label.getLabel(sb.toString());
	}
	
	/*
	
	public static Label buildArtificialLabel(String parentLabel,  
			BoundedLinkedList<String> previousSiblings) {
		
		StringBuilder sb = new StringBuilder();
		sb.append(artificialNodesBracketsStart);
		sb.append(parentLabel);
		sb.append(parentLabelSeparator);
		
		LinkedList<String> list = previousSiblings.getList();
		
		if (!list.isEmpty()) {
			Iterator<String> i = list.iterator();			
			for (;;) {
			    String e = i.next();
			    sb.append(e);
			    if (! i.hasNext()) break;			    	
			    sb.append(catSeparationH);
			}

		}				
		
		sb.append(artificialNodesBracketsEnd);
	    return Label.getLabel(sb.toString());
	}
	
	*/
	 
	
	public static boolean isArtificialNode(TSNodeLabel t) {
		//return t.label().indexOf(artificialNodesBracketsStart)==0;
		return t.label().indexOf(parentLabelSeparator)!=-1;
	}
	
	public static void main1(String[] args) throws Exception {		
		
		markV=3;
		
		TSNodeLabel t = new TSNodeLabel("(A B (C F G H) (D I) (E (J K L)))");		
		System.out.println(t);
		TSNodeLabel parentAnnotatedTree = performParentAnnotation(t);
		System.out.println(parentAnnotatedTree);
		System.out.println(parentAnnotatedTree.checkParentDaughtersConsistency());
		System.out.println(parentAnnotatedTree.checkOnlyAndAllTerminalsAreLexical());
		TSNodeLabel original = undoParentAnnotation(parentAnnotatedTree);
		System.out.println(original);
		System.out.println(original.equals(t));
		System.out.println(original.checkParentDaughtersConsistency());
		System.out.println(original.checkOnlyAndAllTerminalsAreLexical());
		
	}

	public static void main(String[] args) throws Exception {		
		
		String markovH = "-markovH:";
		String markovV = "-markovV:";
		
		markH = 1; 
		markV = 2;
		
		String usage = "USAGE: java [-Xmx1G] TreeMarkoBinarization" +
		"[-markovH:2] [-markovV:2] inputFile outputFile";		
		
		//READING PARAMETERS
		if (args.length<2 || args.length>4) {
			System.err.println("Incorrect number of arguments: " + args.length);
			System.err.println(usage);
			System.exit(-1);
		}	
		
		int i=0;
		while(i<args.length-2) {
			String option = args[i++];
			if (option.startsWith(markovH))
				markH = ArgumentReader.readIntOption(option);
			else if (option.startsWith(markovV))
				markV = ArgumentReader.readIntOption(option);
		}
		
		File inputFile = new File(args[i++]);
		File outputFile = new File(args[i]);
		
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(inputFile);
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		
		TreeMarkoBinarization treeMarkovBinarizer = new TreeMarkoBinarizationLeft_LC();		
		
		for(TSNodeLabel t : treebank) {
			TSNodeLabel transformed = treeMarkovBinarizer.performMarkovBinarization(t);
			pw.println(transformed.toString());
		}
				
		pw.close();		
		
    }

	
}
