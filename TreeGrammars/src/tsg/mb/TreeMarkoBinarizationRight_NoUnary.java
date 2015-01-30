package tsg.mb;

import java.util.ArrayList;

import tsg.Label;
import tsg.TSNodeLabel;
import util.BoundedLinkedList;

public class TreeMarkoBinarizationRight_NoUnary extends TreeMarkoBinarization {
	
	
	public TSNodeLabel performMarkovBinarization(TSNodeLabel t) {
		TSNodeLabel result = performParentAnnotation(t);
		return performMarkoBinarizationRightNoUnary(result);
	}
	
	public TSNodeLabel undoMarkovBinarization(TSNodeLabel t) {
		TSNodeLabel result = undoMarkoBinarizationRightNoUnary(t);
		return undoParentAnnotation(result);
	}

	
	private TSNodeLabel performMarkoBinarizationRightNoUnary(TSNodeLabel t) {
		int prole = t.prole();
		TSNodeLabel binaryCopy = new TSNodeLabel(t.label, t.isLexical);
		if (prole==0) return binaryCopy;
		if (prole==1) {
			TSNodeLabel onlyDaughter = performMarkoBinarizationRightNoUnary(t.firstDaughter());
			binaryCopy.daughters = new TSNodeLabel[]{onlyDaughter};
			onlyDaughter.parent = binaryCopy;
			return binaryCopy;
		}
		
		BoundedLinkedList<String> lastSiblings = new BoundedLinkedList<String>(markH);
		String parentLabelString = t.label();
		
		TSNodeLabel previousDaughter = performMarkoBinarizationRightNoUnary(t.firstDaughter());
		String previousDaughterLabelString = previousDaughter.label(); 
		lastSiblings.add(previousDaughterLabelString);
		Label previousLevelLabelString = buildArtificialLabel(parentLabelString, lastSiblings);
		TSNodeLabel previousLevel = new TSNodeLabel(previousLevelLabelString, false);
		binaryCopy.assignUniqueDaughter(previousLevel);
		
		for(int i=1; i<prole-1; i++) {
			
			TSNodeLabel currentDaughter = performMarkoBinarizationRightNoUnary(t.daughters[i]);			
			TSNodeLabel left = previousDaughter;
			lastSiblings.add(currentDaughter.label());
			Label newLevelLabel = buildArtificialLabel(parentLabelString, lastSiblings);
			TSNodeLabel newLevel = new TSNodeLabel(newLevelLabel, t.isLexical);			
			previousLevel.assignDaughters(new TSNodeLabel[]{left,newLevel});			
			previousLevel = newLevel;	
			previousDaughter = currentDaughter;
			
		}
		
		TSNodeLabel lastDaughter = performMarkoBinarizationRightNoUnary(t.daughters[prole-1]);
		previousLevel.assignDaughters(new TSNodeLabel[]{previousDaughter, lastDaughter});
		
		return binaryCopy;
	}
	

	
	private TSNodeLabel undoMarkoBinarizationRightNoUnary(TSNodeLabel t) {
		int prole = t.prole();
		TSNodeLabel unbinaryCopy = new TSNodeLabel(t.label, t.isLexical);
		if (prole==0) return unbinaryCopy;
		if (prole==1) {
			TSNodeLabel onlyDaughter = undoMarkoBinarizationRightNoUnary(t.firstDaughter());
			if (isArtificialNode(onlyDaughter)) {				
				unbinaryCopy.assignDaughters(onlyDaughter.daughters);
				return unbinaryCopy;
			}
			unbinaryCopy.assignUniqueDaughter(onlyDaughter);
			return unbinaryCopy;
		}
		
		ArrayList<TSNodeLabel> newDaughters = new ArrayList<TSNodeLabel>();
		TSNodeLabel currentLevel = t;
		do {
			newDaughters.add(undoMarkoBinarizationRightNoUnary(currentLevel.firstDaughter()));
			currentLevel = currentLevel.daughters[1];
		} while(isArtificialNode(currentLevel));
		newDaughters.add(undoMarkoBinarizationRightNoUnary(currentLevel));
		int newProle = newDaughters.size();
		unbinaryCopy.daughters = new TSNodeLabel[newProle];
		int i=0;
		for(TSNodeLabel d : newDaughters) {
			unbinaryCopy.daughters[i++] = d;
			d.parent = unbinaryCopy;
		}
		return unbinaryCopy;
	}


	@Override
	public String getDescription() {
		return "TreeMarkoBinarizationRight_New";
	}

	
	public static void main(String[] args) throws Exception {
		
		markH=0; 
		markV=1;	
		
		
		//for(int i=0; i<10000; i++) {
			//TSNodeLabel t = TSNodeLabel.getRandomTree(new String[]{"A","B","C","D","E","F","G"}, 
			//		new String[]{"a","b","c","d","e","f","g"}, 5, new double[]{.2,.2,.2,.2,.2}, 
			//		true, .2, 2, 5);
			//TSNodeLabel t = new TSNodeLabel("(E (C (A a ) ) (E (A c ) (F c ) (D g ) ) (G (D f ) (D a ) (C c ) (G e ) ) (C (B f ) ) )");
			//TSNodeLabel t = new TSNodeLabel("(A B C D E)");
			//for(TSNodeLabel d : t.daughters) d.isLexical = false;
			TSNodeLabel t = new TSNodeLabel("(S (PP (IN In) (NP (NP (DT an) (NNP Oct.) (CD 19) (NN review)) (PP (IN of) (NP (NP (DT The) (NN Misanthrope)) (PP (IN at) (NP (NP (NNP Chicago) (POS 's)) (NNP Goodman) (NNP Theatre))))) (PRN (-LRB- -LRB-) (S (NP (VBN Revitalized) (NNS Classics)) (VP (VBP Take) (NP (DT the) (NN Stage)) (PP (IN in) (NP (NNP Windy) (NNP City))))) (, ,) (NP (NN Leisure) (CC &) (NNS Arts)) (-RRB- -RRB-)))) (, ,) (NP (NP (NP (DT the) (NN role)) (PP (IN of) (NP (NNP Celimene)))) (, ,) (VP (VBN played) (PP (IN by) (NP (NNP Kim) (NNP Cattrall)))) (, ,)) (VP (AUX was) (VP (ADVP (RB mistakenly)) (VBN attributed) (PP (TO to) (NP (NNP Christina) (NNP Haag))))) (. .))");
			System.out.println(t.toStringTex(false, false));
			TreeMarkoBinarizationRight_NoUnary tmb = new TreeMarkoBinarizationRight_NoUnary();
			TSNodeLabel markoBinarizedTree = tmb.performMarkovBinarization(t);
			System.out.println(markoBinarizedTree.toStringTex(false, false));
			System.out.println(markoBinarizedTree.checkParentDaughtersConsistency());
			System.out.println(markoBinarizedTree.checkOnlyAndAllTerminalsAreLexical());
			TSNodeLabel original = tmb.undoMarkovBinarization(markoBinarizedTree);
			System.out.println(original.toStringTex(false, false));
			System.out.println(original.equals(t));
			System.out.println(original.checkParentDaughtersConsistency());
			System.out.println(original.checkOnlyAndAllTerminalsAreLexical());
			//if (!original.equals(t)) {
			//	System.out.println(t);
			//}
		//}
	}

	
}
