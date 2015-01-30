package tsg.parser;
import java.io.Serializable;
import java.util.*;

import settings.Parameters;
import tsg.*;

/**
 * The class represents a derivation forest of a specific
 * non terminal of a specific cell of a cyk parse chart.
 * @author fsangati
 *
 */
public class DerivationForest implements Serializable{
	private static final long serialVersionUID = 0L;
	int root;
	double logInsideProb;	
	int[][] derivations;  // int[numberOfDerivations][5]: //split, leftRoot, leftIndex, rightRoot, rightIndex
	double[] derivationLogProb;
	
	/**
	 * DerivationForest of a terminal rule (a single element is present in derivations)
	 * @param root
	 * @param logProb
	 */
	public DerivationForest(int root, double logProb) {
		this.root = root;
		this.derivations = new int[][] {{-1, -1, -1, -1, -1}};
		this.logInsideProb = logProb;
		this.derivationLogProb = new double[] {logProb};
	}
	
	/**
	 * 
	 * @return true iff the number of derivations in this forest equals the nBest parameters
	 */
	public boolean isFull() {
		return (this.derivations.length == Parameters.nBest);
	}
	
	/**
	 * 
	 * @return the number of derivations in this forest
	 */
	public int size() {
		return this.derivations.length;
	}
	
	/**
	 * Create a new DerivationForest starting with a pair of terminal X,K form two cells of the
	 * current CKY table for the unary internal rule root -> uniqueChild
	 * @param root
	 * @param leftForest
	 * @param ruleLogProb
	 */
	public DerivationForest(int root, DerivationForest uniqueChildForest, double ruleLogProb) {		
		this.root = root; 
		int derivationSize = uniqueChildForest.derivations.length;
		this.derivations = new int[derivationSize][5];
		this.derivationLogProb = new double[derivationSize];
		logInsideProb = uniqueChildForest.logInsideProb + ruleLogProb;				
		for(int i=0; i<derivationSize; i++) {
			this.derivations[i] = new int[]{-1, uniqueChildForest.root, i, -1, -1};
			this.derivationLogProb[i] = uniqueChildForest.derivationLogProb[i] + ruleLogProb;
		}
		//this.checkDerivationConsistency();
	}
	
	/**
	 * Create a new DerivationForest starting with a pair of terminal X,K form two cells of the
	 * current CKY table for the rule root -> X K.
	 * @param root
	 * @param leftForest
	 * @param upForest
	 * @param split
	 * @param ruleLogProb
	 */
	public DerivationForest(int root, DerivationForest leftForest, DerivationForest upForest, 
			int split, double ruleLogProb) {				
		this.root = root;
		int derivationSize = Math.min(Parameters.nBest, leftForest.derivations.length*upForest.derivations.length);
		this.derivations = new int[derivationSize][5];
		this.derivationLogProb = new double[derivationSize];
		logInsideProb = leftForest.logInsideProb + upForest.logInsideProb + ruleLogProb;
		int derivationIndex = 0;
		
		DerivationQueue queue = new DerivationQueue(leftForest, upForest, split);
		DerivationElement topQueueDerivation = queue.pollFirst();
		
		do {			
			this.derivationLogProb[derivationIndex] = topQueueDerivation.logProb + ruleLogProb;
			this.derivations[derivationIndex] = topQueueDerivation.indexes;
			derivationIndex++;
			topQueueDerivation = queue.addNeighboursAndPoll(topQueueDerivation);
		} while(derivationIndex < derivationSize);
		//this.checkDerivationConsistency();
	}
	
	/**
	 * Update the current DerivationForest with a pair of terminal X,K form two cells of the
	 * current CKY table for the internal unary rule root -> uniqueChild
	 * @param root
	 * @param uniqueChildForest
	 * @param ruleLogProb
	 */
	public boolean updateForest(DerivationForest uniqueChildForest, double ruleLogProb) {
		logInsideProb = Math.log(Math.exp(logInsideProb) + Math.exp(uniqueChildForest.logInsideProb + 
						ruleLogProb));		
		int currentDerivationIndex = 0;
		int leftDerivationIndex = 0;
		int newDerivationSize;				
		if (this.derivations.length==Parameters.nBest) newDerivationSize = Parameters.nBest;
		else {
			int completeDerivationSize = this.derivations.length + uniqueChildForest.derivations.length;
			newDerivationSize = Math.min(Parameters.nBest, completeDerivationSize);
		}		
		
		int[][] newDerivations = new int[newDerivationSize][5];
		double[] newDerivationLogProb = new double[newDerivationSize];
		int newDerivationIndex=0;
		
		double leftBestDerivationProbPartial = uniqueChildForest.derivationLogProb[0];
		double leftBestDerivationProb = leftBestDerivationProbPartial + ruleLogProb;
		if (leftBestDerivationProb<=this.derivationLogProb[this.derivations.length-1]) {
			if (this.derivations.length==Parameters.nBest) return false; //no need to update
			//justAppendAtTheEnd			
			for(int i=0; i<this.derivations.length; i++) {
				newDerivations[i] = this.derivations[i];
				newDerivationLogProb[i] = this.derivationLogProb[i];
			}
			newDerivationIndex = this.derivations.length;
			appendLeftDerivations(-1, ruleLogProb, leftDerivationIndex, 
					uniqueChildForest, newDerivations, newDerivationLogProb, newDerivationIndex);
			return true;
		}

		double currentDerivationProb = this.derivationLogProb[currentDerivationIndex];
		do {			
			if (leftBestDerivationProb>currentDerivationProb) {
				do {
					newDerivations[newDerivationIndex] = new int[]{-1, uniqueChildForest.root, leftDerivationIndex, -1, -1};
					newDerivationLogProb[newDerivationIndex] = leftBestDerivationProb;
					leftDerivationIndex++;
					newDerivationIndex++;
					if (uniqueChildForest.derivations.length==leftDerivationIndex) {
						appendCurrentDerivations(currentDerivationIndex, newDerivations, 
								newDerivationLogProb, newDerivationIndex);
						return true;
					}
					leftBestDerivationProb = uniqueChildForest.derivationLogProb[leftDerivationIndex] + ruleLogProb;
				} while (leftBestDerivationProb>currentDerivationProb && newDerivationIndex < newDerivationSize);
			}
			else {
				do {
					newDerivations[newDerivationIndex] = this.derivations[currentDerivationIndex];
					newDerivationLogProb[newDerivationIndex] = this.derivationLogProb[currentDerivationIndex];
					currentDerivationIndex++;
					newDerivationIndex++;
					if (this.derivations.length==currentDerivationIndex) {
						appendLeftDerivations(-1, ruleLogProb, leftDerivationIndex, 
								uniqueChildForest, newDerivations, newDerivationLogProb, newDerivationIndex);
						return true;
					}
					currentDerivationProb = this.derivationLogProb[currentDerivationIndex];
				} while (leftBestDerivationProb<=currentDerivationProb && newDerivationIndex < newDerivationSize);
			}
		} while (newDerivationIndex<newDerivations.length);
		this.derivations = newDerivations;
		this.derivationLogProb = newDerivationLogProb;
		//this.checkDerivationConsistency();
		return true;
	}
	
	/**
	 * Update the current DerivationForest with a pair of terminal X,K form two cells of the
	 * current CKY table for the rule root -> X K.
	 * @param root
	 * @param leftForest
	 * @param split
	 * @param ruleLogProb
	 */
	public boolean updateForest(DerivationForest leftForest, DerivationForest upForest, int split, double ruleLogProb) {
		logInsideProb = Math.log(Math.exp(logInsideProb) + Math.exp(leftForest.logInsideProb + 
						upForest.logInsideProb + ruleLogProb));
		int currentDerivationIndex = 0;
		int newDerivationIndex = 0;
		int newDerivationSize;				
		if (this.derivations.length==Parameters.nBest) newDerivationSize = Parameters.nBest;
		else {
			int completeDerivationSize = this.derivations.length + leftForest.derivations.length * 
										upForest.derivations.length;
			newDerivationSize = Math.min(Parameters.nBest, completeDerivationSize);
		}
		int[][] newDerivations = new int[newDerivationSize][5];
		double[] newDerivationLogProb = new double[newDerivationSize];
		
		DerivationQueue queue = new DerivationQueue(leftForest, upForest, split);		
		DerivationElement topQueueDerivation = queue.pollFirst();		
		double topQueueDerivationProb = topQueueDerivation.logProb + ruleLogProb;
		
		if (topQueueDerivationProb<=this.derivationLogProb[this.derivations.length-1]) {
			if (this.derivations.length==Parameters.nBest) return false; //no need to update
			//justAppendAtTheEnd			
			for(int i=0; i<this.derivations.length; i++) {
				newDerivations[i] = this.derivations[i];
				newDerivationLogProb[i] = this.derivationLogProb[i];
			}
			newDerivationIndex = this.derivations.length;
			appendQueue(queue, topQueueDerivation, ruleLogProb, newDerivations, newDerivationLogProb, 
					newDerivationIndex);
			return true;
		}
		
		topQueueDerivationProb = topQueueDerivation.logProb + ruleLogProb;
		double currentDerivationProb = this.derivationLogProb[currentDerivationIndex];
		do {			
			if (topQueueDerivation==null) { //empty queue
				appendCurrentDerivations(currentDerivationIndex, newDerivations, 
						newDerivationLogProb, newDerivationIndex);
				return true;
			}						
			if(topQueueDerivationProb>currentDerivationProb) {
				do {
					newDerivations[newDerivationIndex] = topQueueDerivation.indexes;
					newDerivationLogProb[newDerivationIndex] = topQueueDerivationProb;
					newDerivationIndex++;
					topQueueDerivation = queue.addNeighboursAndPoll(topQueueDerivation);
					if (topQueueDerivation==null) break;
					topQueueDerivationProb = topQueueDerivation.logProb + ruleLogProb;
				} while (topQueueDerivationProb>currentDerivationProb && newDerivationIndex < newDerivationSize);				
			}
			else {
				do {
					newDerivations[newDerivationIndex] = this.derivations[currentDerivationIndex];
					newDerivationLogProb[newDerivationIndex] = this.derivationLogProb[currentDerivationIndex];
					newDerivationIndex++;
					currentDerivationIndex++;
					if (this.derivations.length==currentDerivationIndex) {
						appendQueue(queue, topQueueDerivation, ruleLogProb, newDerivations, 
								newDerivationLogProb, newDerivationIndex);
						return true;
					}
					else currentDerivationProb = this.derivationLogProb[currentDerivationIndex];
				} while (topQueueDerivationProb<=currentDerivationProb && newDerivationIndex < newDerivationSize);
			}									
		} while(newDerivationIndex < newDerivationSize);
		this.derivations = newDerivations;
		this.derivationLogProb = newDerivationLogProb;
		//this.checkDerivationConsistency();
		return true;
	}

	private void appendCurrentDerivations(int currentDerivationIndex, int[][] newDerivations, 
			double[] newDerivationLogProb, int newDerivationIndex) {
		while(newDerivationIndex < newDerivations.length) {
			newDerivations[newDerivationIndex] = this.derivations[currentDerivationIndex];
			newDerivationLogProb[newDerivationIndex] = this.derivationLogProb[currentDerivationIndex];
			currentDerivationIndex++;
			newDerivationIndex++;
		}
		this.derivations = newDerivations;
		this.derivationLogProb = newDerivationLogProb;
		//this.checkDerivationConsistency();
	}
	
	private void appendLeftDerivations(int split, double ruleLogProb, int leftDerivationIndex, 
			DerivationForest leftForest, int[][] newDerivations, double[] newDerivationLogProb, 
			int newDerivationIndex) {
		while(newDerivationIndex < newDerivations.length) {
			newDerivations[newDerivationIndex] = new int[]{split, leftForest.root, leftDerivationIndex, -1, -1};
			newDerivationLogProb[newDerivationIndex] = leftForest.derivationLogProb[leftDerivationIndex] + ruleLogProb;
			leftDerivationIndex++;
			newDerivationIndex++;
		}
		this.derivations = newDerivations;
		this.derivationLogProb = newDerivationLogProb;
		//this.checkDerivationConsistency();
	}
	
	private void appendQueue(DerivationQueue queue, DerivationElement topQueueDerivation, 
			double ruleLogProb, int[][] newDerivations, double[] newDerivationLogProb, int newDerivationIndex) {
		while(newDerivationIndex < newDerivations.length) {			
			double topQueueDerivationProb = topQueueDerivation.logProb + ruleLogProb;
			newDerivations[newDerivationIndex] = topQueueDerivation.indexes;
			newDerivationLogProb[newDerivationIndex] = topQueueDerivationProb;
			topQueueDerivation = queue.addNeighboursAndPoll(topQueueDerivation);						
			newDerivationIndex++;
		}		
		this.derivations = newDerivations;
		this.derivationLogProb = newDerivationLogProb;
		//this.checkDerivationConsistency();
	}
	
	public String toString() {
		String result = "";
		//if (this.derivations==null) return result; 
		result += Arrays.toString(this.derivations[0]) + ":" + this.derivationLogProb[0];
		if (this.derivations.length>1) result += " ...";
		return result;
	}
	
	/*public void checkDerivationConsistency() {
		for(int i=1; i<this.derivationLogProb.length; i++) {
			if (this.derivationLogProb[i]>this.derivationLogProb[i-1]) {
				System.out.println("Consistency error!");
			}
			if (Arrays.equals(this.derivations[i], this.derivations[i-1]) ) {
				System.out.println("Consistency error!");
			}
		}
	}*/
	
	public int hashCode() {
		return root;		
	}
}
