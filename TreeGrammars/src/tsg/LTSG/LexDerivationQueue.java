package tsg.LTSG;

import java.util.PriorityQueue;

import tsg.TSNode;
import util.*;

public class LexDerivationQueue {
	
	PriorityQueue<LexicalDerivation> queue;
	LexicalDerivation[][] subSideNBestTable;
	TSNode anchor;
	int combinations;
	int anchorIndex;
	double lexTreeProb;
	int[] subSideSizes;
	boolean[] accessMatrix;
	LexicalDerivation lastReturned;
	
	
	public LexDerivationQueue(LexicalDerivation[][] subSideNBestTable, TSNode anchor, 
			int anchorIndex, double lexTreeProb) {
		queue = new PriorityQueue<LexicalDerivation>();
		this.subSideNBestTable = subSideNBestTable;
		this.anchor = anchor;
		this.anchorIndex = anchorIndex;
		this.lexTreeProb = lexTreeProb;
		subSideSizes = new int[subSideNBestTable.length];
		combinations = 1;
		for(int i=0; i<subSideNBestTable.length; i++) {
			//if (subSideNBestTable[i]==null) subSideSizes[i] = 0;
			//else {
				subSideSizes[i] = subSideNBestTable[i].length;
				combinations *= subSideNBestTable[i].length;
			//}			 			
		}		
		accessMatrix = new boolean[combinations];				
	}
	
	private int oneDimIndex(int[] indexes) {
		if(indexes.length>2) {
			System.out.print("");
		}
		int uniIndex = indexes[0];
		int multFact = subSideSizes[0];
		for(int i = 1; i<indexes.length; i++) {
			uniIndex += multFact * indexes[i];
			multFact *= subSideSizes[i];
		}
		return uniIndex;
	}
	
	private LexicalDerivation createNewLexicalDerivation(int[] indexes) {
		LexicalDerivation[] indexSubSiteDerivations = new LexicalDerivation[indexes.length];
		double logDerivationProb = lexTreeProb;
		for(int i=0; i<indexes.length; i++) {
			if (indexes[i]!=-1) {
				logDerivationProb += subSideNBestTable[i][indexes[i]].logDerivationProb;
				indexSubSiteDerivations[i] = subSideNBestTable[i][indexes[i]];
			}			
		}
		int[] indexCopy = new int[indexes.length];
		for(int i=0; i<indexes.length; i++) indexCopy[i] = indexes[i];
		LexicalDerivation topQueueDerivation = 
			new LexicalDerivation(anchor, anchorIndex, logDerivationProb, indexSubSiteDerivations, indexCopy);
		return topQueueDerivation;
	}
		
		
	public LexicalDerivation pollFirst() {
		int[] indexes = new int[subSideNBestTable.length];
		for(int i=0; i<subSideNBestTable.length; i++) {
			indexes[i] = (subSideNBestTable[i]==null)? -1 : 0;
		}		
		accessMatrix[0] = true;
		lastReturned = createNewLexicalDerivation(indexes);
		return lastReturned;
	}
	
	public LexicalDerivation addNeighboursAndPoll() {
		int[] lastExtractedIndexes = lastReturned.subSiteDerivationsIndexes;
		boolean noNeighboursAdded = true;
		for(int i=0; i<subSideNBestTable.length; i++) {
			if (lastExtractedIndexes[i] != -1 && 
					lastExtractedIndexes[i]+1 < subSideSizes[i]) {
				lastExtractedIndexes[i]++;
				int oneDimIndex = oneDimIndex(lastExtractedIndexes);
				if (!accessMatrix[oneDimIndex]) {
					noNeighboursAdded = false;
					queue.add(createNewLexicalDerivation(lastExtractedIndexes));
					accessMatrix[oneDimIndex] = true;
				}
				lastExtractedIndexes[i]--;
			}
		}
		//if (noNeighboursAdded) {
		//	if (Utility.containsFalse(this.accessMatrix)) {
		//		System.out.println("");
		//	}			
		//}
		lastReturned = queue.poll();
		return lastReturned;
	}

}
