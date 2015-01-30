package tsg.kernels;

import java.io.File;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Random;

import kernels.NodeSetCollector;
import kernels.NodeSetCollectorMUB;
import kernels.NodeSetCollectorSimple;
import kernels.NodeSetCollectorUnion;
import tsg.TSNodeLabel;
import tsg.TSNodeLabelIndex;
import tsg.TSNodeLabelStructure;

public abstract class CommonSubtrees extends CommonStructures {
	
	public CommonSubtrees(ArrayList<TSNodeLabelStructure> treebank, 
			int threads, int startIndex) {
		super(treebank, threads, startIndex);		
	}

	protected static NodeSetCollectorUnion[][] getCST(TSNodeLabelStructure t1, TSNodeLabelStructure t2) {				
		NodeSetCollectorUnion[][] CST = new NodeSetCollectorUnion[t1.length()][t2.length()];
		int currentLength = t1.length();
		for(TSNodeLabelIndex nodeA : t1.structure()) {
			for(TSNodeLabelIndex nodeB : t2.structure()) {	
				getCST(nodeA, nodeB, CST, currentLength);				
			}			
		}
		return CST;
	}
	
	private static NodeSetCollectorUnion getCST(TSNodeLabelIndex nodeA, TSNodeLabelIndex nodeB,
			NodeSetCollectorUnion[][] CST,  int currentLength) {
		NodeSetCollectorUnion stored = CST[nodeA.index][nodeB.index];
		if (stored != null) return stored;							
		NodeSetCollectorUnion nodeCollector = new NodeSetCollectorUnion();
		nodeCollector.setMaxLength(currentLength);
		
		if (nodeA.sameLabel(nodeB)) {						
			nodeCollector.addDefaultBitSet(nodeA.index);
			if (!nodeA.isLexical && nodeA.sameDaughtersLabel(nodeB)) {
				TSNodeLabel[] daughtersA = nodeA.daughters;
				TSNodeLabel[] daughtersB = nodeB.daughters;
				int prole = nodeA.daughters.length;				
				NodeSetCollectorUnion[] nodeCollDaughters = new NodeSetCollectorUnion[prole];
				int[] nodeSetDaughtersLength = new int[prole];
				for(int i=0; i<prole; i++) {
					TSNodeLabelIndex nodeADaughter = (TSNodeLabelIndex)daughtersA[i];
					TSNodeLabelIndex nodeBDaughter = (TSNodeLabelIndex)daughtersB[i];					
					NodeSetCollectorUnion collD = getCST(nodeADaughter, nodeBDaughter, CST, currentLength);
					nodeCollDaughters[i] = collD;
					nodeSetDaughtersLength[i] = collD.size();
				}			
				nodeCollector.addAllCombinations(nodeCollDaughters, 
						nodeSetDaughtersLength, nodeA.index);
				//if (!extractIntermediateStructures) {
					for(NodeSetCollectorUnion collD : nodeCollDaughters) {
						collD.makeEmpty();					
					}
				//}
				
			}
		}
		return CST[nodeA.index][nodeB.index] = nodeCollector;		
	}
	
	protected static void extractSubTreesIntermediate(NodeSetCollectorUnion[][] CPG,
			NodeSetCollectorSimple intermediateCollector) {
		
		NodeSetCollectorMUB finalNodeSet = new NodeSetCollectorMUB();
		for(NodeSetCollectorUnion[] wordCollectors : CPG) {
			for(NodeSetCollectorUnion coll : wordCollectors) {
				BitSet singleBS = coll.singleBS();
				if (singleBS==null) continue;
				finalNodeSet.add(singleBS);
			}						
		}				
		for(BitSet bs : finalNodeSet.bitSetSet) {
			if (bs.cardinality()==1) continue;
			intermediateCollector.add(bs);					
		}
	}
	
	public static TSNodeLabel[] extractSubTrees(NodeSetCollectorSimple intermediateCollector,
			TSNodeLabelStructure s) {
		TSNodeLabel[] result = new TSNodeLabel[intermediateCollector.size()];
		int index = 0;
		for(BitSet bs : intermediateCollector.bitSetSet) {
			TSNodeLabelIndex rootNode = s.structure[bs.nextSetBit(0)];
			TSNodeLabel fragment = rootNode.getSubTree(bs);
			result[index++] = fragment;			
		}
		return result;
	}

	
	
	public static void main(String[] args) throws Exception {
		//CommonStructures.extractIntermediateStructures = true;
		File inputFile = new File("tmp/treesWo.mrg");
		ArrayList<TSNodeLabelStructure> treebank = TSNodeLabelStructure.readTreebank(inputFile);
		int size = treebank.size();
		Random random = new Random();
		TSNodeLabel toFind = new TSNodeLabel("(MD \"wo\")", false); 
		boolean found = false;
		while(!found) {
			int index1 = random.nextInt(size);
			int index2 = random.nextInt(size);
			TSNodeLabelStructure t1 = treebank.get(index1);
			TSNodeLabelStructure t2 = treebank.get(index2);
			//TSNodeLabelStructure t1 = new TSNodeLabelStructure("(TOP (S (SBAR (IN While) (S (NP (NNS rights) (NNS fees)) (VP (VBP head) (ADVP (RB skyward))))) (, ,) (NP (NN ad) (NNS rates)) (VP (MD wo) (RB n't)) (. .)))");
			//TSNodeLabelStructure t2 = t1;
			NodeSetCollectorUnion[][] CST = getCST(t1,t2);
			NodeSetCollectorSimple intermediateCollector = new NodeSetCollectorSimple();
			extractSubTreesIntermediate(CST, intermediateCollector);
			TSNodeLabel[] recurringFragments = extractSubTrees(intermediateCollector, t1);			
			for(TSNodeLabel rf : recurringFragments) {
				//System.out.println(rf);
				//System.out.println(rf.equals(toFind));				
				if (rf.equals(toFind)) {				
					System.out.println(index1);
					System.out.println(index2);
					String t1String = t1.structure[0].toStringQtree();
					String t2String = t2.structure[0].toStringQtree();
					System.out.println(t1String);
					System.out.println(t2String);
					System.out.println(t1String.length());
					System.out.println(t2String.length());
					for(TSNodeLabel rf1 : recurringFragments) {
						System.out.println(rf1);
					}
					found = true;
					break;
				}							
			}
		}		
	}
	
}
