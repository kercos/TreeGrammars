package tsg.mb;

import java.io.File;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.ArrayList;

import edu.berkeley.nlp.PCFGLA.Binarization;
import tsg.Label;
import tsg.TSNodeLabel;
import tsg.utils.CleanPetrov;
import util.BoundedLinkedList;
import util.file.FileUtil;
import wordModel.UkWordMapping;
import wordModel.UkWordMappingPetrov;

public class TreeMarkoBinarizationLeft_Petrov extends TreeMarkoBinarization {
	
	public TSNodeLabel performMarkovBinarization(TSNodeLabel t) {
		TSNodeLabel result = performParentAnnotation(t);
		return performMarkoBinarizationLeft(result);
	}
	
	public TSNodeLabel undoMarkovBinarization(TSNodeLabel t) {
		TSNodeLabel result = undoMarkoBinarizationLeft(t);
		return undoParentAnnotation(result);
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


	public static void main1(String[] args) throws Exception {
		markH=1; 
		markV=2;
		TreeMarkoBinarizationLeft_Petrov tmb = new TreeMarkoBinarizationLeft_Petrov();
		TSNodeLabel tree = new TSNodeLabel("(VP|S VBD|VP (NP|VP (NP|NP DT|NP NN|NP) (PP|NP (IN|PP \"with\") NP|PP)))", false);		
		
		System.out.println(tree.toStringQtree());
		//TSNodeLabel mbTree = tmb.performMarkovBinarization(tree);
		TSNodeLabel clean = tmb.undoMarkovBinarization(tree);
		
		System.out.println(clean.toStringQtree());
	}
	
	public static void main3(String[] args) throws Exception {
		
		markH=1; 
		markV=2;	

		//for(int i=0; i<10000; i++) {
			TSNodeLabel t = TSNodeLabel.getRandomTree(new String[]{"A","B","C","D","E","F","G"}, 
				new String[]{"a","b","c","d","e","f","g"}, 5, new double[]{.2,.2,.2,.2,.2}, 
				true, .2, 2, 5);
			//TSNodeLabel t = new TSNodeLabel("(E (C (A a ) ) (E (A c ) (F c ) (D g ) ) (G (D f ) (D a ) (C c ) (G e ) ) (C (B f ) ) )");

		
					
			//TSNodeLabel t = new TSNodeLabel("(E (D (C f) (B c) (C c) (E b)) (C (E d) (A e) (D d) (F a)))");			
			//for(TSNodeLabel d : t.daughters) d.isLexical = false;
			System.out.println(t.toStringQtree());
			TreeMarkoBinarizationLeft_Petrov tmb = new TreeMarkoBinarizationLeft_Petrov();
			TSNodeLabel markoBinarizedTree = tmb.performMarkovBinarization(t);
			//System.out.println(markoBinarizedTree.toString());
			System.out.println(markoBinarizedTree.toStringQtree());
			//System.out.println(markoBinarizedTree.checkParentDaughtersConsistency());
			//System.out.println(markoBinarizedTree.checkOnlyAndAllTerminalsAreLexical());
			//System.out.println(markoBinarizedTree.maxBranching());
			//TSNodeLabel original = tmb.undoMarkovBinarization(markoBinarizedTree);
			//System.out.println(original.toStringQtree());
			//System.out.println(original.equals(t));
			//System.out.println(original.checkParentDaughtersConsistency());
			//System.out.println(original.checkOnlyAndAllTerminalsAreLexical());
			BigInteger fragmnetsOriginal = t.countTotalFragments()[0];
			BigInteger fragmnetsBinarized = markoBinarizedTree.countTotalFragments()[0];
			System.out.println(fragmnetsOriginal);
			System.out.println(fragmnetsBinarized);
			//if (!original.equals(t)) {
			//	System.out.println(t);
			//	break;
			//}
		//}
	}
	
	private static void transformTreebank(ArrayList<TSNodeLabel> treebank, File ouputFile) {
		PrintWriter pw = FileUtil.getPrintWriter(ouputFile);		
		
		TreeMarkoBinarization treeMarkovBinarizer = new TreeMarkoBinarizationLeft_Petrov();		
		
		for(TSNodeLabel t : treebank) {
			TSNodeLabel transformed = treeMarkovBinarizer.performMarkovBinarization(t);
			pw.println(transformed.toString());
		}
				
		pw.close();
	}
	
	@Override
	public String getDescription() {
		return "TreeMarkoBinarizationLeft_Petrov";
	}

	
	public static void main(String[] args) throws Exception {
		markH=1; 
		markV=2;
		TreeMarkoBinarizationLeft_Petrov tmb = new TreeMarkoBinarizationLeft_Petrov();
		TSNodeLabel t = new TSNodeLabel("(A B C D E)");
		TSNodeLabel markoBinarizedTree = tmb.performMarkovBinarization(t);
		System.out.println(markoBinarizedTree.toStringQtree());
		//TSNodeLabel tree = new TSNodeLabel("(TOP (S (NP (NNP Ms.) (NNP Haag)) (VP (VBZ plays) (NP (NNP Elianti))) (. .)))");
		//tree.makeTreeCharBased();
		//TSNodeLabel tree = new TSNodeLabel("(TOP (S (ADVP (RB Meanwhile)) (, ,) (NP (NNP September) (NN housing) (NNS starts) (, ,) (NP (JJ due) (NNP Wednesday)) (, ,)) (VP (VBP are) (VP (VBN thought) (S (VP (TO to) (VP (VB have) (VP (VBD inched) (ADVP (RB upward)))))))) (. .)))");		
		//System.out.println(tree.toStringQtree());
		//TSNodeLabel mbTree = tmb.performMarkovBinarization(tree);
		//System.out.println(mbTree.toStringQtree());
		//System.out.println(mbTree);
		//TSNodeLabel clean = tmb.undoMarkovBinarization(mbTree);		
		//System.out.println(clean.equals(tree));
	}
	
	public static void main2(String[] args) throws Exception {				
		
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
