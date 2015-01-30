package tsg.kernels;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.ListIterator;

import kernels.NodeSetCollectorSimple;
import kernels.NodeSetCollectorUnion;
import tsg.TSNodeLabel;
import tsg.TSNodeLabelIndex;
import tsg.TSNodeLabelStructure;

public class CommonSubtreesMUBThreads extends CommonSubtrees {
	
	public CommonSubtreesMUBThreads(ArrayList<TSNodeLabelStructure> treebank, 
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
		
		HashSet<TSNodeLabel> fragmentSet;		
		
		public CountFragmentsThread() {
			super();
			fragmentSet = new HashSet<TSNodeLabel>();
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
			for(BitSet bs : intermediateCollector.bitSetSet) {
				TSNodeLabelIndex rootNode = s.structure[bs.nextSetBit(0)];
				TSNodeLabel fragment = rootNode.getSubTree(bs);
				fragmentSet.add(fragment); 			
			}
		}
		
		protected void printFragmentsToFile(BufferedWriter fileWriter) throws IOException {						
			for(TSNodeLabel fragment : fragmentSet) {				
				String fragmetnFreqString = fragment.toString(false, true) + "\t1"; 
				fileWriter.write(fragmetnFreqString + "\n");				
			}
		}
		
		protected void clearFragmentBank() {
			fragmentSet.clear();
		}
	
	}	
	
	
	public static void main(String[] args) throws Exception {

	}


		
}
