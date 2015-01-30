package tsg.parsingExp;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;

import tsg.Label;
import tsg.TSNodeLabel;
import util.ArgumentReader;
import util.file.FileUtil;

public class MarkoBinarizationOld {
	
	static char binarizeLevelMarker = '*';
	static char catSeparation = '|';
	static char nullNode = '#';
	static boolean oldMarkoBinarization = false;	
	public static int markH=1, markV=1;	
	public static boolean markovize = true;
	public static boolean binarize = true;
	
	public static void setHV(int h, int v) {
		markH = h;
		markV = v;
	}
	
	public static boolean isMarkoBinarizationActive() {
		return binarize || markovize;
	}
	
	public static ArrayList<TSNodeLabel> markoBinarizeTreebank(ArrayList<TSNodeLabel> treebank) {
		if (oldMarkoBinarization) return oldMarkoBinarizeTreebank(treebank);
		else return newMarkoBinarizeTreebank(treebank);		
	}
	
	public static TSNodeLabel undoMarkoBinarization(TSNodeLabel tree) {
		if (oldMarkoBinarization) return oldUndoMarkoBinarization(tree);
		else return newUndoMarkoBinarization(tree);		
	}
	
	public static TSNodeLabel markoBinarize(TSNodeLabel tree) {
		if (oldMarkoBinarization) return leftBinarizeH1V1(tree);

		TSNodeLabel result = markovizeTree(tree);
		result = leftBinarize(result);
		return result;
	}
	

	private static TSNodeLabel newUndoMarkoBinarization(TSNodeLabel tree) {
		tree = undoLeftBinarize(tree);
		tree = unmarkovizeTree(tree);
		return tree;		
	}
	
	private static TSNodeLabel oldUndoMarkoBinarization(TSNodeLabel tree) {
		return undoLeftBinarizeH1V1(tree);		
	}


	private static ArrayList<TSNodeLabel> newMarkoBinarizeTreebank(ArrayList<TSNodeLabel> treebank) {
		ArrayList<TSNodeLabel> result = new ArrayList<TSNodeLabel>();
		for(TSNodeLabel t : treebank) {
			t = markovizeTree(t);
			t = leftBinarize(t);
			result.add(t);
		}
		return result;
	}

	private static ArrayList<TSNodeLabel> oldMarkoBinarizeTreebank(ArrayList<TSNodeLabel> treebank) {
		ArrayList<TSNodeLabel> result = new ArrayList<TSNodeLabel>();
		for(TSNodeLabel t : treebank) {
			t = leftBinarizeH1V1(t);
			result.add(t);
		}
		return result;
	}

	public static TSNodeLabel markovizeTree(TSNodeLabel thisNode) {		
		if (thisNode.isLexical) {
			TSNodeLabel result = new TSNodeLabel(thisNode.label, true);
			return result;
		}
		StringBuilder newLabel = new StringBuilder(thisNode.label());
		TSNodeLabel chainUp = thisNode;
		for(int i=0; i<markV; i++) {
			chainUp = chainUp==null ? null : chainUp.parent;
			newLabel.append(catSeparation);
			newLabel.append(chainUp==null ? nullNode : chainUp.label());
		}		
		newLabel.append(catSeparation);
		TSNodeLabel parent = thisNode.parent;
		if (parent==null) {
			for(int i=0; i<markH; i++) {
				newLabel.append(catSeparation);
				newLabel.append(nullNode);
			}
		}
		else {
			int index = parent.indexOfDaughter(thisNode);
			for(int i=0; i<markH; i++) {
				index--;
				newLabel.append(catSeparation);
				newLabel.append(index<0 ? nullNode : parent.daughters[i].label());
			}
		}
		
		TSNodeLabel result = new TSNodeLabel(Label.getLabel(newLabel.toString()), false);
		int prole = thisNode.prole();
		TSNodeLabel[] newDaughters = new TSNodeLabel[prole]; 
		result.daughters = newDaughters;
		for(int i=0; i<prole; i++) {
			TSNodeLabel d = markovizeTree(thisNode.daughters[i]);
			d.parent = result;
			newDaughters[i] = d;
		}
					
		return result;
	}
	
	public static TSNodeLabel unmarkovizeTree(TSNodeLabel thisNode) {
		if (thisNode.isLexical) {
			TSNodeLabel result = new TSNodeLabel(thisNode.label, true);
			return result;
		}
		
		String labelString = thisNode.label();
		int indexCatSeparation = labelString.indexOf(catSeparation); 
		Label newLabel = Label.getLabel(labelString.substring(0,indexCatSeparation));
		
		TSNodeLabel result = new TSNodeLabel(newLabel, false);
				
		int prole = thisNode.prole();
		TSNodeLabel[] newDaughters = new TSNodeLabel[prole]; 
		result.daughters = newDaughters;
		for(int i=0; i<prole; i++) {
			TSNodeLabel d = unmarkovizeTree(thisNode.daughters[i]);
			d.parent = result;
			newDaughters[i] = d;
		}
					
		return result;
	}

	
	public static TSNodeLabel leftBinarize(TSNodeLabel thisNode) {
		int prole = thisNode.prole();
		TSNodeLabel binaryCopy = new TSNodeLabel(thisNode.label, thisNode.isLexical);
		if (prole==0) return binaryCopy;
		if (prole==1) {
			TSNodeLabel onlyDaughter = leftBinarize(thisNode.firstDaughter());
			binaryCopy.daughters = new TSNodeLabel[]{onlyDaughter};
			onlyDaughter.parent = binaryCopy;
			return binaryCopy;
		}
		
		TSNodeLabel left = thisNode.daughters[0];
		TSNodeLabel right = thisNode.daughters[1];
		if (prole==2) {
			TSNodeLabel leftCopy = leftBinarize(left);
			TSNodeLabel rightCopy = leftBinarize(right);						
			binaryCopy.daughters = new TSNodeLabel[]{leftCopy, rightCopy};
			leftCopy.parent = binaryCopy;
			rightCopy.parent = binaryCopy;
			return binaryCopy;
		}
		
		Label levelParentLabel = Label.getLabel(thisNode.label() + binarizeLevelMarker);
		TSNodeLabel levelParent = new TSNodeLabel(levelParentLabel, false);
		TSNodeLabel leftCopy = leftBinarize(left);
		TSNodeLabel rightCopy = leftBinarize(right);
		levelParent.daughters = new TSNodeLabel[]{leftCopy, rightCopy};
		leftCopy.parent = levelParent;
		rightCopy.parent = levelParent;
		
		for(int i=2; i<prole; i++) {
			TSNodeLabel newLevelParent = binaryCopy;
			if (i<prole-1) {
				Label newLevelParentLabel = Label.getLabel(thisNode.label() + binarizeLevelMarker);
				newLevelParent = new TSNodeLabel(newLevelParentLabel, false);
			}				
			right = thisNode.daughters[i];
			rightCopy = leftBinarize(right);			
			newLevelParent.daughters = new TSNodeLabel[]{levelParent, rightCopy};
			levelParent.parent = newLevelParent;
			rightCopy.parent = newLevelParent;
			levelParent = newLevelParent;
		}		
		return binaryCopy;
	}
	
	public static TSNodeLabel undoLeftBinarize(TSNodeLabel thisNode) {
		int prole = thisNode.prole();
		TSNodeLabel unbinaryCopy = new TSNodeLabel(thisNode.label, thisNode.isLexical);
		if (prole==0) return unbinaryCopy;
		if (prole==1) {
			TSNodeLabel onlyDaughter = undoLeftBinarize(thisNode.firstDaughter());
			unbinaryCopy.daughters = new TSNodeLabel[]{onlyDaughter};			
			onlyDaughter.parent = unbinaryCopy;
			return unbinaryCopy;
		}
		
		ArrayList<TSNodeLabel> newDaughters = new ArrayList<TSNodeLabel>();
		TSNodeLabel currentLevel = thisNode;
		do {
			newDaughters.add(undoLeftBinarize(currentLevel.daughters[1]));
			currentLevel = currentLevel.daughters[0];
		} while(currentLevel.label().indexOf(binarizeLevelMarker)!=-1);
		newDaughters.add(undoLeftBinarize(currentLevel));
		int newProle = newDaughters.size();
		unbinaryCopy.daughters = new TSNodeLabel[newProle];
		int i=newProle-1;
		for(TSNodeLabel d : newDaughters) {
			unbinaryCopy.daughters[i--] = d;
			d.parent = unbinaryCopy;
		}
		return unbinaryCopy;
	}


	public static TSNodeLabel leftBinarizeH1V1(TSNodeLabel thisNode) {
		int prole = thisNode.prole();
		TSNodeLabel binaryCopy = new TSNodeLabel(thisNode.label, thisNode.isLexical);
		if (prole==0) return binaryCopy;
		if (prole==1) {
			TSNodeLabel onlyDaughter = leftBinarizeH1V1(thisNode.firstDaughter());
			binaryCopy.daughters = new TSNodeLabel[]{onlyDaughter};
			onlyDaughter.parent = binaryCopy;
			return binaryCopy;
		}
		
		TSNodeLabel left = thisNode.daughters[0];
		TSNodeLabel right = thisNode.daughters[1];
		if (prole==2) {
			TSNodeLabel leftCopy = leftBinarizeH1V1(left);
			TSNodeLabel rightCopy = leftBinarizeH1V1(right);						
			binaryCopy.daughters = new TSNodeLabel[]{leftCopy, rightCopy};
			leftCopy.parent = binaryCopy;
			rightCopy.parent = binaryCopy;
			return binaryCopy;
		}
		
		TSNodeLabel next = thisNode.daughters[2];
		Label levelParentLabel = Label.getLabel(thisNode.label() + catSeparation + next.label());
		TSNodeLabel levelParent = new TSNodeLabel(levelParentLabel, false);
		TSNodeLabel leftCopy = leftBinarizeH1V1(left);
		TSNodeLabel rightCopy = leftBinarizeH1V1(right);
		levelParent.daughters = new TSNodeLabel[]{leftCopy, rightCopy};
		leftCopy.parent = levelParent;
		rightCopy.parent = levelParent;
		
		for(int i=2; i<prole; i++) {
			TSNodeLabel newLevelParent = binaryCopy;
			if (i<prole-1) {
				Label newLevelParentLabel = Label.getLabel(
						thisNode.label() + catSeparation + thisNode.daughters[i+1].label());
				newLevelParent = new TSNodeLabel(newLevelParentLabel, false);
			}				
			right = thisNode.daughters[i];
			rightCopy = leftBinarizeH1V1(right);			
			newLevelParent.daughters = new TSNodeLabel[]{levelParent, rightCopy};
			levelParent.parent = newLevelParent;
			rightCopy.parent = newLevelParent;
			levelParent = newLevelParent;
		}		
		return binaryCopy;
	}
	
	public static TSNodeLabel undoLeftBinarizeH1V1(TSNodeLabel thisNode) {
		int prole = thisNode.prole();
		TSNodeLabel unbinaryCopy = new TSNodeLabel(thisNode.label, thisNode.isLexical);
		if (prole==0) return unbinaryCopy;
		if (prole==1) {
			TSNodeLabel onlyDaughter = undoLeftBinarizeH1V1(thisNode.firstDaughter());
			unbinaryCopy.daughters = new TSNodeLabel[]{onlyDaughter};			
			onlyDaughter.parent = unbinaryCopy;
			return unbinaryCopy;
		}
		
		ArrayList<TSNodeLabel> newDaughters = new ArrayList<TSNodeLabel>();
		TSNodeLabel currentLevel = thisNode;
		do {
			newDaughters.add(undoLeftBinarizeH1V1(currentLevel.daughters[1]));
			currentLevel = currentLevel.daughters[0];
		} while(currentLevel.label().indexOf(catSeparation)!=-1);
		newDaughters.add(undoLeftBinarizeH1V1(currentLevel));
		int newProle = newDaughters.size();
		unbinaryCopy.daughters = new TSNodeLabel[newProle];
		int i=newProle-1;
		for(TSNodeLabel d : newDaughters) {
			unbinaryCopy.daughters[i--] = d;
			d.parent = unbinaryCopy;
		}
		return unbinaryCopy;
	}
		

	public static void main1(String[] args) throws Exception {
		
		//TSNodeLabel t = new TSNodeLabel("(TOP|#||# (S|TOP||# (NP|S||# (NP|NP||# (DT|NP||# The) (NN|NP||DT economy) (POS|NP||DT 's)) (NN|NP||NP temperature)) (VP|S||NP (MD|VP||# will) (VP|VP||MD (VB|VP||# be) (VP|VP||VB (VBN|VP||# taken) (PP|VP||VBN (IN|PP||# from) (NP|PP||IN (NP|NP||# (JJ|NP||# several) (NN|NP||JJ vantage) (NNS|NP||JJ points)) (NP|NP||NP (DT|NP||# this) (NN|NP||DT week)))) (,|VP||VBN ,) (PP|VP||VBN (IN|PP||# with) (NP|PP||IN (NP|NP||# (NNS|NP||# readings)) (PP|NP||NP (IN|PP||# on) (NP|PP||IN (NN|NP||# trade) (,|NP||NN ,) (NN|NP||NN output) (,|NP||NN ,) (NN|NP||NN housing) (CC|NP||NN and) (NN|NP||NN inflation)))))))) (.|S||NP .)))");
		TSNodeLabel t = new TSNodeLabel("(A B C D E)");
		TSNodeLabel markoBinarizedTree = markoBinarize(t);
		System.out.println(markoBinarizedTree);
		System.out.println(markoBinarizedTree.checkParentDaughtersConsistency());
		System.out.println(markoBinarizedTree.checkOnlyAndAllTerminalsAreLexical());
		TSNodeLabel original = undoMarkoBinarization(markoBinarizedTree);
		System.out.println(original);		
		System.out.println(original.checkParentDaughtersConsistency());
		System.out.println(original.checkOnlyAndAllTerminalsAreLexical());
		
	}
	
		
	public static void main(String[] args) throws Exception {
		
		String binarizeOption = "-binarize:";
		String markovH = "-markovH:";
		String markovV = "-markovV:";
		
		markH=1; 
		markV=1;
		
		binarize = true;
		
		String usage = "USAGE: java [-Xmx1G] TreeMarkoBinarization" +
		"[-markovH:1] [-markovV:1] [-binarize:true] inputFile outputFile";		
		
		//READING PARAMETERS
		if (args.length<2 || args.length>9) {
			System.err.println("Incorrect number of arguments: " + args.length);
			System.err.println(usage);
			System.exit(-1);
		}	
		
		int i=0;
		while(i<args.length-2) {
			String option = args[i++];
			if (option.startsWith(binarizeOption))
				binarize = ArgumentReader.readBooleanOption(option);
			else if (option.startsWith(markovH))
				markH = ArgumentReader.readIntOption(option);
			else if (option.startsWith(markovV))
				markV = ArgumentReader.readIntOption(option);
		}
		
		File inputFile = new File(args[i++]);
		File outputFile = new File(args[i]);
		
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(inputFile);
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		boolean markovize = markH>0 || markV>0;
		
		for(TSNodeLabel t : treebank) {
			TSNodeLabel transformed1 = markovize ? markovizeTree(t) : t;
			TSNodeLabel transformed2 = binarize ? leftBinarize(transformed1) : transformed1;
			pw.println(transformed2.toString());
		}		
		pw.close();		
		
    }

	
}
