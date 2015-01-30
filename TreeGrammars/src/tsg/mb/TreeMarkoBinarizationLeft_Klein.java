package tsg.mb;

import java.io.File;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.ArrayList;

import settings.Parameters;
import tsg.HeadLabeler;
import tsg.Label;
import tsg.TSNodeLabel;
import tsg.utils.CleanPetrov;
import util.BoundedLinkedList;
import util.file.FileUtil;
import wordModel.UkWordMapping;
import wordModel.UkWordMappingPetrov;

public class TreeMarkoBinarizationLeft_Klein extends TreeMarkoBinarization {
	
	static HeadLabeler defaultHL = new HeadLabeler(new File(Parameters.codeBase + "resources/fede.rules_04_08_10"));
		
	public TSNodeLabel performMarkovBinarization(TSNodeLabel t) {		
		TSNodeLabel result = performLexHeadAnnotation(t);
		result = performRealParentAnnotation(result);
		return performMarkoBinarizationLeft(result);
	}
	
	private TSNodeLabel performLexHeadAnnotation(TSNodeLabel t) {		
		TSNodeLabel result = t.clone();
		defaultHL.markHead(result);
		ArrayList<TSNodeLabel> interns = result.collectNonLexicalNodes();
		for(TSNodeLabel i : interns) {
			if (i.parent==null || i.isPreLexical()) continue; 
			String posTagHead = i.getPosLexHead().label();
			i.relabel(i.label() + "{" + posTagHead + "}");
		}
		return result;
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
				String prependString = catSeparationV + parent.label().replaceFirst("\\{.*\\}", "");; 
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
	
	private TSNodeLabel undoLexHeadAnnotation(TSNodeLabel t) {		
		TSNodeLabel result = t.clone();
		ArrayList<TSNodeLabel> interns = result.collectNonLexicalNodes();
		for(TSNodeLabel i : interns) {
			if (i.parent==null || i.isPreLexical()) continue;
			String l = i.label();
			int s = l.indexOf('{');
			int e = l.indexOf('}');
			String newLabel = l.substring(0,s) + l.substring(e+1);
			i.relabel(newLabel);
		}
		return result;
	}

	public TSNodeLabel undoMarkovBinarization(TSNodeLabel t) {
		TSNodeLabel result = undoMarkoBinarizationLeft(t);
		result = undoParentAnnotation(result);
		return undoLexHeadAnnotation(result);
	}

	
	private TSNodeLabel performMarkoBinarizationLeft(TSNodeLabel t) {
		int prole = t.prole();
		TSNodeLabel binaryCopy = new TSNodeLabel(t.label, t.isLexical);
		if (prole==0) return binaryCopy;
		if (prole==1) {
			TSNodeLabel onlyDaughter = performMarkoBinarizationLeft(t.firstDaughter());
			binaryCopy.assignUniqueDaughter(onlyDaughter);
			return binaryCopy;
		}
		if (prole==2) {
			TSNodeLabel firstDaughter = performMarkoBinarizationLeft(t.daughters[0]);
			TSNodeLabel secondDaughter = performMarkoBinarizationLeft(t.daughters[1]);
			binaryCopy.assignDaughters(new TSNodeLabel[]{firstDaughter,secondDaughter});
			return binaryCopy;
		}
		
		BoundedLinkedList<String> lastSiblings = new BoundedLinkedList<String>(markH);
		
		String parentLabelString = t.label();
		
		TSNodeLabel firstDaughter = performMarkoBinarizationLeft(t.daughters[0]);
		TSNodeLabel secondDaughter = performMarkoBinarizationLeft(t.daughters[1]);
		lastSiblings.add(firstDaughter.label());
		lastSiblings.add(secondDaughter.label());
		Label firstLevelLabel = TreeMarkoBinarization.buildArtificialLabel(parentLabelString, lastSiblings);
		TSNodeLabel previousLevel = new TSNodeLabel(firstLevelLabel, false);
		previousLevel.assignDaughters(new TSNodeLabel[]{firstDaughter, secondDaughter});		
		

		for(int i=2; i<prole-1; i++) {
			
			TSNodeLabel d = t.daughters[i];
			TSNodeLabel right = performMarkoBinarizationLeft(d);
			lastSiblings.add(d.label());
			Label newLevelLabel = TreeMarkoBinarization.buildArtificialLabel(parentLabelString, lastSiblings);
			TSNodeLabel newLevel = new TSNodeLabel(newLevelLabel, t.isLexical);			
			newLevel.assignDaughters(new TSNodeLabel[]{previousLevel,right});
			
			previousLevel = newLevel;						
			
		}
		
		TSNodeLabel lastDaughter = performMarkoBinarizationLeft(t.daughters[prole-1]);
		binaryCopy.assignDaughters(new TSNodeLabel[]{previousLevel,lastDaughter});
		
		return binaryCopy;
	}
	

	
	private TSNodeLabel undoMarkoBinarizationLeft(TSNodeLabel t) {
		int prole = t.prole();
		TSNodeLabel unbinaryCopy = new TSNodeLabel(t.label, t.isLexical);
		if (prole==0) return unbinaryCopy;
		if (prole==1) {
			TSNodeLabel onlyDaughter = undoMarkoBinarizationLeft(t.firstDaughter());
			unbinaryCopy.daughters = new TSNodeLabel[]{onlyDaughter};			
			onlyDaughter.parent = unbinaryCopy;
			return unbinaryCopy;
		}
		ArrayList<TSNodeLabel> newDaughters = new ArrayList<TSNodeLabel>();
		TSNodeLabel currentLevel = t;
		do {
			newDaughters.add(undoMarkoBinarizationLeft(currentLevel.lastDaughter()));
			currentLevel = currentLevel.daughters[0];
		} while(isArtificialNode(currentLevel));
		newDaughters.add(undoMarkoBinarizationLeft(currentLevel));
		int newProle = newDaughters.size();
		unbinaryCopy.daughters = new TSNodeLabel[newProle];
		int i=newProle-1;
		for(TSNodeLabel d : newDaughters) {
			unbinaryCopy.daughters[i--] = d;
			d.parent = unbinaryCopy;
		}
		return unbinaryCopy;
	}
	
	private static void transformTreebank(ArrayList<TSNodeLabel> treebank, File ouputFile) {
		PrintWriter pw = FileUtil.getPrintWriter(ouputFile);		
		
		TreeMarkoBinarization treeMarkovBinarizer = new TreeMarkoBinarizationLeft_Klein();		
		
		for(TSNodeLabel t : treebank) {
			TSNodeLabel transformed = treeMarkovBinarizer.performMarkovBinarization(t);
			pw.println(transformed.toString());
		}
				
		pw.close();
	}
	
	@Override
	public String getDescription() {
		return "TreeMarkoBinarizationLeft_Klein";
	}



	public static void main(String[] args) throws Exception {
		markH=1; 
		markV=2;
		TreeMarkoBinarizationLeft_Klein tmb = new TreeMarkoBinarizationLeft_Klein();
		TSNodeLabel tree = new TSNodeLabel("(S (S (NP (WDT That)) (VP (VBD got) (ADJP (RB hard) (SBAR (S (VP (TO to) (VP (VB take)))))))) (, ,) ('' '') (NP (PRP he)) (VP (VBD added)) (. .))");		
		System.out.println(tree.toStringQtree());
		TSNodeLabel mbTree = tmb.performMarkovBinarization(tree);
		System.out.println(mbTree.toStringQtree());
		System.out.println(mbTree);
		TSNodeLabel clean = tmb.undoMarkovBinarization(mbTree);		
		System.out.println(clean.equals(tree));
	}
	
	

	
	public static void main1(String[] args) throws Exception {				
		
		File trainingTreebankFile = new File(args[0]);
		File testTreebankFile = new File(args[1]);
		File outputTrainingTreebankFile = new File(args[2]);		
		File outputTestTreebankFile = new File(args[3]);
		markH = Integer.parseInt(args[4]);		
		markV = Integer.parseInt(args[5]);		
		UkWordMapping.ukThreashold = Integer.parseInt(args[6]);
						
		ArrayList<TSNodeLabel> trainingTreebank = TSNodeLabel.getTreebank(trainingTreebankFile);
		ArrayList<TSNodeLabel> testTreebank = TSNodeLabel.getTreebank(testTreebankFile);
		if (UkWordMapping.ukThreashold>0) {			
			UkWordMapping ukModel = new UkWordMappingPetrov();
			ukModel.init(trainingTreebank, testTreebank);
			trainingTreebank = ukModel.transformTrainingTreebank();
			testTreebank = ukModel.transformTestTreebank();
		}		
		
		transformTreebank(trainingTreebank, outputTrainingTreebankFile);
		transformTreebank(testTreebank, outputTestTreebankFile);		
				
	}
	
	
}
