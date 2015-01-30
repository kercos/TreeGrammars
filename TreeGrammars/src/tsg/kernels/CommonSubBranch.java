package tsg.kernels;

import java.io.File;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedHashSet;
import java.util.Random;

import kernels.*;

import settings.Parameters;
import tsg.*;
import util.Pair;

public abstract class CommonSubBranch extends CommonStructures {
	
	public CommonSubBranch(ArrayList<TSNodeLabelStructure> treebank, 
			int threads, int startIndex) {
		super(treebank, threads, startIndex);		
	}

	protected static NodeSetCollectorMUB[][] getCST(TSNodeLabelStructure t1, TSNodeLabelStructure t2) {				
		NodeSetCollectorMUB[][] CST = new NodeSetCollectorMUB[t1.length()][t2.length()];
		int currentLength = t1.length();		
		for(TSNodeLabelIndex nodeA : t1.structure) {
			for(TSNodeLabelIndex nodeB : t2.structure) {
				getCST(nodeA, nodeB, CST, currentLength);				
			}			
		}
		return CST;
	}
	
	
	private static NodeSetCollectorMUB getCST(TSNodeLabelIndex nodeA, TSNodeLabelIndex nodeB,
			NodeSetCollectorMUB[][] CST, int currentLength) {
		NodeSetCollectorMUB stored = CST[nodeA.index][nodeB.index];
		if (stored != null) return stored;							
		NodeSetCollectorMUB nodeCollector = new NodeSetCollectorMUB();
		nodeCollector.setMaxLength(currentLength);
		
		if (nodeA.sameLabel(nodeB)) {						
			nodeCollector.addDefaultBitSet(nodeA.index);
			
			if (!nodeA.isLexical) {
				ArrayList<ArrayList<Pair<TSNodeLabel>>> allDaughtersMatch =
					AllOrderedNodeSubSet.allDaughtersMatchBackupOnSimple(nodeA, nodeB);
				if (allDaughtersMatch!=null) {
					int p=0;
					for(ArrayList<Pair<TSNodeLabel>> pairList : allDaughtersMatch) {
						int prole = pairList.size();
						NodeSetCollectorMUB[] nodeCollDaughters = new NodeSetCollectorMUB[prole];
						int[] nodeSetDaughtersLength = new int[prole];
						int i=0;
						long totalComb = 1;
						for(Pair<TSNodeLabel> pair : pairList) {
							TSNodeLabelIndex nodeADaughter = (TSNodeLabelIndex)pair.getFirst();
							TSNodeLabelIndex nodeBDaughter = (TSNodeLabelIndex)pair.getSecond();					
							NodeSetCollectorMUB collD = getCST(nodeADaughter, nodeBDaughter,
									CST, currentLength);
							nodeCollDaughters[i] = collD;
							int size = collD.size();
							nodeSetDaughtersLength[i] = size;
							totalComb *= size;
							i++;
						}		
						if (totalComb>maxCombDaughters) {
							Parameters.appendReturnInLogFile("Skipping comparison: total number of combinaitons to high: " + totalComb +
									"\n\t" + nodeA + "\n\t" + nodeB );							
							continue;
						}
						nodeCollector.addAllCombinations(nodeCollDaughters, 
								nodeSetDaughtersLength, nodeA.index);
						p++;
						//if (!extractIntermediateStructures) {
						for(NodeSetCollectorMUB collD : nodeCollDaughters) {
							collD.makeEmpty();					
						}
						//}
					}
				}					
			}
		}
		return CST[nodeA.index][nodeB.index] = nodeCollector;		
	}	
	
	public static void extractSubTreesIntermediate(NodeSetCollectorMUB[][] CPG,
			NodeSetCollectorSimple intermediateCollector) {
		int nodeIndex = -1;
		NodeSetCollectorMUB finalNodeSet = new NodeSetCollectorMUB();
		for(NodeSetCollectorMUB[] wordCollectors : CPG) {
			nodeIndex++;
			for(NodeSetCollectorMUB coll : wordCollectors) {
				finalNodeSet.addAll(coll);
			}						
		}				
		for(BitSet bs : finalNodeSet.bitSetSet) {
			if (bs.cardinality()==1) continue;
			intermediateCollector.add(bs);					
		}
	}

	/*
	protected static long getMaxCombinationDaughtersMatch (TSNodeLabelStructure t1,
			TSNodeLabelStructure t2) {				
		long max = 0;
		for(TSNodeLabelIndex nodeA : t1.structure) {
			for(TSNodeLabelIndex nodeB : t2.structure) {
				if (nodeA.sameLabel(nodeB) && !nodeA.isLexical && !nodeB.isLexical) {
					long comb = AllOrderedNodeSubSet.allDaughtersMatchSize(nodeA, nodeB);
					if (comb>max) max = comb;
				}
			}			
		}
		return max;
	}
	*/
	
	public static TSNodeLabel[] extractSubBranches(NodeSetCollectorSimple intermediateCollector,
			TSNodeLabelStructure s) {
		TSNodeLabel[] result = new TSNodeLabel[intermediateCollector.size()];
		int index = 0;
		for(BitSet bs : intermediateCollector.bitSetSet) {
			TSNodeLabelIndex rootNode = s.structure[bs.nextSetBit(0)];
			TSNodeLabel fragment = rootNode.getSubBranch(bs);
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
			if (index1==172 || index2==172) continue;
			TSNodeLabelStructure t1 = treebank.get(index1);
			TSNodeLabelStructure t2 = treebank.get(index2);
			//TSNodeLabelStructure t1 = new TSNodeLabelStructure("(TOP (S (SBAR (IN While) (S (NP (NNS rights) (NNS fees)) (VP (VBP head) (ADVP (RB skyward))))) (, ,) (NP (NN ad) (NNS rates)) (VP (MD wo) (RB n't)) (. .)))");
			//TSNodeLabelStructure t2 = t1;
			NodeSetCollectorMUB[][] CST = getCST(t1,t2);
			NodeSetCollectorSimple intermediateCollector = new NodeSetCollectorSimple();
			extractSubTreesIntermediate(CST, intermediateCollector);
			TSNodeLabel[] recurringFragments = extractSubBranches(intermediateCollector, t1);			
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
