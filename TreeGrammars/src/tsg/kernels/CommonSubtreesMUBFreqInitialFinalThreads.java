package tsg.kernels;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Hashtable;
import java.util.ListIterator;
import java.util.Map.Entry;

import kernels.NodeSetCollectorSimple;
import kernels.NodeSetCollectorUnion;
import tsg.TSNodeLabel;
import tsg.TSNodeLabelIndex;
import tsg.TSNodeLabelStructure;

public class CommonSubtreesMUBFreqInitialFinalThreads extends CommonSubtrees {
	
	public CommonSubtreesMUBFreqInitialFinalThreads(ArrayList<TSNodeLabelStructure> treebank, 
			int threads, int startIndex) {

		super(treebank, threads, startIndex);
	}
	
	protected void initiateThreadArray() {
		threadsArray = new CountFragmentsThread[threads];
		for(int i=0; i<threads; i++) {
			threadsArray[i] = new CountFragmentsThread();
		}
	}

	private class CountFragmentsThread extends CommonStructuresThread{
		
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
					NodeSetCollectorUnion[][] CST = getCST(t1, t2);
					extractSubTreesIntermediate(CST, intermediateCollector);
				}			
				extractSubTreesInTableThread(intermediateCollector, t1);					
			}
		}
		
		
		private void extractSubTreesInTableThread(NodeSetCollectorSimple intermediateCollector,
				TSNodeLabelStructure s) {
			TSNodeLabelIndex topNode = s.structure[0];
			int firstWordNodeIndex = ((TSNodeLabelIndex)topNode.getLeftmostLexicalNode()).index;
			int lastWordNodeIndex = ((TSNodeLabelIndex)topNode.getRightmostLexicalNode()).index;			
			for(BitSet bs : intermediateCollector.bitSetSet) {
				boolean hasFirstWord = bs.get(firstWordNodeIndex);
				boolean hasLastWord = bs.get(lastWordNodeIndex);
				TSNodeLabelIndex rootNode = s.structure[bs.nextSetBit(0)];
				TSNodeLabel fragment = rootNode.getSubTree(bs);				
				addThread(fragment, hasFirstWord, hasLastWord); 			
			}
		}
		
		private void addThread(TSNodeLabel fragment, boolean hasFirstWord, boolean hasLastWord) {
			int[] freq = freqTable.get(fragment);
			if (freq==null) {
				freq = new int[3];
				freqTable.put(fragment, freq);
			}
			freq[0]++;
			if (hasFirstWord)
				freq[1]++;
			if (hasLastWord)
				freq[2]++;
		}
		
		protected void printFragmentsToFile(BufferedWriter fileWriter) throws IOException {			
			for(Entry<TSNodeLabel, int[]> e : freqTable.entrySet()) {
				TSNodeLabel fragment = e.getKey();
				int[] freq = e.getValue();				
				String fragmetnFreqString = fragment.toString(false, true) 
					+ "\t" + freq[0] + "\t" + freq[1]+ "\t" + freq[2]; 
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
