package tsg.parser;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.*;

import util.*;
import util.file.FileUtil;

/**
 * This class contains a single cell which will be placed in a specific
 * position of a cky chart table.
 * @author fsangati
 *
 */
public class Cell implements Serializable {
	private static final long serialVersionUID = 0L;
	static Grammar grammar;
	Hashtable<Integer, DerivationForest> DerivationForestSet; 
	TreeSet<Integer> possibleRoots; //ordered array with possible root indexes
	ArrayList<Integer> yield;
	
	
	public Cell() {
		DerivationForestSet = new Hashtable<Integer, DerivationForest>();
		possibleRoots = new TreeSet<Integer>();
		this.yield = new ArrayList<Integer>();
	}
	
	public Cell(ArrayList<Integer> yield) {		
		DerivationForestSet = new Hashtable<Integer, DerivationForest>();
		possibleRoots = new TreeSet<Integer>();
		this.yield = yield;
	}

	
	/**
	 * Constructor for single production cells i.e. y->w where w is a terminal.
	 * @param word
	 */
	public Cell(Integer word) {
		this();
		this.yield.add(word);
		int[] possibleRules = grammar.rulesOfWord[word]; 
		for(int i=0; i<possibleRules.length; i++) {
			int lexRuleIndex = possibleRules[i];
			int root = grammar.lexRules[lexRuleIndex][0];
			possibleRoots.add(root);
			DerivationForest forest = new DerivationForest(root, grammar.lexLogProb[lexRuleIndex]);			
			DerivationForestSet.put(root, forest);			
		}
	}
	
	public String getYieldInWord() {
		String result = "";
		for(ListIterator<Integer> i=this.yield.listIterator(); i.hasNext();) {
			String word = grammar.lexArray[i.next()];
			result += word + " ";
		}
		return result.trim();
	}
		
	/**
	 * Update a cell with all the rules that can be constructed with Left productions
	 * belonging to the input leftCell and Right productions belonging to the input upCell.
	 * Memory hook: leftCell is the cell at the left of the current cell at position `split` (from left)
	 * while upCell is the cell above the current cell at position `split` (from bottom).
	 * @param leftCell
	 * @param upCell
	 * @param split
	 */
	public void updateBinary(Cell leftCell, Cell upCell, int split) {
			for(Iterator<Integer> j = leftCell.possibleRoots.iterator(); j.hasNext(); ) {
				int leftRoot = j.next();
				int[] applicableRules = grammar.binaryRulesOfLeftChild[leftRoot];
				if (applicableRules==null) continue;
				for(int k=0; k<applicableRules.length; k++) {
					int ruleIndex = applicableRules[k];
					int[] rule = grammar.intBinaryRules[ruleIndex];
					int upRoot = rule[2];
					int parentRoot = rule[0];
					if (upCell.possibleRoots.contains(upRoot)) {
						double ruleLogProb = grammar.intBinaryLogProb[ruleIndex];
						boolean newEntry = this.possibleRoots.add(parentRoot);
						DerivationForest leftForest = leftCell.DerivationForestSet.get(leftRoot);
						DerivationForest upForest =  upCell.DerivationForestSet.get(upRoot);					
						if (newEntry) {
							DerivationForest newForest = new DerivationForest(parentRoot, leftForest,
									upForest, split, ruleLogProb);
							DerivationForestSet.put(parentRoot, newForest); 
						}
						else {
							DerivationForest updatingForest = this.DerivationForestSet.get(parentRoot);
							updatingForest.updateForest(leftForest, upForest, split, ruleLogProb);
						}
					}
				}
			}			
	}
	
	public void updateUnary() {
		TreeSet<Integer> initialRoots = new TreeSet<Integer>(this.possibleRoots);
		for(Iterator<Integer> j = initialRoots.iterator(); j.hasNext(); ) {
			Integer root = j.next();
			updateUnary(root);
		}
	}
		
	
	public void updateUnary(Integer root) {		
		int[] applicableRules = grammar.unaryRulesOfLeftChild[root];
		if (applicableRules==null) return;
		//TreeSet<Integer> toBeCountinued = new TreeSet<Integer>(); 
		DerivationForest rootForest = this.DerivationForestSet.get(root);
		for(int k=0; k<applicableRules.length; k++) {
			int ruleIndex = applicableRules[k];
			int[] rule = grammar.intUnaryRules[ruleIndex];
			double ruleLogProb = grammar.intUnaryLogProb[ruleIndex];
			int parentRoot = rule[0];
			if (rule[0]==rule[1]) continue;
			updateUnary(parentRoot, rootForest, ruleLogProb);
			//if (updateUnary(parentRoot, rootForest, ruleLogProb)) toBeCountinued.add(parentRoot);
		}
		//for(Iterator<Integer> i = toBeCountinued.iterator(); i.hasNext();) {
		//	updateUnary(i.next());
		//}
	}
	
	public boolean updateUnary(int parentRoot, DerivationForest rootForest, double ruleLogProb) {
		boolean newEntry = this.possibleRoots.add(parentRoot);
		if (newEntry) {
			DerivationForest newForest = new DerivationForest(parentRoot, rootForest, ruleLogProb);
			DerivationForestSet.put(parentRoot, newForest);
			return true;
		}
		else {
			DerivationForest updatingForest = this.DerivationForestSet.get(parentRoot);
			return updatingForest.updateForest(rootForest, ruleLogProb);
		}
	}

	
	public void updateUnaryOld() {		
		TreeSet<Integer> parentRoots = new TreeSet<Integer>();
		TreeSet<Integer> uniqueChildren = new TreeSet<Integer>();
		TreeSet<Integer> ruleIndexes = new TreeSet<Integer>();
		for(Iterator<Integer> j = this.possibleRoots.iterator(); j.hasNext(); ) {
			int uniqueChild = j.next();
			int[] applicableRules = grammar.unaryRulesOfLeftChild[uniqueChild];
			if (applicableRules==null) continue;						
			for(int k=0; k<applicableRules.length; k++) {
				int ruleIndex = applicableRules[k];
				int[] rule = grammar.intUnaryRules[ruleIndex];
				int parentRoot = rule[0];
				if (parentRoot==uniqueChild) continue;
				ruleIndexes.add(ruleIndex);
				parentRoots.add(parentRoot);
				uniqueChildren.add(uniqueChild);
			}
		}		
		TreeSet<Integer> parentRootsProblems = new TreeSet<Integer>(parentRoots); 
		TreeSet<Integer> parentRootsNoProblems = new TreeSet<Integer>(parentRoots);
		parentRootsProblems.retainAll(this.possibleRoots);
		parentRootsNoProblems.removeAll(parentRootsProblems);
		if(!parentRootsProblems.isEmpty()) {
			System.out.print("");
		}		
		 
		for(int round=0; round<2; round++) {
			for(Iterator<Integer> j = uniqueChildren.iterator(); j.hasNext(); ) {
				int uniqueChild = j.next();
				int[] applicableRules = grammar.unaryRulesOfLeftChild[uniqueChild];
				if (applicableRules==null) continue;						
				for(int k=0; k<applicableRules.length; k++) {
					int ruleIndex = applicableRules[k];
					int[] rule = grammar.intUnaryRules[ruleIndex];
					int parentRoot = rule[0];
					if (parentRoot==uniqueChild) continue; //recursive unary production i.e. NP -> NP
					if (!parentRootsProblems.contains(parentRoot) && round==0) continue;
					if (parentRootsProblems.contains(parentRoot) && round==1) continue;					
					double ruleLogProb = grammar.intUnaryLogProb[ruleIndex];
					boolean newEntry = this.possibleRoots.add(parentRoot);
					DerivationForest uniqueChildForest = this.DerivationForestSet.get(uniqueChild);
					if (newEntry) {
						DerivationForest newForest = new DerivationForest(parentRoot, uniqueChildForest, ruleLogProb);
						DerivationForestSet.put(parentRoot, newForest);
					}
					else {
						DerivationForest updatingForest = this.DerivationForestSet.get(parentRoot);
						updatingForest.updateForest(uniqueChildForest, ruleLogProb);
					}
				}			
			}
		}
	}
	
	/*public void checkDerivationConsistency() {
		for(Enumeration<DerivationForest> e = this.DerivationForestSet.elements(); e.hasMoreElements();) {
			DerivationForest forest = e.nextElement();
			forest.checkDerivationConsistency();
		}
	}*/

	
	/**
	 * Read a cell object from file
	 * @param inputFile
	 * @return
	 */
	public static Cell fromBinaryFile(File inputFile) {
		try {
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(inputFile));			
			return (Cell) in.readObject();
		} catch (Exception e) {FileUtil.handleExceptions(e);}
		return null;
	}
	
	/**
	 * Write this cell object to file
	 * @param outputFile
	 */
	public void toBinaryFile(File outputFile) {
		try{
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(outputFile));
			out.writeObject(this);
		} catch (Exception e) {FileUtil.handleExceptions(e);}		
	}

	
}
