package tsg.mb;

import java.io.File;
import java.util.ArrayList;

import tsg.Label;
import tsg.TSNodeLabel;
import util.BoundedLinkedList;

public class TreeMarkoBinarizationRight_LC_LeftCorner extends TreeMarkoBinarization {

	public static final boolean emptyNodes = false;

	public static boolean avoidUnaryInBinarization = false;
	public static final String leftCornerSeparator = ":";
	public static final String unarySeparator = "/";

	public TSNodeLabel performMarkovBinarization(TSNodeLabel t) {
		TSNodeLabel result = performParentAnnotation(t);
		//System.out.println("After PA: " + result.toStringTex(false, false));
		result.compressUnaryProductions(unarySeparator, false);
		//System.out.println("After UC: " + result.toStringTex(false, false));
		result = performLeftCornerTransform(result);
		//System.out.println("After LC: " + result.toStringTex(false, false));
		if (!t.collectLexicalLabels().equals(result.collectLexicalLabels())) {
			System.err.println("Bad Left corner transform: ");
			System.err.println("\t" + t.toStringTex(false, false));
			System.err.println("\t" + result.toStringTex(false, false));
			return null;
		}
		result = avoidUnaryInBinarization ?
				performMarkoBinarizationRightNoUnary(result) :
				performMarkoBinarizationRight(result);
		//System.out.println("After MB: " + result.toStringTex(false, false));
		return result;
	}

	public TSNodeLabel undoMarkovBinarization(TSNodeLabel t) {
		TSNodeLabel result = avoidUnaryInBinarization ?
				undoMarkoBinarizationRightNoUnary(t) :
				undoMarkoBinarizationRight(t);
		//System.out.println("After uMB: " + result.toStringTex(false, false));
		result = undoLeftCornerTranform(result);
		//System.out.println("After uLC: " + result.toStringTex(false, false));
		result.uncompressUnaryProductions(unarySeparator);
		//System.out.println("After uUC: " + result.toStringTex(false, false));
		result =  undoParentAnnotation(result);
		//System.out.println("After uPA: " + result.toStringTex(false, false));
		return result;
	}

	private static TSNodeLabel performLeftCornerTransform(TSNodeLabel tree) {
		if (tree.isPreLexical())
			return tree.clone();

		if (tree.isPreLexical())
			return tree.clone();

		TSNodeLabel leftCornerLex = tree.getLeftmostLexicalNode();
		TSNodeLabel leftCornerPos = leftCornerLex.parent;

		TSNodeLabel root = new TSNodeLabel(tree.label, false);
		Label rootLCLabel = Label.getLabel(tree.label() + leftCornerSeparator + leftCornerPos.label());
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
		boolean reachedTop = higherLeftCorner == tree;
		boolean oneLessDaughters = reachedTop && !emptyNodes;
		TSNodeLabel[] daughters = higherLeftCorner.daughters;
		int prole = daughters.length;
		int newProle = oneLessDaughters ? prole - 1 : prole;
		TSNodeLabel[] newDaughters = new TSNodeLabel[newProle];
		for (int i = 1; i < prole; i++) {
			TSNodeLabel d = daughters[i];
			newDaughters[i - 1] = performLeftCornerTransform(d);
		}
		if (!oneLessDaughters) {
			Label lastNewDaughterLabel = reachedTop ? Label.getLabel(tree.label() + leftCornerSeparator + tree.label()) : Label.getLabel(tree.label() + leftCornerSeparator + higherLeftCorner.label());
			TSNodeLabel newRootLCnode = new TSNodeLabel(lastNewDaughterLabel, false);
			newDaughters[newProle - 1] = newRootLCnode;
			if (!reachedTop)
				convertToLeftCornerSpine(tree, higherLeftCorner, newRootLCnode);
		}
		if (newProle != 0)
			rootLCnode.assignDaughters(newDaughters);
	}

	private static TSNodeLabel undoLeftCornerTranform(TSNodeLabel tree) {
		if (tree.isPreLexical())
			return tree.clone();

		String goalLabelSep = tree.label() + leftCornerSeparator;
		TSNodeLabel higherLc = tree.daughters[1];
		while (!higherLc.isTerminal()) {
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
		for (int i = 0; i < prole - 1; i++) {
			newDaughters[i + 1] = undoLeftCornerTranform(daughters[i]);
		}
		newDaughters[0] = convertFromLCspine(tree, higherLc);

		result.assignDaughters(newDaughters);
		return result;
	}

	private static TSNodeLabel convertFromLCspine(TSNodeLabel goal, TSNodeLabel leftCornerNode) {

		TSNodeLabel p = leftCornerNode.parent;
		if (p == goal)
			return goal.firstDaughter().clone();

		String higherLClabel = leftCornerNode.label();
		Label baseLabel = Label.getLabel(higherLClabel.substring(higherLClabel.indexOf(leftCornerSeparator) + leftCornerSeparator.length()));
		TSNodeLabel result = new TSNodeLabel(baseLabel, false);
		int prole = p.prole();
		TSNodeLabel[] daughters = p.daughters;
		TSNodeLabel[] newDaughters = new TSNodeLabel[prole];
		for (int i = 0; i < prole - 1; i++) {
			newDaughters[i + 1] = undoLeftCornerTranform(daughters[i]);
		}
		newDaughters[0] = convertFromLCspine(goal, p);
		result.assignDaughters(newDaughters);
		return result;
	}

	private TSNodeLabel performMarkoBinarizationRight(TSNodeLabel t) {
		int prole = t.prole();
		TSNodeLabel binaryCopy = new TSNodeLabel(t.label, t.isLexical);
		if (prole == 0)
			return binaryCopy;
		if (prole == 1) {
			TSNodeLabel onlyDaughter = performMarkoBinarizationRight(t.firstDaughter());
			binaryCopy.daughters = new TSNodeLabel[] { onlyDaughter };
			onlyDaughter.parent = binaryCopy;
			return binaryCopy;
		}

		BoundedLinkedList<String> lastSiblings = new BoundedLinkedList<String>(markH);
		String parentLabelString = t.label();
		TSNodeLabel previousLevel = binaryCopy;
		for (int i = 0; i < prole - 1; i++) {
			TSNodeLabel d = t.daughters[i];
			lastSiblings.add(d.label());
			TSNodeLabel left = performMarkoBinarizationRight(d);
			Label rightLabel = TreeMarkoBinarization.buildArtificialLabel(parentLabelString, lastSiblings);
			TSNodeLabel right = new TSNodeLabel(rightLabel, t.isLexical);
			previousLevel.daughters = new TSNodeLabel[] { left, right };
			left.parent = previousLevel;
			right.parent = previousLevel;
			previousLevel = right;
		}

		TSNodeLabel onlyDaughter = performMarkoBinarizationRight(t.daughters[prole - 1]);
		previousLevel.daughters = new TSNodeLabel[] { onlyDaughter };
		onlyDaughter.parent = previousLevel;

		return binaryCopy;
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

	private TSNodeLabel undoMarkoBinarizationRight(TSNodeLabel t) {
		int prole = t.prole();
		TSNodeLabel unbinaryCopy = new TSNodeLabel(t.label, t.isLexical);
		if (prole == 0)
			return unbinaryCopy;
		if (prole == 1) {
			TSNodeLabel onlyDaughter = undoMarkoBinarizationRight(t.firstDaughter());
			unbinaryCopy.daughters = new TSNodeLabel[] { onlyDaughter };
			onlyDaughter.parent = unbinaryCopy;
			return unbinaryCopy;
		}

		ArrayList<TSNodeLabel> newDaughters = new ArrayList<TSNodeLabel>();
		TSNodeLabel currentLevel = t;
		do {
			newDaughters.add(undoMarkoBinarizationRight(currentLevel.firstDaughter()));
			currentLevel = currentLevel.lastDaughter();
		} while (isArtificialNode(currentLevel));
		int newProle = newDaughters.size();
		unbinaryCopy.daughters = new TSNodeLabel[newProle];
		int i = 0;
		for (TSNodeLabel d : newDaughters) {
			unbinaryCopy.daughters[i++] = d;
			d.parent = unbinaryCopy;
		}
		return unbinaryCopy;
	}

	@Override
	public String getDescription() {
		return "TreeMarkoBinarizationRight_LC_LeftCorner";
	}

	public static void main(String[] args) throws Exception {
		markH = 0;
		markV = 1;
		avoidUnaryInBinarization = false;
		
		TreeMarkoBinarizationRight_LC_LeftCorner tmb = new TreeMarkoBinarizationRight_LC_LeftCorner();
				
		TSNodeLabel t = new TSNodeLabel("(S (A (C c) (D (E e) (F f))) (B (G g) (H (I i) (L l))))");
		//TSNodeLabel t = new TSNodeLabel("(S (A (C (C' c)) (D (D' (E e) (F f)))) (B (G g) (H (I i) (L l))))");
		//TSNodeLabel t = new TSNodeLabel("(S (A (C c) (D (E e) (F f)) (J j)) (B (G g) (H (I i) (L l))(K k)))");
		//TSNodeLabel t = new TSNodeLabel("(S (A (C (J j) (K k)) (D (E e) (F f)) ) (B (G g) (H (I i) (L l))))");
		//TSNodeLabel t = new TSNodeLabel("(S (PP (IN In) (NP (NP (DT an) (NNP Oct.) (CD 19) (NN review)) (PP (IN of) (NP (NP (DT The) (NN Misanthrope)) (PP (IN at) (NP (NP (NNP Chicago) (POS 's)) (NNP Goodman) (NNP Theatre))))) (PRN (-LRB- -LRB-) (S (NP (VBN Revitalized) (NNS Classics)) (VP (VBP Take) (NP (DT the) (NN Stage)) (PP (IN in) (NP (NNP Windy) (NNP City))))) (, ,) (NP (NN Leisure) (CC &) (NNS Arts)) (-RRB- -RRB-)))) (, ,) (NP (NP (NP (DT the) (NN role)) (PP (IN of) (NP (NNP Celimene)))) (, ,) (VP (VBN played) (PP (IN by) (NP (NNP Kim) (NNP Cattrall)))) (, ,)) (VP (AUX was) (VP (ADVP (RB mistakenly)) (VBN attributed) (PP (TO to) (NP (NNP Christina) (NNP Haag))))) (. .))");
		//TSNodeLabel t = new TSNodeLabel("(PP (IN of) (NP (NNP Celimene)))");
		//TSNodeLabel t = new TSNodeLabel("(S (DT The) (S@ (S:DT (NN economy) (S:DT@ (POS 's) (S:DT@ (S:NP (NN UNK-LC) (S:NP@ (S:NP (VP (MD will) (VP@ (VP:MD (VP (AUX be) (VP@ (VP:AUX (VP (VBN taken) (VP@ (VP:VBN (PP (IN from) (PP@ (PP:IN (NP (JJ several) (NP@ (NP:JJ (NN UNK-LC) (NP:JJ@ (NNS points)))))))) (VP:VBN@ (NP (DT this) (NP@ (NP:DT (NN week)))) (VP:VBN@ (, ,) (VP:VBN@ (PP (IN with) (PP@ (PP:IN (NP (NP/NNS UNK-LC-s) (NP@ (NP:NP/NNS (PP (IN on) (PP@ (PP:IN (NP (NP/NN trade) (NP@ (NP:NP/NN (, ,) (NP:NP/NN@ (NP/NN output) (NP:NP/NN@ (, ,) (NP:NP/NN@ (NP/NN housing) (NP:NP/NN@ (CC and) (NP:NP/NN@ (NP/NN inflation)))))))))))))))))))))))))))))) (S:NP@ (. .))))))))))");
		//TSNodeLabel t = new TSNodeLabel("(S (DT The) (S@ (S:DT (NN consensus) (S:DT@ (NN view) (S:DT@ (S:NP (VP (VBZ expects) (VP@ (VP:VBZ (NP (DT a) (NP@ (NP:DT (ADJP (CD 0.4) (ADJP@ (ADJP:CD (NN %)))) (NP:DT@ (NN increase) (NP:DT@ (NP:NP (PP (IN in) (PP@ (PP:IN (NP (DT the) (NP@ (NP:DT (NNP September) (NP:DT@ (NNP UNK-CAPS)))))))))))))) (VP:VBZ@ (PP (IN after) (PP@ (PP:IN (NP (DT a) (NP@ (NP:DT (JJ flat) (NP:DT@ (NN reading) (NP:DT@ (NP:NP (PP (IN in) (PP@ (PP:IN (NP/NNP August))))))))))))))))) (S:NP@ (. .))))))))");
		//TSNodeLabel t = new TSNodeLabel("(S:S (S:S@ (S (DT The) (S:DT (NN consensus) (NN view) (S:NP (VP (VBZ expects) (VP:VBZ (NP (DT a) (NP:DT (ADJP (CD 0.4) (ADJP:CD (NN %))) (NN increase) (NP:NP (PP (IN in) (PP:IN (NP (DT the) (NP:DT (NNP September) (NNP UNK-CAPS) (NP:NP (PP (IN after) (PP:IN (NP (DT a) (NP:DT (JJ flat) (NN reading) (NP:NP (PP (IN in) (PP:IN (NP/NNP August))))))))))))))))))))) (. .)))");
				
		
		TSNodeLabel tClone = t.clone();
		System.out.println(t.toStringTex(false, false));
		TSNodeLabel tlc = tmb.performMarkovBinarization(t);							
		System.out.println(tlc.toStringTex(false, false));
		TSNodeLabel btlc = tmb.undoMarkovBinarization(tlc);
		System.out.println(btlc.toStringTex(false, false));
		System.out.println(tClone.equals(btlc));		
		
		/*
		String path = "/afs/inf.ed.ac.uk/user/f/fsangati/CORPORA/WSJ/ROARK/NOTOP/";
		File inputFile = new File(path + "wsj-02-21.mrg");
		File outputFile = new File(path + "wsj-02-21_MB_LC.mrg");
		File backoutputFile = new File(path + "wsj-02-21_MB_BLC.mrg");
		tmb.markoBinarizeFile(inputFile, outputFile);
		tmb.undoMarkoBinarizeFile(outputFile, backoutputFile);
		*/
		
	}

	public static void mainM(String[] args) throws Exception {

		markH = 1;
		markV = 2;
		TreeMarkoBinarizationRight_LC_LeftCorner tmb = new TreeMarkoBinarizationRight_LC_LeftCorner();
		// TSNodeLabel t = new
		// TSNodeLabel("(A (B b) (C (F f) (G g) (H h)) (D (I i)) (E (J (K k) (L l))))");
		// TSNodeLabel t = new TSNodeLabel("(A B C D E)");
		// TSNodeLabel t = new
		// TSNodeLabel("(TOP (S (NP (NNS Terms)) (S@ (VP (VBD were) (VP (VBN disclosed))) (S@ (. .)))))");
		TSNodeLabel t = new TSNodeLabel("(TOP (NP (NNP UNK-SC-Dash-y) (NP@ (NNP Street))) (TOP@ (||STOP|| ||STOP||)))");
		// for(TSNodeLabel d : t.daughters) d.isLexical = false;

		TSNodeLabel p = tmb.undoMarkovBinarization(t);
		System.out.println(p.toString());

		/*
		 * System.out.println(t.toStringQtree());
		 * System.out.println(markoBinarizedTree.toStringQtree());
		 * System.out.println
		 * (markoBinarizedTree.checkParentDaughtersConsistency());
		 * System.out.println
		 * (markoBinarizedTree.checkOnlyAndAllTerminalsAreLexical());
		 * TSNodeLabel original =
		 * tmb.undoMarkovBinarization(markoBinarizedTree);
		 * System.out.println(original.toStringQtree());
		 * System.out.println(original.equals(t));
		 * System.out.println(original.checkParentDaughtersConsistency());
		 * System.out.println(original.checkOnlyAndAllTerminalsAreLexical());
		 */

	}

}
