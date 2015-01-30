package hays;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.TreeSet;
import java.util.Vector;

import util.Pair;
import util.Utility;

public class ComputAllDS {
	
	public static class DSTree implements Comparable<DSTree> {
		
		TreeSet<DSTree> leftDep, rightDep;
		Integer index;
		
		public DSTree(int index) {
			this.index = index;
			leftDep = new TreeSet<DSTree>();
			rightDep = new TreeSet<DSTree>();
		}
		
		public boolean merge(DSTree d) {
			if (index.equals(d.index)) {
				leftDep.addAll(d.leftDep);
				rightDep.addAll(d.rightDep);
			}
			return false;
		}
		
		public DSTree clone() {
			DSTree c = new DSTree(this.index);
			for(DSTree l : this.leftDep) {
				c.leftDep.add(l.clone());
			}
			for(DSTree r : this.rightDep) {
				c.rightDep.add(r.clone());
			}
			return c;
		}
		
		public DSTree(int index, TreeSet<DSTree> leftDep, TreeSet<DSTree> rightDep) {
			this.index=index;
			this.leftDep = leftDep;
			this.rightDep = rightDep;
		}

		public void addDep(int i) {
			DSTree newDep = new DSTree(i);
			if (i<index) leftDep.add(newDep);
			else rightDep.add(newDep);
		}
		
		public void addDep(DSTree newDep) {
			int newIndex = newDep.index; 
			if (newIndex<index) { 
				if (!leftDep.add(newDep)) {
					for(DSTree d : leftDep) {
						if (d.index==newIndex)
							d.merge(newDep);
					}
				}
			}
			else {
				if (!rightDep.add(newDep)) {
					for(DSTree d : rightDep) {
						if (d.index==newIndex)
							d.merge(newDep);
					}
				}
			}
		}
		
		@Override
		public int compareTo(DSTree o) {
			return index.compareTo(o.index);
		}
		
		public String toString() {
			if (leftDep.isEmpty() && rightDep.isEmpty()) 
				return ""+(index+1);
			StringBuilder sb = new StringBuilder();
			sb.append("(" + (index+1));			
			for(DSTree d : leftDep) {
				sb.append(" " + d.toString());
			}
			for(DSTree d : rightDep) {
				sb.append(" " + d.toString());
			}
			sb.append(")");
			return sb.toString();
		}
		
		int getMaxIndex() {
			int max = index;
			for(DSTree d: leftDep) {
				int maxIndexD = d.getMaxIndex();
				if (maxIndexD>max)
					max = maxIndexD;
			}
			for(DSTree d: rightDep) {
				int maxIndexD = d.getMaxIndex();
				if (maxIndexD>max)
					max = maxIndexD;
			}
			return max;
		}
		
		int getMaxDepth() {
			int max = 0;
			for(DSTree d: leftDep) {
				int depth = d.getMaxDepth();
				if (depth>max) max=depth;
			}
			for(DSTree d: rightDep) {
				int depth = d.getMaxDepth();
				if (depth>max) max=depth;
			}
			return ++max;
		}
		
		public ArrayList<Pair<Integer>> collectPairs() {
			ArrayList<Pair<Integer>> result = new ArrayList<Pair<Integer>>();
			this.fillPairs(result);
			return result;
		}
		
		public void fillPairs(ArrayList<Pair<Integer>> pairs) {
			for(DSTree d: leftDep) {
				pairs.add(new Pair<Integer>(this.index, d.index));
				d.fillPairs(pairs);
			}
			for(DSTree d: rightDep) {
				pairs.add(new Pair<Integer>(this.index, d.index));
				d.fillPairs(pairs);
			}
		}
		
		public String toMatrix(int rowSep, int colSep) {
			BitSet[] levels = buildLevels();	
			int maxIndex = getMaxIndex();
			StringBuilder sb = new StringBuilder();
			sb.append("\\begin{tikzpicture}" + "\n" +
					"\\matrix" + "\n" +
					"[matrix of math nodes," + "\n" +
					"row sep={" + rowSep + "pt,between origins}," + "\n" +
					"column sep={" + colSep + "pt,between origins}]" + "\n" +
					"{" + "\n");
			for(BitSet level : levels) {
				for(int i=0; i<=maxIndex; i++) {
					int iPlus = i+1;
					if (level.get(i))
						sb.append(" |(" + i + ")| " + iPlus + " ");
					if (i<maxIndex)
						sb.append(" & ");						
				}
				sb.append("\\\\" + "\n");				
			}
			//"|(A)| 1 &&&\\\\" + "\n" +
			//"& |(B)| 2 & |(C)| 3 &|(D)| 4\\\\" + "\n" +
			sb.append("};" + "\n");
			
			ArrayList<Pair<Integer>> pairs = collectPairs();
			for(Pair<Integer> p : pairs) {
				sb.append("\\draw[thick] (" + p.getFirst() + ".south) to (" + p.getSecond() + ".north) {};" + "\n");
			}
			
			//"\\draw[thick] (A.south) to (B.north) {};" + "\n" +
			//"\\draw[thick] (A.south) to (C.north) {};" + "\n" +
			//"\\draw[thick] (A.south) to (D.north) {};" + "\n" +
			sb.append("\\end{tikzpicture}");
			return sb.toString();
		}

		private BitSet[] buildLevels() {
			int maxDepth = getMaxDepth();
			BitSet[] result = new BitSet[maxDepth];
			ArrayList<DSTree> nodesLevel = new ArrayList<DSTree>();
			nodesLevel.add(this);
			for(int hight=0; hight<maxDepth; hight++) {
				BitSet bs = new BitSet();
				ArrayList<DSTree> newLevel = new ArrayList<DSTree>();
				for(DSTree d : nodesLevel) {
					bs.set(d.index);
					newLevel.addAll(d.leftDep);
					newLevel.addAll(d.rightDep);
				}
				result[hight] = bs;
				nodesLevel = newLevel;
			}
			return result;
		}
		
		public int countWords() {
			int result = 1;
			for(DSTree d: leftDep) {
				result += d.countWords();
			}
			for(DSTree d: rightDep) {
				result += d.countWords();
			}
			return result;
		}

		public int[][] collectAllNodesSpans() {
			int size = countWords();
			int[][] result = new int[size][];
			fillNodesSpans(result, new int[]{0});
			return result;
		}

		private void fillNodesSpans(int[][] result, int[] i) {
			result[i[0]++] = new int[]{leftmostDep(), rightmostDep()}; 
			for(DSTree d: leftDep) {
				d.fillNodesSpans(result,i);
			}
			for(DSTree d: rightDep) {
				d.fillNodesSpans(result,i);
			}
		}

		private int leftmostDep() {
			if (leftDep.isEmpty()) 
				return this.index;
			return (leftDep.first().leftmostDep());			
		}

		private int rightmostDep() {
			if (rightDep.isEmpty()) 
				return this.index;
			return (rightDep.last().rightmostDep());			
		}

	}
	
	static int countAllDS(int sentenceLength) {
		int result = 0;
		int lastWordIndex = sentenceLength-1;
		for(int root=0; root<sentenceLength; root++) {			
			if (root!=0) {
				result += countSubDS(root,0,root-1);
			}			
			if (root!=lastWordIndex) {
				result += countSubDS(root,root+1,lastWordIndex);				
			}
		}
		return result;
	}
	
	static int countSubDS(int rootIndex, int spanLeftIndex, int spanRightIndex) {
		int spanSize = spanRightIndex - spanLeftIndex + 1;
		if (spanSize==1) return 1;
		int result = 0;
		for(int depsSize=1; depsSize<=spanSize; depsSize++) {
			int[][] depsGroups = Utility.n_air(spanSize, depsSize);
			int lastChild = depsSize-1;
			for(int[] deps : depsGroups) {
				int combinations = 1;
				
				int leftHoleSize = deps[0];
				int firstDepIndex = deps[0] + spanLeftIndex;
				if (leftHoleSize>0) {					
					combinations *= countSubDS(firstDepIndex, spanLeftIndex, firstDepIndex-1);
				}												
				
				int rightHoleSize = spanSize - deps[lastChild] -1;
				int lastDepIndex = deps[lastChild] + spanLeftIndex;
				if (rightHoleSize>0) {					
					combinations *= countSubDS(lastDepIndex, lastDepIndex+1, spanRightIndex);
				}
					
				if (depsSize>1) {
					int depBeforeHoleIndex = firstDepIndex;
					for(int d=1; d<depsSize; d++) {
						int depAfterHoleIndex = deps[d] + spanLeftIndex;;						
						int innerHoleSize = depAfterHoleIndex - depBeforeHoleIndex - 1;
						int firstDepInHoleIndex = depBeforeHoleIndex+1;
						int lastDepInHoleIndex = depBeforeHoleIndex+innerHoleSize;
						for(int firstSplitSize = 0; firstSplitSize<=innerHoleSize; firstSplitSize++) {
							if (firstSplitSize>0) { 
								int leftSplitEndIndex = firstDepInHoleIndex+firstSplitSize;
								combinations *= countSubDS(depBeforeHoleIndex, firstDepInHoleIndex, leftSplitEndIndex);								
							}
							if (firstSplitSize<innerHoleSize) {
								int rightSplitStartIndex = depAfterHoleIndex + firstSplitSize;
								combinations *= countSubDS(depAfterHoleIndex, rightSplitStartIndex, lastDepInHoleIndex);
							}							
							
						}
						depBeforeHoleIndex = depAfterHoleIndex;
					}										
				}
				 
				
				result += combinations;
			}
		}
		return result;
	}
	
	static ArrayList<DSTree> getAllDS(int sentenceLength) {
		ArrayList<DSTree> result = new ArrayList<DSTree>();
		int lastWordIndex = sentenceLength-1;
		for(int root=0; root<sentenceLength; root++) {
			ArrayList<DSTree> leftSubTrees = new ArrayList<DSTree>(); 
			if (root!=0) {
				leftSubTrees = getAllSubDS(root,0,root-1);
			}
			else {
				leftSubTrees.add(null);
			}
			ArrayList<DSTree> rightSubTrees = new ArrayList<DSTree>();
			if (root!=lastWordIndex) {
				rightSubTrees = getAllSubDS(root,root+1,lastWordIndex);				
			}
			else {
				rightSubTrees.add(null);
			}
			
			for(DSTree l : leftSubTrees) {				
				for(DSTree r : rightSubTrees) {
					if (l==null) {
						result.add(r);
					}
					else if (r==null) {
						result.add(l);
					}
					else {
						DSTree newL = l.clone();
						newL.merge(r);
						result.add(newL);
						//result.add(r);
					}
				}
			}
		}
		return result;
	}
	

	static ArrayList<DSTree> getAllSubDS(int rootIndex, int spanLeftIndex, int spanRightIndex) {
		
		ArrayList<DSTree> result = new ArrayList<DSTree>();		
		int spanSize = spanRightIndex - spanLeftIndex + 1;		
		if (spanSize==1) {
			DSTree rootTree = new DSTree(rootIndex);
			rootTree.addDep(spanLeftIndex);
			result.add(rootTree);
			return result;
		}		
		for(int depsSize=1; depsSize<=spanSize; depsSize++) {
			int[][] depsGroups = Utility.n_air(spanSize, depsSize);
			int lastChild = depsSize-1;
			for(int[] deps : depsGroups) {
				
				ArrayList<DSTree> leftSubTree = new ArrayList<DSTree>(); 				
				int leftHoleSize = deps[0];
				int firstDepIndex = deps[0] + spanLeftIndex;
				if (leftHoleSize>0) {					
					leftSubTree = getAllSubDS(firstDepIndex, spanLeftIndex, firstDepIndex-1);
				}
				else {
					leftSubTree.add(null);
				}
				
				ArrayList<DSTree> rightSubTree = new ArrayList<DSTree>();
				int rightHoleSize = spanSize - deps[lastChild] -1;
				int lastDepIndex = deps[lastChild] + spanLeftIndex;
				if (rightHoleSize>0) {					
					rightSubTree = getAllSubDS(lastDepIndex, lastDepIndex+1, spanRightIndex);
				}
				else {
					rightSubTree.add(null);
				}
					
				Vector<Vector<ArrayList<DSTree>>> innerSubTrees = new Vector<Vector<ArrayList<DSTree>>>();
				
				if (depsSize>1) {
					int depBeforeHoleIndex = firstDepIndex;
					for(int d=1; d<depsSize; d++) {
						int depAfterHoleIndex = deps[d] + spanLeftIndex;;						
						int innerHoleSize = depAfterHoleIndex - depBeforeHoleIndex - 1;
						if (innerHoleSize>0) {
							int firstDepInHoleIndex = depBeforeHoleIndex+1;
							int lastDepInHoleIndex = depBeforeHoleIndex+innerHoleSize;												
							for(int firstSplitSize = 0; firstSplitSize<=innerHoleSize; firstSplitSize++) {
								Vector<ArrayList<DSTree>> holeInnerSubTrees = new Vector<ArrayList<DSTree>>();
								if (firstSplitSize>0) { 
									int leftSplitEndIndex = firstDepInHoleIndex + firstSplitSize - 1;
									holeInnerSubTrees.add(
											getAllSubDS(depBeforeHoleIndex, firstDepInHoleIndex, leftSplitEndIndex));								
								}
								if (firstSplitSize<innerHoleSize) {
									int rightSplitStartIndex = firstDepInHoleIndex + firstSplitSize;
									holeInnerSubTrees.add(
										getAllSubDS(depAfterHoleIndex, rightSplitStartIndex, lastDepInHoleIndex));
								}							
								innerSubTrees.add(holeInnerSubTrees);
							}
						}												
						depBeforeHoleIndex = depAfterHoleIndex;
					}										
				}
				if (innerSubTrees.isEmpty()) {
					Vector<ArrayList<DSTree>> holeInnerSubTrees = new Vector<ArrayList<DSTree>>();					
					innerSubTrees.add(holeInnerSubTrees);
				}
				
				for(DSTree l : leftSubTree) {
					for(DSTree r : rightSubTree) {												
						for(Vector<ArrayList<DSTree>> v : innerSubTrees) {
							DSTree rootTree = new DSTree(rootIndex);
							for(int d : deps) {
								int dIndex = d + spanLeftIndex;
								rootTree.addDep(dIndex);
							}
							if (l!=null) {
								rootTree.addDep(l);
							}
							if (r!=null) {
								rootTree.addDep(r);
							}
							if (v.isEmpty()) {								
								result.add(rootTree);
							}
							else if (v.size()==1) {
								ArrayList<DSTree> splitOne = v.get(0);
								if (splitOne!=null) {
									for(DSTree split : splitOne) {
										DSTree newRootTree = rootTree.clone();
										newRootTree.addDep(split);
										result.add(newRootTree);
									}
								}								
							}
							else {
								ArrayList<DSTree> splitOne = v.get(0);
								ArrayList<DSTree> splitTwo = v.get(1);
								for(DSTree split1 : splitOne) {
									for(DSTree split2 : splitTwo) {
										DSTree newRootTree = rootTree.clone();
										newRootTree.addDep(split1);
										newRootTree.addDep(split2);
										result.add(newRootTree);
									}									
								}								
							}
							
						}						
					}										
					
				}							
								
			}
		}
		return result;
	}
	
	static String getParetheses(int[] words) {
		StringBuilder s= new StringBuilder();
		s.append("(");
		for(int i : words) {
			s.append(i + " ");
		}
		s.append(") ");
		return s.toString();
	}


	static public void main(String[] args) {
		int sentenceLength = 4;
		//System.out.println(countAllDS(sentenceLength));
		ArrayList<DSTree> trees = getAllDS(sentenceLength);
		//System.out.println(trees.size());
		for(DSTree t : trees) {
			//System.out.println(t);
			System.out.println(t.toMatrix(22, 8));			
			System.out.println();
		}
		//System.out.println();
		//System.out.println(trees.get(2).toMatrix(22, 8));
	}
	
}
