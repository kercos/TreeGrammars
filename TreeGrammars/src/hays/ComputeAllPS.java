package hays;

import java.util.Vector;

import util.Utility;

public class ComputeAllPS {

	
	public static class PSTree {
		
		int span;
		int startIndex, endIndex;
		PSTree[] daughters;
		
		public PSTree(int span) {
			this.span = span;
			startIndex = 0;
			endIndex = span-1;
		}
		
		public PSTree(int startIndex, int endIndex) {
			this.startIndex = startIndex;
			this.endIndex = endIndex;
			span = endIndex - startIndex + 1;
		}
		
		public PSTree clone() {
			PSTree result = new PSTree(startIndex, endIndex);
			if (this.daughters!=null) {
				int prole = this.daughters.length;
				result.daughters = new PSTree[prole];
				for(int i=0; i<prole; i++) {
					result.daughters[i] = this.daughters[i].clone();
				}
			}
			return result;
		}
		
		public Vector<PSTree> getAllSplits() {			
			Vector<PSTree> result = new Vector<PSTree>();
			if (span==1) {
				result.add(this);
				return result;
			}
			int positions = span-1;
			for(int breakNumb=1; breakNumb<span; breakNumb++) {
				int[][] combs = Utility.n_air(positions, breakNumb);
				for(int[] breakPositions : combs) {
					int prole = breakNumb+1;
					int breakIndex = 0;
					int subSpanStart = startIndex;					
					Vector<Vector<PSTree>> daughtersSplit = new Vector<Vector<PSTree>>(prole);
					int[] daughetersSpliSize = new int[prole];
					int d=0;
					for(; d<prole-1; d++) {
						int subSpanEnd = breakPositions[breakIndex++] + startIndex;
						PSTree dTree = new PSTree(subSpanStart, subSpanEnd);
						Vector<PSTree> dSplit = dTree.getAllSplits(); 
						daughtersSplit.add(dSplit);
						daughetersSpliSize[d] = dSplit.size();
						subSpanStart = subSpanEnd+1;						
					}
					PSTree dTree = new PSTree(subSpanStart, endIndex);
					Vector<PSTree> dSplit = dTree.getAllSplits();
					daughetersSpliSize[d] = dSplit.size();
					daughtersSplit.add(dSplit);					
					int[][] combinations = Utility.combinations(daughetersSpliSize);
					for(int[] c : combinations) {
						PSTree split = new PSTree(startIndex, endIndex);
						split.daughters = new PSTree[prole];
						for(int i=0; i<prole; i++) {
							int index = c[i];
							split.daughters[i] = daughtersSplit.get(i).get(index);
						}
						result.add(split);
					}
				}
			}						
			return result;
		}
		
		public String toString() {
			if (daughters==null) { 
				return Integer.toString(startIndex+1);				
			}
			StringBuilder sb = new StringBuilder();
			//sb.append("(X"); 			
			sb.append("(");
			int i = 0;
			int last = daughters.length;
			for(PSTree d : daughters) {
				sb.append(d.toString());
				if (++i < last)
					sb.append(" ");
			}
			sb.append(")");
			return sb.toString();
		}
		
		public String toQtreeRecursive() {
			if (daughters==null) { 
				return Integer.toString(startIndex+1);				
			}
			StringBuilder sb = new StringBuilder();
			sb.append("["); 			
			for(PSTree d : daughters) {
				sb.append(d.toQtreeRecursive() + " ");
			}
			sb.append("]");
			return sb.toString();
		}
		
		public String toQtree() {
			return "\\SRTree{" + toQtreeRecursive() + "}";
		}
		
		public int countConstituents() {
			int result = 1;
			if (daughters==null) 
				return result;
			for(PSTree t : daughters) {
				result += t.countConstituents();
			}
			return result;
		}

		public int[][] collectAllConstituentSpans() {
			int numberCounstituents = this.countConstituents();
			int[][] result = new int[numberCounstituents][];
			fillConstituents(result, new int[]{0});
			return result;
		}
		
		private void fillConstituents(int[][] result, int[] i) {
			result[i[0]++] = new int[]{startIndex, endIndex};
			if (daughters!=null) {
				for(PSTree t : daughters) {
					t.fillConstituents(result, i);
				}
			}			
		}
		
		
		
	}
	
	public static Vector<PSTree> getAllPS(int sentenceLength) {
		PSTree tree = new PSTree(sentenceLength);
		return tree.getAllSplits();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int sentenceLength = 4;
		//System.out.println(countAllDS(sentenceLength));
		PSTree tree = new PSTree(sentenceLength);
		Vector<PSTree> trees = tree.getAllSplits();
		System.out.println(trees.size());
		for(PSTree t : trees) {
			//System.out.println(t);
			System.out.println(t.toQtree());						
		}

	}


}
