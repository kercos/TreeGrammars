package tsg.mwe;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Scanner;
import java.util.TreeSet;

import kernels.algo.StringsAlignment;
import util.Utility;

public class CorpusCornetto {

	static final String path = "/Volumes/HardDisk/Scratch/CORPORA/MWE_Datasets/Cornetto/Cornetto_2.0/DATA/";
	static final File cornettoXmlFile = new File(path + "cdb2.0.lu.xml");
	static final File cornettoFormSpellingMweFile = new File(path + "cdb2.0.lu.form-spelling-mwe.txt");
	static final File cornettoFormSpellingSyCompFile = new File(path + "cdb2.0.lu.form-spelling-syComp.txt");
	
	public static void extractExpressions() throws FileNotFoundException {
		Scanner scan = new Scanner(cornettoXmlFile);
		PrintWriter pw_mwe = new PrintWriter(cornettoFormSpellingMweFile);
		PrintWriter pw_syComp = new PrintWriter(cornettoFormSpellingSyCompFile);
		int countAll=0, countMWE=0;
		HashMap<Integer, int[]> lengthStats = new HashMap<Integer, int[]>();
		while(scan.hasNextLine()) {
			String line = scan.nextLine().trim();
			if (line.startsWith("<form ")) {
				countAll++;
				String formSpelling = getField(line, "form-spelling=\"", "\"");
				int l = formSpelling.split("\\s").length;
				pw_syComp.println(formSpelling);
				if (l>1) {
					countMWE++;
					pw_mwe.println(formSpelling);
				}
				Utility.increaseInHashMap(lengthStats, l);
			}
			if (line.startsWith("<sy-comp>")) {
				String syComp = getField(line, "<sy-comp>", "</sy-comp>");
				if (syComp!=null)
					pw_syComp.println("\t" + syComp);
			}
		}
		pw_mwe.close();
		pw_syComp.close();
		System.out.println("Total forms: " + countAll);
		System.out.println("Total MWE: " + countMWE);
		System.out.println("Length stats: ");
		TreeSet<Integer> lengthSet = new TreeSet<Integer>(lengthStats.keySet());
		for(int l : lengthSet) {
			System.out.println(l + "\t" + lengthStats.get(l)[0]);
		}
	}
	
	public static String getField(String line, String fieldName, String endFieldMarker) {
		int expStart = line.indexOf(fieldName)+fieldName.length();
		int expEnd = line.indexOf(endFieldMarker, expStart);
		if (expEnd==-1)
			return null;
		return line.substring(expStart, expEnd);
		
	}
	
	public static void main(String[] args) throws FileNotFoundException {
		extractExpressions();
	}
	
}
