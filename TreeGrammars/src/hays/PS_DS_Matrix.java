package hays;

import hays.ComputAllDS.DSTree;
import hays.ComputeAllPS.PSTree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;

import util.Pair;

public class PS_DS_Matrix {

	private static void makeCompatibilityMatrix(Vector<PSTree> pStrees,
			ArrayList<DSTree> dStrees) {
		
		for(PSTree psT : pStrees) {
			System.out.print("\t" + psT.toString());
		}
		System.out.println();
		
		for(DSTree dsT : dStrees) {
			System.out.print(dsT.toString());
			for(PSTree psT : pStrees) {
				System.out.print("\t");
				if (compatible(psT,dsT))
					System.out.print("1");
			}
			System.out.println();
		}
		
	}
	
	private static void makeDotDiagram(Vector<PSTree> pStrees,
			ArrayList<DSTree> dStrees) {
		
		StringBuilder sb = new StringBuilder();
		sb.append("graph ER {" + "\n");
		
		sb.append("\t" + "node [shape=box];");
		for(DSTree dsT : dStrees) {
			sb.append(" {node [label=\"" + dsT + "\"] " + "\"ds_" + dsT + "\";}");			
		}
		sb.append("\n");
		
		sb.append("\t" + "node [shape=ellipse];");
		for(PSTree psT : pStrees) {
			sb.append(" {node [label=\"" + psT + "\"] " + "\"ps_" + psT + "\";}");
		}
		sb.append("\n");
		sb.append("\n");
		
		for(DSTree dsT : dStrees) {			
			for(PSTree psT : pStrees) {				
				if (compatible(psT,dsT))
					sb.append("\t" + " \"ds_" + dsT + "\"" + " -- " + " \"ps_" + psT + "\"" + "\n");
			}			
		}
		
		sb.append("\t" + "fontsize=20;" + "\n");
		sb.append("}");
		System.out.println(sb);
	}
	
	private static boolean compatible(PSTree psT, DSTree dsT) {
		int[][] constSpans = psT.collectAllConstituentSpans();
		int[][] depSpans = dsT.collectAllNodesSpans();
		ArrayList<Pair<Integer>> dsPairs = dsT.collectPairs();
		for(int[] d : depSpans) {
			if (d[0]==d[1])
				continue;
			if (!contains(constSpans,d))
				return false;
		}
		for(int[] c : constSpans) {
			if (c[0]==c[1])
				continue;
			if (!isConnectedSpan(c,dsPairs))
				return false;
		}
		return true;
	}

	private static boolean isConnectedSpan(int[] c,
			ArrayList<Pair<Integer>> dsPairs) {		
		int pairInSpan = 0;
		for(Pair<Integer> p : dsPairs) {
			int first = p.getFirst();
			int second = p.getSecond();
			if (first>=c[0] && first<=c[1] && second>=c[0] && second<=c[1])
				pairInSpan++;
		}
		int span = c[1]-c[0]+1;
		return pairInSpan==span-1;
	}

	private static boolean contains(int[][] constSpans, int[] d) {
		for(int[] c : constSpans) {
			if (Arrays.equals(c, d))
				return true;
		}
		return false;
	}
	
	public static void test() {
		PSTree pstA = new PSTree(0,3);
		PSTree pstB = new PSTree(0,2);
		PSTree pstC = new PSTree(1,2);
		
		pstA.daughters = new PSTree[]{
				pstB, new PSTree(3,3)	
			};
		pstB.daughters = new PSTree[]{
				new PSTree(0,0), pstC 	
			};
		pstC.daughters = new PSTree[]{
				new PSTree(1,1), new PSTree(2,2) 	
			};
		
		System.out.println(pstA);
		
		DSTree dsT0 = new DSTree(0);
		DSTree dsT1 = new DSTree(1);
		DSTree dsT2 = new DSTree(2);
		DSTree dsT3 = new DSTree(3);
		dsT0.addDep(dsT1);
		dsT1.addDep(dsT2);
		dsT1.addDep(dsT3);
		
		System.out.println(dsT0);
		
		System.out.println(compatible(pstA,dsT0));
	}

	public static void main(String[] args) {
		
		int sentenceLength = 4;
		ArrayList<DSTree> DStrees = ComputAllDS.getAllDS(sentenceLength);
		Vector<PSTree> PStrees = ComputeAllPS.getAllPS(sentenceLength);
		//makeCompatibilityMatrix(PStrees,DStrees);
		makeDotDiagram(PStrees,DStrees);
		
		/*
		PSTree pst = PStrees.get(4);
		DSTree dst = DStrees.get(2);
		
		System.out.println(pst);
		System.out.println(dst);
		System.out.println(compatible(pst,dst));
		*/
		//test();
	}

	
	
}
