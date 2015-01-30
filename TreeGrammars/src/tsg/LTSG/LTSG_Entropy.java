package tsg.LTSG;

import java.util.*;

import settings.Parameters;
import tsg.TSNode;
import tsg.corpora.ConstCorpus;
import tsg.parser.Parser;
import util.*;
import util.file.FileUtil;


public class LTSG_Entropy extends LTSG{
		
	public static final String Current = "Current";
	public static final String Random = "Random";
	public static final String FirstLeft = "FirstLeft";
	public static final String FirstRight = "FirstRight";
	
	public static final String RandomChange = "RandomChange";
	public static final String BiggestChange = "BiggestChange";
	public static final String SmallestChange = "SmallestChange";
	public static final String[] OrderOfChangeTypes = new String[]{RandomChange, BiggestChange, SmallestChange};
	
	public LTSG_Entropy() {
		super();
	}
	
	public void assignStartingHeads() {		
		FileUtil.appendReturn("Assigning Starting Heads: " + Parameters.startingHeads, Parameters.logFile);
		if (Parameters.startingHeads.equals(Current)) {
			Parameters.trainingCorpus.correctHeadAnnotation();
			return;
		}
		Parameters.trainingCorpus.removeHeadAnnotations();
		if (Parameters.startingHeads.equals(Random)) Parameters.trainingCorpus.assignRandomHeads();
		else if (Parameters.startingHeads.equals(FirstLeft)) Parameters.trainingCorpus.assignFirstLeftHeads();
		else if (Parameters.startingHeads.equals(FirstRight)) Parameters.trainingCorpus.assignFirstRightHeads();
	}
	
	public void hillClimbing() {		
		this.assignStartingHeads();
		ConstCorpus originalTrainingCorpus = null;
		if (Parameters.posTagConversion) {
			originalTrainingCorpus = Parameters.trainingCorpus.deepClone();
			Parameters.trainingCorpus.makePosTagsLexicon();
		}
		this.readTreesFromCorpus(Parameters.spineConversion);
		double entropy = this.entropy();
		String log = "Hill Climbing procedure" + "\n\tStart Entropy:\t" + entropy;
		FileUtil.appendReturn(log, Parameters.logFile);		
		boolean increasing;
		int cycle = 0;
		int maxCycles = Parameters.maxEntropyCycles;
		do {			
			maxCycles--;
			increasing = false;
			double total_delta = 0;
			for(TSNode tree : Parameters.trainingCorpus.treeBank) {
				double delta_entropy = hillClimbing(tree);								
				if (delta_entropy>0) {
					total_delta += delta_entropy;
					entropy -= delta_entropy;					
					increasing = true;					
				}			
			}				
			log = "\tHC cycle: " + ++cycle + "\tEntropy: " + entropy;
			log += "\tDelta Entropy: " + total_delta;
			FileUtil.appendReturn(log, Parameters.logFile);
		} while (increasing && maxCycles!=0);		
		log = "Count of trees after hillclimbing. # Trees: " + template_freq.size();
		FileUtil.appendReturn(log, Parameters.logFile);
		if (Parameters.posTagConversion) {
			Parameters.trainingCorpus.unMakePosTagsLexicon(originalTrainingCorpus);
		}
	}
	
	
	/**
	 * Performs the hillclimbing algorithm to redistribute the head dependencies
	 * in the current TreeNode in order to reduce the entropy of the entire grammar.
	 * @param template_freq the Hashtable<String,Integer> containing the frequencies of 
	 * the lexical templates in the treebank
	 * @param lexicon_freq the Hashtable<String,Integer> containing the frequencies of 
	 * the lexical items in the treebank
	 * @param changes number of maximum changes in the current treenode
	 * @param order 0:random, 1:biggest change first, 2: smallest change first
	 * @param entropy_delta_threshold 
	 * @return
	 */	
	private double hillClimbing(TSNode tree) {
		List<TSNode> internals = tree.collectNodes(true, false, false, false);
		double delta_entropy = 0;
		boolean increase = true;
		int changes = Parameters.maxNumberOfChanges;
		while(changes!=0 && increase) {
			increase = false;
			if (Parameters.orderOfChange==0) Collections.shuffle(internals);
			double bestDelta_value = (Parameters.orderOfChange==1) ? 0. : Double.MAX_VALUE;
			TSNode old_HeadNode = null, new_HeadNode = null;
			String[] decreasing_trees = null; //original_head_tree, original_dependent_tree
			String[] increasing_trees = null;	// new_head_tree, new_dependent_tree		
			for(TSNode node : internals) {
				TSNode[] head_choices = node.daughters;
				int prole = head_choices.length;
				TSNode originalMarkedDaughter = node.getHeadDaughter();
				while(node.headMarked && node.parent!=null) node = node.parent;				
				TSNode original_head_tree = node.lexicalizedTreeCopy();
				original_head_tree.applyAllConversions();				
				TSNode new_dependent_tree = originalMarkedDaughter.lexicalizedTreeCopy();
				new_dependent_tree.applyAllConversions();
				String original_head_lexicon = original_head_tree.getAnchor().label;
				int[] daughter_index = null;
				if (Parameters.orderOfChange==0) daughter_index = Utility.permutation(prole);
				for(int d=0; d<prole; d++) {
					TSNode D = (Parameters.orderOfChange==0) ? head_choices[daughter_index[d]] : head_choices[d];
					if (D.headMarked) continue;
					originalMarkedDaughter.headMarked = false;
					D.headMarked = true;
					TSNode original_dependent_tree = D.lexicalizedTreeCopy();
					original_dependent_tree.applyAllConversions();	
					TSNode new_head_tree = node.lexicalizedTreeCopy();
					new_head_tree.applyAllConversions();	
					String new_head_lexicon = new_head_tree.getAnchor().label;
					originalMarkedDaughter.headMarked = true;
					D.headMarked = false;
					Hashtable<String,Integer> temp_ETrees_freq = new Hashtable<String,Integer>();
					Hashtable<String,Integer> temp_lex_freq = new Hashtable<String,Integer>();
					String original_head_tree_String = original_head_tree.toString(false, true);
					String original_dependent_tree_String = original_dependent_tree.toString(false, true);
					String new_head_tree_String = new_head_tree.toString(false, true);
					String new_dependent_tree_String = new_dependent_tree.toString(false, true);
					String trees[] = new String[] {original_head_tree_String, original_dependent_tree_String, 
							new_head_tree_String,new_dependent_tree_String};
					String lex[] = new String[] {original_head_lexicon, new_head_lexicon};
					Utility.putStringIntegerFromToTable(trees, template_freq, temp_ETrees_freq);					
					Utility.putStringIntegerFromToTable(lex, lexicon_freq, temp_lex_freq);
					double H0 = LTSG.entropy(temp_ETrees_freq, temp_lex_freq);					
					Utility.decreaseStringInteger(temp_ETrees_freq, original_head_tree_String, 1);
					Utility.decreaseStringInteger(temp_ETrees_freq, original_dependent_tree_String, 1);
					Utility.increaseStringInteger(temp_ETrees_freq, new_head_tree_String, 1);
					Utility.increaseStringInteger(temp_ETrees_freq, new_dependent_tree_String, 1);					
					double H1 = LTSG.entropy(temp_ETrees_freq, temp_lex_freq);
					double delta = H0-H1;
					if (delta > Parameters.entropy_delta_threshold) {
						increase = true;
						boolean foundBestDelta = true;
						if (Parameters.orderOfChange!=0) 
							foundBestDelta = (Parameters.orderOfChange==1) ? 
									delta > bestDelta_value : delta < bestDelta_value;
						if(foundBestDelta) {
							decreasing_trees = new String[] {original_head_tree_String, original_dependent_tree_String};
							increasing_trees = new String[] {new_head_tree_String, new_dependent_tree_String};		
							bestDelta_value = delta;
							old_HeadNode = originalMarkedDaughter;
							new_HeadNode = D;
							if (Parameters.orderOfChange==0) break;
						}
					}
				}
				if (increase && Parameters.orderOfChange==0) break;
			}
			if (increase) {
				delta_entropy += bestDelta_value;
				changes--;
				old_HeadNode.headMarked = false;
				new_HeadNode.headMarked = true;
				Utility.increaseStringInteger(template_freq, increasing_trees[0], 1);
				Utility.increaseStringInteger(template_freq, increasing_trees[1], 1);				
				Utility.decreaseStringInteger(template_freq, decreasing_trees[0], 1);
				Utility.decreaseStringInteger(template_freq, decreasing_trees[1], 1);				
			}			
		}
		return delta_entropy;
	}
	
	
	public static void main(String args[]) {
		Parameters.setDefaultParam();		
		
		Parameters.LTSGtype = "Entropy";
		Parameters.outputPath = "/home/fsangati/PROJECTS/TSG/RESULTS/LTSG/Entropy/";
		
		Parameters.startingHeads = Random; //Random Current FirstLeft FirstRight
		Parameters.maxNumberOfChanges = -1; //-1 = no limits  
		Parameters.orderOfChange = 2; //0:random, 1:biggest change first, 2: smallest change first
		Parameters.maxEntropyCycles = -1; //-1 = no limits  
		Parameters.entropy_delta_threshold = 0.01;

		Parameters.spineConversion = false;
		Parameters.posTagConversion = false;
		
		LTSG_Entropy Grammar = new LTSG_Entropy();
		Grammar.hillClimbing();		
		Grammar.readTreesFromCorpus();
		//java.io.File templateFile = new java.io.File("/home/fsangati/PROJECTS/TSG/RESULTS/LTSG/Entropy/Tue_Jun_24_17_19_54/TemplatesFile");
		//Grammar.readTreesFromFile(templateFile);		
		
		Grammar.printTemplatesToFile();
		Grammar.treatTreeBank();
		Grammar.toPCFG();
		
		Grammar.printTrainingCorpusToFile();		
		Grammar.printLexiconAndGrammarFiles();				
		
		new Parser(Grammar);
	}
	
}
