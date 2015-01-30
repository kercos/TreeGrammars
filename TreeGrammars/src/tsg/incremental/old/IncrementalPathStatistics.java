package tsg.incremental.old;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;
import java.util.TreeSet;

import tsg.Label;
import tsg.TSNodeLabel;
import util.Utility;
import util.file.FileUtil;

public class IncrementalPathStatistics {
	
	static final int maxLength = 20;
	static final int halfMaxLength = maxLength/2;
	
	HashMap<Path,int[]> pathTable; 
	HashMap<ArrayList<Label>,int[]> pathUpTable, pathDownTable;		
	int[] lengthTotalStats = new int[maxLength];	
	int[][] lengthUpDownStats = new int[halfMaxLength][halfMaxLength];
	int maxUp=-1;
	int maxDown=-1;
	

	public IncrementalPathStatistics(File tBfile) throws Exception {		
		pathTable = new HashMap<Path,int[]>();
		pathUpTable = new HashMap<ArrayList<Label>,int[]>();
		pathDownTable = new HashMap<ArrayList<Label>,int[]>();
		Scanner scan = FileUtil.getScanner(tBfile);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			TSNodeLabel t = new TSNodeLabel(line);			
			ArrayList<TSNodeLabel> lexItems = t.collectLexicalItems();
			if (lexItems.size()==1)
				continue;
			Iterator<TSNodeLabel> iter = lexItems.iterator();
			do {
				TSNodeLabel lex = iter.next();
				if (!iter.hasNext()) break;
				getLabelPathToNextWord(lex);
				Path path = getLabelPathToNextWord(lex);
				Utility.increaseInTableInt(pathTable, path);
				Utility.increaseInTableInt(pathUpTable, path.getPathUpWithVertex());
				Utility.increaseInTableInt(pathDownTable, path.getPathDownWithVertex());
				lengthTotalStats[path.totalLength()]++;
				int[] udl = path.upDownLengths();
				int up = udl[0];
				int down = udl[1];
				lengthUpDownStats[up][down]++;
				if (up>maxUp) maxUp=up;
				if (down>maxDown) maxDown=down;
			} while(true);			
		}
		printStatistics();
	}
	
	private void printStatistics() {
		printFreqFreqPath();
		System.out.println("\n\n\n");		
		printFreqFreqPathUp();
		System.out.println("\n\n\n");
		printFreqFreqPathDown();
		System.out.println("\n\n\n");
		printPathTotalLengthStats();
		System.out.println("\n\n\n");
		printUpDownPathStats();		
	}
	
	public void printFreqFreqPath() {
		HashMap<Integer,int[]> freqFreq = freqFreq(pathTable);
		System.out.println("Freq-freq statistics");
		System.out.println("Path_Freq\tFreq");
		printOrderTable(freqFreq);
	}
	
	public void printFreqFreqPathUp() {
		HashMap<Integer,int[]> freqFreq = freqFreq(pathUpTable);
		System.out.println("Freq-freq statistics");
		System.out.println("Path_Up_Freq\tFreq");
		printOrderTable(freqFreq);
	}
	
	public void printFreqFreqPathDown() {
		HashMap<Integer,int[]> freqFreq = freqFreq(pathDownTable);
		System.out.println("Freq-freq statistics");
		System.out.println("Path_Down_Freq\tFreq");
		printOrderTable(freqFreq);
	}
	
	public static void printOrderTable(HashMap<Integer,int[]> freqFreq) {
		TreeSet<Integer> keySet = new TreeSet<Integer>(freqFreq.keySet());
		for(Integer key : keySet) {
			System.out.println(key + "\t" + freqFreq.get(key)[0]);
		}
	}
	
	public void printPathTotalLengthStats() {
		System.out.println("Total Path length statistics");
		System.out.println("Length\tFreq");
		for(int i=0; i<maxLength; i++) {
			int freq = lengthTotalStats[i]; 
			if (freq==0)
				continue;
			System.out.println(i + "\t" + freq);
		}
	}
	
	public void printUpDownPathStats() {
		System.out.println("Up(->)/Down(|) length stats");
		//int[] maxUpDown = getMaxUpDown();
		//int maxUp = maxUpDown[0];
		//int maxDown = maxUpDown[1];
		for(int i=0; i<maxUp; i++)
			System.out.print("\t" + i);
		System.out.println();
		for(int j=0; j<maxDown; j++) {
			System.out.print(j + "\t");
			for(int i=0; i<maxUp; i++) {
				System.out.print(lengthUpDownStats[i][j] + "\t");				
			}
			System.out.println();
		}
	}
	


	public int[] getOrderedFreq() {
		int[] result = new int[pathTable.size()];
		int i = 0;
		for(int[] freq : pathTable.values()) {
			result[i++] = freq[0];
		}
		Arrays.sort(result);
		return result;
	}
	
	public static <T> HashMap<Integer,int[]> freqFreq(HashMap<T,int[]> table) {
		HashMap<Integer,int[]> result = new HashMap<Integer,int[]>();
		for(int[] freq : table.values()) {
			Utility.increaseInTableInt(result, new Integer(freq[0]));
		}
		return result;
	}
	
	private Path getLabelPathToNextWord(TSNodeLabel l) {
		ArrayList<Label> pathUp = new ArrayList<Label>();		
		TSNodeLabel p = l.parent;
		do {
			pathUp.add(p.label);
			p = p.parent;			
		} while(p.prole()==1);		 
		
		Label vertex = p.label;
		
		ArrayList<Label> pathDown = new ArrayList<Label>();
		TSNodeLabel d = p.daughters[1];
		do {
			pathDown.add(d.label);
			d = d.daughters[0];
		} while(!d.isLexical);
		return new Path(pathUp, vertex, pathDown);
	}
	
	protected class Path {
		ArrayList<Label> pathUp, pathDown;
		Label vertex;
		
		public Path(ArrayList<Label> pathUp, Label vertex, ArrayList<Label> pathDown) {
			this.pathUp = pathUp;
			this.vertex = vertex;
			this.pathDown = pathDown;
		}
		
		public ArrayList<Label> getPathUpWithVertex() {
			ArrayList<Label> result = new ArrayList<Label>(pathUp);
			result.add(vertex);
			return result;
		}
		
		public ArrayList<Label> getPathDownWithVertex() {
			ArrayList<Label> result = new ArrayList<Label>(pathDown);
			result.add(0, vertex);
			return result;
		}
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			Iterator<Label> iter = pathUp.iterator();
			while(iter.hasNext()) {
				sb.append(iter.next());
				if (iter.hasNext())
					sb.append("-");
			}
			sb.append(" | ");
			sb.append(vertex);
			sb.append(" | ");
			iter = pathDown.iterator();
			while(iter.hasNext()) {
				sb.append(iter.next());
				if (iter.hasNext())
					sb.append("-");
			}
			return sb.toString();
		}
		
		public int totalLength() {
			return pathUp.size() + pathDown.size();
		}
		
		public int[] upDownLengths() {
			return new int[]{pathUp.size(),pathDown.size()};
		}
		
		public boolean equals(Object o) {
			if (o==this) return true;		
			if (o instanceof Path) {
				Path p = (Path)o;
				return 
						this.vertex.equals(p.vertex) && 
						this.pathUp.equals(p.pathUp) &&
						this.pathDown.equals(p.pathDown);
			}
			return false;				
		}
			

		@Override	
		public int hashCode() {	
			int result = 31 + vertex.hashCode();
			result = 31 * result + pathUp.hashCode();
			result = 31 * result + pathDown.hashCode();
			return result;
		}
	}

	public static void main(String[] args) throws Exception {
		//File TBfile = new File(args[0]);
		File TBfile = new File("wsj-22.mrg");		
		new IncrementalPathStatistics(TBfile);
	}
	
}
