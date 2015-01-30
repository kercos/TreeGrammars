package tsg.kernels;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Hashtable;
import java.util.ListIterator;
import java.util.Map.Entry;

import kernels.NodeSetCollector;
import kernels.NodeSetCollectorMUB;
import kernels.NodeSetCollectorSimple;
import tsg.TSNodeLabel;
import tsg.TSNodeLabelIndex;
import tsg.TSNodeLabelStructure;

public class CommonSubBranchMUBFreqThreads extends CommonSubBranch {
	
	public CommonSubBranchMUBFreqThreads(ArrayList<TSNodeLabelStructure> treebank, 
			int threads, int startIndex) {

		super(treebank, threads, startIndex);
	}
	
	protected void initiateThreadArray() {
		threadsArray = new CountFragmentsThread[threads];
		for(int i=0; i<threads; i++) {
			threadsArray[i] = new CountFragmentsThread();
		}
	}
		
	private class CountFragmentsThread extends CommonStructuresThread {
		
		Hashtable<TSNodeLabel, int[]> freqTable;
		
		public CountFragmentsThread() {
			super();
			freqTable = new Hashtable<TSNodeLabel, int[]>();
		}
		
		protected void collectCommonFragments(TSNodeLabelStructure[] trees) {						
			for(TSNodeLabelStructure t1 : trees) {
				if (t1==null) break;
				ListIterator<TSNodeLabelStructure> iter = treebank.listIterator(++startIndex[0]);
				NodeSetCollectorSimple intermediateCollector = new NodeSetCollectorSimple();
				while (iter.hasNext()) {									
					TSNodeLabelStructure t2 = iter.next();
					NodeSetCollectorMUB[][] CST = getCST(t1, t2);
					extractSubTreesIntermediate(CST, intermediateCollector);
				}			
				extractSubBranchesInTableThread(intermediateCollector, t1);					
			}
		}
		
		
		private void extractSubBranchesInTableThread(NodeSetCollectorSimple intermediateCollector,
				TSNodeLabelStructure s) {		
			for(BitSet bs : intermediateCollector.bitSetSet) {
				TSNodeLabelIndex rootNode = s.structure[bs.nextSetBit(0)];
				TSNodeLabel fragment = rootNode.getSubBranch(bs);
				addThread(fragment); 			
			}
		}
		
		private void addThread(TSNodeLabel fragment) {
			int[] freq = freqTable.get(fragment);
			if (freq==null) freqTable.put(fragment, new int[]{1});
			else freq[0]++;
		}
		
		protected void printFragmentsToFile(BufferedWriter fileWriter) throws IOException {			
			for(Entry<TSNodeLabel, int[]> e : freqTable.entrySet()) {
				TSNodeLabel fragment = e.getKey();
				int freq = e.getValue()[0];				
				String fragmetnFreqString = fragment.toString(false, true) + "\t" + freq; 
				fileWriter.write(fragmetnFreqString + "\n");				
			}
		}		
		
		protected void clearFragmentBank() {
			freqTable.clear();
		}
	
	}	
	
	public static void main(String[] args) throws Exception {
	}


		
}
