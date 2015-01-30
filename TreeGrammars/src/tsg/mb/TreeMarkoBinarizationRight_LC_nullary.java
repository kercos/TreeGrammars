package tsg.mb;

import java.util.ArrayList;

import tsg.Label;
import tsg.TSNodeLabel;
import util.BoundedLinkedList;

public class TreeMarkoBinarizationRight_LC_nullary extends TreeMarkoBinarization {
	
	static Label epsilonLabel = Label.getLabel("_");
	
	public TSNodeLabel performMarkovBinarization(TSNodeLabel t) {
		TSNodeLabel result = performParentAnnotation(t);
		return performMarkoBinarizationRight(result);
	}
	
	public TSNodeLabel undoMarkovBinarization(TSNodeLabel t) {
		TSNodeLabel result = undoMarkoBinarizationRight(t);
		return undoParentAnnotation(result);
	}

	
	private TSNodeLabel performMarkoBinarizationRight(TSNodeLabel t) {
		
		int prole = t.prole();
		
		if (prole==0 || (prole==1 && t.isPreLexical())) {
			return t.clone();
		}
		
		TSNodeLabel binaryCopy = new TSNodeLabel(t.label, t.isLexical);
		
		BoundedLinkedList<String> lastSiblings = new BoundedLinkedList<String>(markH);
		String parentLabelString = t.label();
		TSNodeLabel previousLevel = binaryCopy;
		for(int i=0; i<prole; i++) {
			TSNodeLabel d = t.daughters[i];
			lastSiblings.add(d.label());			 			
			TSNodeLabel left = performMarkoBinarizationRight(d);
			Label rightLabel = TreeMarkoBinarization.buildArtificialLabel(parentLabelString, lastSiblings);
			TSNodeLabel right = new TSNodeLabel(rightLabel, t.isLexical);
			previousLevel.daughters = new TSNodeLabel[]{left,right};
			left.parent = previousLevel;
			right.parent = previousLevel;
			previousLevel = right;
		}
		TSNodeLabel epsilonDaughter = new TSNodeLabel(epsilonLabel, true);
		previousLevel.daughters = new TSNodeLabel[]{epsilonDaughter};
		epsilonDaughter.parent = previousLevel;
		return binaryCopy;
	}

	
	private TSNodeLabel undoMarkoBinarizationRight(TSNodeLabel t) {
		
		int prole = t.prole();
		
		if (prole==0) { // only for fragments
			return t.clone();
		}
		
		if (prole==1) {
			if (t.firstDaughter().label.equals(epsilonLabel))
				return null;
			return t.clone();			
		}
			
		TSNodeLabel unbinaryCopy = new TSNodeLabel(t.label, t.isLexical);
		
		ArrayList<TSNodeLabel> newDaughters = new ArrayList<TSNodeLabel>();
		TSNodeLabel currentLevel = t;
		do {
			TSNodeLabel firstDaughter = currentLevel.firstDaughter();
			TSNodeLabel firstDaughterUmbinarized = undoMarkoBinarizationRight(firstDaughter);
			newDaughters.add(firstDaughterUmbinarized);
			TSNodeLabel secondDaughter = currentLevel.secondDaughter();
			TSNodeLabel secondDaughterUmbinarized = undoMarkoBinarizationRight(secondDaughter);
			if (secondDaughterUmbinarized==null)
				break;			
			currentLevel = currentLevel.secondDaughter();
		} while(isArtificialNode(currentLevel));		
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
		return "TreeMarkoBinarizationRight_LC_nullary";
	}


	public static void main(String[] args) throws Exception {
		
		markH=0; 
		markV=1;	
		TSNodeLabel t1 = new TSNodeLabel("(TOP (S (NP (NNS Terms) (NP@ _)) (S@ (VP (AUX were) (VP@ (RB n't) (VP@ (VP (VBN disclosed) (VP@ _)) (VP@ _))))) (S@ (. .) (S@ _))) (TOP@ _))");
		TSNodeLabel t = new TSNodeLabel("(TOP (S (NP (NNS Terms)) (VP (VBD were) (RB n't) (VP (VBN disclosed))) (. .)))");
		//TSNodeLabel t = new TSNodeLabel("(A B C D E)");
		//TSNodeLabel t = new TSNodeLabel("(TOP (S (NP (PRP I)) (S@_NP (VP (VBP love) (VP@_VBP (NP (NP (PRP UNK-LC)) (NP@_NP (NP (DT both)))))) (S@_VP (. .)))))");
		//for(TSNodeLabel d : t.daughters) d.isLexical = false;
		System.out.println(t.toStringQtree());
		
		TreeMarkoBinarizationRight_LC_nullary tmb = new TreeMarkoBinarizationRight_LC_nullary();
		TSNodeLabel markoBinarizedTree = tmb.performMarkovBinarization(t);
		System.out.println(markoBinarizedTree.toStringQtree());
		System.out.println(t1.toStringQtree());
		System.out.println(markoBinarizedTree.checkParentDaughtersConsistency());
		System.out.println(markoBinarizedTree.checkOnlyAndAllTerminalsAreLexical());
		TSNodeLabel original = tmb.undoMarkovBinarization(markoBinarizedTree);
		System.out.println(original.toStringQtree());
		System.out.println(original.equals(t));
		System.out.println(original.checkParentDaughtersConsistency());
		System.out.println(original.checkOnlyAndAllTerminalsAreLexical());
		
	}
	
}
