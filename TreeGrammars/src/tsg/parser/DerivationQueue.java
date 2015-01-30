package tsg.parser;

import java.util.PriorityQueue;

public class DerivationQueue {
	
	PriorityQueue<DerivationElement> queue;
	DerivationForest leftForest, upForest;
	int split;
	boolean[][] accessMatrix;
	
	public DerivationQueue(DerivationForest leftForest, DerivationForest upForest, int split) {
		queue = new PriorityQueue<DerivationElement>();
		this.leftForest = leftForest;
		this.upForest = upForest;
		this.split = split;
		accessMatrix = new boolean[leftForest.size()][upForest.size()];				
	}
	
		
	public DerivationElement pollFirst() {
		double logProbInitialPartial = leftForest.derivationLogProb[0] + upForest.derivationLogProb[0];
		int[] topIndexes = new int[] {split, leftForest.root, 0, upForest.root, 0};
		DerivationElement topQueueDerivation = new DerivationElement(topIndexes, logProbInitialPartial);
		accessMatrix[0][0] = true;
		return topQueueDerivation;
	}
	
	public DerivationElement addNeighboursAndPoll(DerivationElement topQueueDerivation) {
		addNeighbours(topQueueDerivation);
		return queue.poll();
	}
	
	public void addNeighbours(DerivationElement topQueueDerivation) {		
		int currentIndexLeft = topQueueDerivation.indexes[2];
		int newIndexLeft = topQueueDerivation.indexes[2] + 1;
		int currentIndexUp = topQueueDerivation.indexes[4];
		int newIndexUp = topQueueDerivation.indexes[4] + 1;
		if (leftForest.size() != newIndexLeft && !accessMatrix[newIndexLeft][currentIndexUp]) {
			int[] newIndexLeftNeighbour = new int[]{split, leftForest.root, newIndexLeft, upForest.root, currentIndexUp};
			double newProbLeftNeighbourPartial = leftForest.derivationLogProb[newIndexLeft] + 
													upForest.derivationLogProb[currentIndexUp];
			DerivationElement leftNeighbour = new DerivationElement(newIndexLeftNeighbour, newProbLeftNeighbourPartial);
			queue.add(leftNeighbour);
			accessMatrix[newIndexLeft][currentIndexUp] = true;
		}		
		if (upForest.size() != newIndexUp && !accessMatrix[currentIndexLeft][newIndexUp]) {
			int[] newIndexUpNeighbour = new int[]{split, leftForest.root, currentIndexLeft, upForest.root, newIndexUp};
			double newProbUpNeighbourPartial = leftForest.derivationLogProb[currentIndexLeft] + 
													upForest.derivationLogProb[newIndexUp];
			DerivationElement upNeighbour = new DerivationElement(newIndexUpNeighbour, newProbUpNeighbourPartial);
			queue.add(upNeighbour);
			accessMatrix[currentIndexLeft][newIndexUp] = true;
		}
	}

}
