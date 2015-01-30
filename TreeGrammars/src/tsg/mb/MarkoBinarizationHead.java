package tsg.mb;

import java.util.ArrayList;

import tsg.Label;
import tsg.TSNodeLabel;
import util.BoundedLinkedList;

public class MarkoBinarizationHead extends TreeMarkoBinarization {
	
	public TSNodeLabel performMarkovBinarization(TSNodeLabel t) {
		TSNodeLabel result = performParentAnnotation(t);
		return performMarkoBinarizationHead(result);
	}
	
	public TSNodeLabel undoMarkovBinarization(TSNodeLabel t) {
		TSNodeLabel result = undoMarkoBinarizationHead(t);
		return undoParentAnnotation(result);
	}

	
	private TSNodeLabel performMarkoBinarizationHead(TSNodeLabel t) {
		int prole = t.prole();
		TSNodeLabel binaryCopy = new TSNodeLabel(t.label, t.isLexical);
		if (prole==0) return binaryCopy;
		if (prole==1) {
			TSNodeLabel onlyDaughter = performMarkoBinarizationHead(t.firstDaughter());
			binaryCopy.daughters = new TSNodeLabel[]{onlyDaughter};
			onlyDaughter.parent = binaryCopy;
			return binaryCopy;
		}
		
		BoundedLinkedList<String> lastSiblings = new BoundedLinkedList<String>(markH);
		String parentLabelString = t.label() + parentLabelSeparator;
		TSNodeLabel previousLevel = binaryCopy;
		for(int i=0; i<prole-1; i++) {
			TSNodeLabel d = t.daughters[i];
			lastSiblings.add(d.label());			 			
			TSNodeLabel left = performMarkoBinarizationHead(d);
			StringBuilder rightLabelSB = new StringBuilder(parentLabelString);
			for(String s : lastSiblings.getList()) {
				rightLabelSB.append(catSeparationH);
				rightLabelSB.append(s);
			}
			Label rightLabel = Label.getLabel(rightLabelSB.toString());
			TSNodeLabel right = new TSNodeLabel(rightLabel, t.isLexical);
			previousLevel.daughters = new TSNodeLabel[]{left,right};
			left.parent = previousLevel;
			right.parent = previousLevel;
			previousLevel = right;
		}
		
		TSNodeLabel onlyDaughter = performMarkoBinarizationHead(t.daughters[prole-1]);
		previousLevel.daughters = new TSNodeLabel[]{onlyDaughter};
		onlyDaughter.parent = previousLevel;
		
		return binaryCopy;
	}

	
	private TSNodeLabel undoMarkoBinarizationHead(TSNodeLabel t) {
		int prole = t.prole();
		TSNodeLabel unbinaryCopy = new TSNodeLabel(t.label, t.isLexical);
		if (prole==0) return unbinaryCopy;
		if (prole==1) {
			TSNodeLabel onlyDaughter = undoMarkoBinarizationHead(t.firstDaughter());
			unbinaryCopy.daughters = new TSNodeLabel[]{onlyDaughter};			
			onlyDaughter.parent = unbinaryCopy;
			return unbinaryCopy;
		}
		
		ArrayList<TSNodeLabel> newDaughters = new ArrayList<TSNodeLabel>();
		TSNodeLabel currentLevel = t;
		do {
			newDaughters.add(undoMarkoBinarizationHead(currentLevel.firstDaughter()));
			currentLevel = currentLevel.lastDaughter();
		} while(currentLevel.label().indexOf(parentLabelSeparator)!=-1);		
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
		return "MarkoBinarizationHead";
	}


	public static void main(String[] args) throws Exception {
		
		markH=1; 
		markV=3;	
		TSNodeLabel t = new TSNodeLabel("(A (B b) (C (F f) (G g) (H h)) (D (I i)) (E (J (K k) (L l))))");		
		//TSNodeLabel t = new TSNodeLabel("(A B C D E)");
		System.out.println(t);
		MarkoBinarizationHead tmb = new MarkoBinarizationHead();
		TSNodeLabel markoBinarizedTree = tmb.performMarkovBinarization(t);
		System.out.println(markoBinarizedTree);
		System.out.println(markoBinarizedTree.checkParentDaughtersConsistency());
		System.out.println(markoBinarizedTree.checkOnlyAndAllTerminalsAreLexical());
		TSNodeLabel original = tmb.undoMarkovBinarization(markoBinarizedTree);
		System.out.println(original);
		System.out.println(original.equals(t));
		System.out.println(original.checkParentDaughtersConsistency());
		System.out.println(original.checkOnlyAndAllTerminalsAreLexical());
		
	}
	
}
