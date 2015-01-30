package tsg.mwe;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Scanner;
import java.util.TreeSet;

import util.StringsAlignment;
import util.Utility;

public class CorpusDuelme2 {

	static final File duelmeXmlFile = new File("/Volumes/HardDisk/Scratch/CORPORA/MWE_Datasets/Duelme2/Data/DuELME_UTF8.xml");
	static final File duelmeExpressionsFile = new File("/Volumes/HardDisk/Scratch/CORPORA/MWE_Datasets/Duelme2/Data/DuELME_UTF8_expressions.txt");
	
	public static void extractExpressions() throws FileNotFoundException {
		Scanner scan = new Scanner(duelmeXmlFile);
		PrintWriter pw = new PrintWriter(duelmeExpressionsFile);
		int countAll=0, countDisc = 0;
		HashMap<Integer, int[]> mweLengthTable = new HashMap<Integer, int[]>();
		while(scan.hasNextLine()) {
			String line = scan.nextLine().trim();
			if (line.startsWith("<LexicalEntry")) {
				countAll++;
				String expression = getField(line, "expression=\"");
				String nextLine = scan.nextLine().trim();
				String example = getField(nextLine, "sentence=\"");
				StringsAlignment SA = new StringsAlignment(expression.split("\\s"),example.split("\\s"));
				int l = expression.split("\\s").length;
				Utility.increaseInHashMap(mweLengthTable, l);
				boolean contiguous = SA.isBestAlignementContiguous();
				if (!contiguous)
					countDisc++;
				String contStr = contiguous ? "C" : "D";
				pw.println(expression + "\t" + example + "\t" + contStr);
			}
		}
		pw.close();
		System.out.println("Total MWE: " + countAll);
		System.out.println("of which discontigous in examples: " + countDisc);
		System.out.println("MWE length stats: ");
		TreeSet<Integer> lengthSet = new TreeSet<Integer>(mweLengthTable.keySet());
		for(int l : lengthSet) {
			System.out.println(l + "\t" + mweLengthTable.get(l)[0]);
		}
	}
	
	public static String getField(String line, String fieldName) {
		int expStart = line.indexOf(fieldName)+fieldName.length();
		int expEnd = line.indexOf('"', expStart);
		return line.substring(expStart, expEnd);
		
	}
	
	public static void main(String[] args) throws FileNotFoundException {
		extractExpressions();
	}
	
}
