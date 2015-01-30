package tsg.LTSG;

import java.util.PriorityQueue;


public class LDQQueue {
	
	PriorityQueue<LexicalDerivation> queue;
	LexDerivationQueue[] derivationQueues;
	LexicalDerivation lastReturned;
	
	public LDQQueue(LexDerivationQueue[] derivationQueues) {
		queue = new PriorityQueue<LexicalDerivation>();
		this.derivationQueues = derivationQueues;
	}
			
	public LexicalDerivation pollFirst() {		
		for(LexDerivationQueue LDQ : derivationQueues) {
			if (LDQ!=null) queue.add(LDQ.pollFirst());
		}
		lastReturned = queue.poll(); 
		return lastReturned;
	}
	
	public LexicalDerivation addNeighboursAndPoll() {
		LexicalDerivation next = this.derivationQueues[lastReturned.anchorIndex].addNeighboursAndPoll();
		if (next!=null) queue.add(next);		 		
		lastReturned = queue.poll();
		return lastReturned;
	}

}
