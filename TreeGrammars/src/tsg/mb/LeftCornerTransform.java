package tsg.mb;

import java.io.File;
import java.io.PrintWriter;
import java.security.Policy.Parameters;
import java.util.Scanner;

import tsg.Label;
import tsg.TSNodeLabel;
import util.ArgumentReader;
import util.file.FileUtil;

public class LeftCornerTransform {

	
	public static String separator = "-";
	public static boolean emptyNodes = false;
	
	public static TSNodeLabel convertToLC(TSNodeLabel tree) {
		
		if (tree.isPreLexical())
			return tree.clone();
		
		TSNodeLabel leftCornerLex = tree.getLeftmostLexicalNode();
		TSNodeLabel leftCornerPos = leftCornerLex.parent;
		
		TSNodeLabel root = new TSNodeLabel(tree.label, false);
		Label rootLCLabel = Label.getLabel(tree.label() + separator + leftCornerPos.label());
		TSNodeLabel rootLCnode = new TSNodeLabel(rootLCLabel, false);
		TSNodeLabel[] newDaughters = new TSNodeLabel[2];
		newDaughters[0] = leftCornerPos.clone();
		newDaughters[1] = rootLCnode;
		root.assignDaughters(newDaughters);
		convertToLeftCornerSpine(tree, leftCornerPos, rootLCnode);
		return root;
	}
	
	private static void convertToLeftCornerSpine(TSNodeLabel tree, TSNodeLabel previousLCnode, TSNodeLabel rootLCnode) {
		
		TSNodeLabel higherLeftCorner = previousLCnode.parent;
		boolean reachedTop = higherLeftCorner==tree;
		boolean oneLessDaughters = reachedTop && !emptyNodes; 
		TSNodeLabel[] daughters = higherLeftCorner.daughters;
		int prole = daughters.length;
		int newProle = oneLessDaughters ? prole-1 : prole;
		TSNodeLabel[] newDaughters = new TSNodeLabel[newProle];
		for(int i=1; i<prole; i++) {
			TSNodeLabel d = daughters[i];
			newDaughters[i-1] = convertToLC(d);			
		}
		if (!oneLessDaughters) {
			Label lastNewDaughterLabel = reachedTop ?
				Label.getLabel(tree.label() + separator + tree.label()) :
				Label.getLabel(tree.label() + separator + higherLeftCorner.label());
			TSNodeLabel newRootLCnode = new TSNodeLabel(lastNewDaughterLabel, false);
			newDaughters[newProle-1] = newRootLCnode;
			if (!reachedTop)
				convertToLeftCornerSpine(tree, higherLeftCorner, newRootLCnode);
		}
		if (newProle!=0)
			rootLCnode.assignDaughters(newDaughters);
	}

	public static TSNodeLabel convertFromLC(TSNodeLabel tree) {
		
		if (tree.isPreLexical())
			return tree.clone();
		
		assert isGoalTransformed(tree);
		
		String goalLabelSep = tree.label() + separator;
		TSNodeLabel higherLc = tree.daughters[1];
		while(!higherLc.isTerminal()) {
			TSNodeLabel lastDaughter = higherLc.lastDaughter();		
			if (!lastDaughter.label().startsWith(goalLabelSep))
				break;
			higherLc = lastDaughter;
		}
		
		if (emptyNodes)
			higherLc = higherLc.parent;
		
		TSNodeLabel result = new TSNodeLabel(tree.label, false);
		TSNodeLabel[] daughters = higherLc.daughters;
		int prole = higherLc.prole();
		if (!emptyNodes)
			prole++;
		TSNodeLabel[] newDaughters = new TSNodeLabel[prole];		
		for(int i=0; i<prole-1; i++) {
			newDaughters[i+1] = convertFromLC(daughters[i]);
		}				
		newDaughters[0] = convertFromLCspine(tree, higherLc);
		
		result.assignDaughters(newDaughters);
		return result;
	}
	
	private static TSNodeLabel convertFromLCspine(TSNodeLabel goal, TSNodeLabel leftCornerNode) {
			
		TSNodeLabel p = leftCornerNode.parent; 
		if (p==goal)
			return goal.firstDaughter().clone();
			
		String higherLClabel = leftCornerNode.label();
		Label baseLabel = Label.getLabel(
				higherLClabel.substring(higherLClabel.indexOf(separator)+separator.length()));
		TSNodeLabel result = new TSNodeLabel(baseLabel, false);
		int prole = p.prole();
		TSNodeLabel[] daughters = p.daughters;
		TSNodeLabel[] newDaughters = new TSNodeLabel[prole];		
		for(int i=0; i<prole-1; i++) {
			newDaughters[i+1] = convertFromLC(daughters[i]);
		}	
		newDaughters[0] = convertFromLCspine(goal,p);
		result.assignDaughters(newDaughters);
		return result;
	}

	private static boolean isGoalTransformed(TSNodeLabel t) {
		if (t.prole()!=2)
			return false;
		String d2 = t.daughters[1].label();
		if (!d2.contains(separator))
			return false;
		String root = t.label();
		String d1 = t.daughters[0].label();
		return d2.equals(root + separator + d1);		
	}
	
	private static void leftCornerTransform(File inputFile, File outputFile, boolean backTransform) throws Exception {
		Scanner scan = FileUtil.getScanner(inputFile);
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		
		if (backTransform) {
			while(scan.hasNextLine()) {
				String line = scan.nextLine();
				TSNodeLabel t = new TSNodeLabel(line);
				TSNodeLabel z = convertFromLC(t);
				pw.println(z);
			}
		}		
		else {
			while(scan.hasNextLine()) {
				String line = scan.nextLine();
				TSNodeLabel t = new TSNodeLabel(line);
				TSNodeLabel z = convertToLC(t);
				pw.println(z);
			}
		}
		
		pw.close();
	}
	
	public static void main(String[] args) throws Exception {
		emptyNodes = false;
		separator = "-";
		//TSNodeLabel t = new TSNodeLabel("(S (A (C c) (D (E e) (F f))) (B (G g) (H (I i) (L l))))");
		//TSNodeLabel t = new TSNodeLabel("(S (A (C (C' c)) (D (D' (E e) (F f)))) (B (G g) (H (I i) (L l))))");
		//TSNodeLabel t = new TSNodeLabel("(S (A (C c) (D (E e) (F f)) (J j)) (B (G g) (H (I i) (L l))(K k)))");
		//TSNodeLabel t = new TSNodeLabel("(S (A (C (J j) (K k)) (D (E e) (F f)) ) (B (G g) (H (I i) (L l))))");
		//TSNodeLabel t = new TSNodeLabel("(S (PP (IN In) (NP (NP (DT an) (NNP Oct.) (CD 19) (NN review)) (PP (IN of) (NP (NP (DT The) (NN Misanthrope)) (PP (IN at) (NP (NP (NNP Chicago) (POS 's)) (NNP Goodman) (NNP Theatre))))) (PRN (-LRB- -LRB-) (S (NP (VBN Revitalized) (NNS Classics)) (VP (VBP Take) (NP (DT the) (NN Stage)) (PP (IN in) (NP (NNP Windy) (NNP City))))) (, ,) (NP (NN Leisure) (CC &) (NNS Arts)) (-RRB- -RRB-)))) (, ,) (NP (NP (NP (DT the) (NN role)) (PP (IN of) (NP (NNP Celimene)))) (, ,) (VP (VBN played) (PP (IN by) (NP (NNP Kim) (NNP Cattrall)))) (, ,)) (VP (AUX was) (VP (ADVP (RB mistakenly)) (VBN attributed) (PP (TO to) (NP (NNP Christina) (NNP Haag))))) (. .))");
		//TSNodeLabel t = new TSNodeLabel("(PP (IN of) (NP (NNP Celimene)))");
		TSNodeLabel t  = new TSNodeLabel("(PP (IN of) (NP/NNP Celimene))");
		TSNodeLabel tlc = convertToLC(t);				
		System.out.println(t.toStringTex(false, false));	
		System.out.println(tlc.toStringTex(false, false));
		TSNodeLabel btlc = convertFromLC(tlc);
		System.out.println(btlc.toStringTex(false, false));
		System.out.println(t.equals(btlc));
	}
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void mainM(String[] args) throws Exception {
		
		String backTransformOption = "-backTransform:";
		String emptyNodesOption = "-emptyNodes:";
		String separatorOption = "-separator:";
		
		File inputFile = null;
		File outputFile = null;
		boolean backTransform = false;
		
		String usage = "USAGE: java -jar LeftCornerTranform.jar " +
				"[-backTransform:false] " +
				"[-emptyNodes:false] " +
				"[-separator:-] " +
				"inputFile " +
				"outputFile";
		
		int argsNum = args.length;
		
		
		if (argsNum<2 || argsNum>5) {
			System.err.println("Invalid number of arguments");
			System.err.println(usage);
			return;
		}
		
		
		for(int i=0; i<argsNum-2; i++) {
			String a = args[i];
			if (a.startsWith(backTransformOption))
				backTransform = ArgumentReader.readBooleanOption(a);
			else if (a.startsWith(emptyNodesOption))
				emptyNodes = ArgumentReader.readBooleanOption(a);
			else if (a.startsWith(separatorOption))
				separator = ArgumentReader.readStringOption(a);
		}		
				
		inputFile = new File(args[argsNum-2]);
		outputFile = new File(args[argsNum-1]);
		
		leftCornerTransform(inputFile, outputFile, backTransform);
		
	}



}
