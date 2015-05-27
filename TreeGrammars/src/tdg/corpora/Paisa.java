package tdg.corpora;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.Vector;
import java.util.zip.GZIPInputStream;


public class Paisa {
	
	static File paisaInputFile = new File("/Volumes/HardDisk/Scratch/CORPORA/PAISA/paisa.annotated.CoNLL.utf8.gz");
	
	public static void readPaisa() throws IOException {
		InputStream fileStream = new FileInputStream(paisaInputFile);
		InputStream gzipStream = new GZIPInputStream(fileStream);
		Reader decoder = new InputStreamReader(gzipStream, "UTF-8");
		BufferedReader buffered = new BufferedReader(decoder);

		String line = null;
		int sentences = 0;
		while((line = buffered.readLine()) != null) {
			if (line.isEmpty())
				sentences++;
		}
		buffered.close();
		System.out.println("Read sentences: " + sentences);
	}
	
	public static void getFarePaterns() throws IOException {
		InputStream fileStream = new FileInputStream(paisaInputFile);
		InputStream gzipStream = new GZIPInputStream(fileStream);
		Reader decoder = new InputStreamReader(gzipStream, "UTF-8");
		BufferedReader buffered = new BufferedReader(decoder);

		TreeSet<String> nouns = new TreeSet<String>(); 
		
		Vector<String[]> nextSentence = null;
		int pattenrsCount = 0;
		do {
			nextSentence = getNextSentenceFields(buffered);
		} while(!nextSentence.isEmpty());
		do {
			for(int s=0; s<nextSentence.size()-2; s++) {				
				String[][] triplet = new String[3][];
				triplet[0] = nextSentence.get(s);
				triplet[1] = nextSentence.get(s+1);
				triplet[2] = nextSentence.get(s+2);
				if (triplet[0][2].equals("fare") && triplet[1][3].equals("R") && triplet[2][3].equals("S")) {
					//System.out.println(triplet[0][1] + " " + triplet[1][1] + " " + triplet[2][1]);
					pattenrsCount++;
					nouns.add(triplet[2][2]);
				}
				
			}
			nextSentence = getNextSentenceFields(buffered);
		} while(nextSentence!=null);
		buffered.close();	
		System.out.println("Patterns found: " + pattenrsCount);
		System.out.println("Patterns set size: " + nouns.size());
		System.out.println(nouns);
	}
	
	private static Vector<String[]> getNextSentenceFields(BufferedReader buffered) throws IOException {
		String line = null;
		Vector<String[]> result = new Vector<String[]>();
		while((line = buffered.readLine()) != null) {
			if (line.isEmpty())
				break;
			String[] split = line.split("\t");
			if (split.length!=8)
				break;
			result.add(split);
		}
		if (line==null)
			return null;
		return result;
	}

	public static void main(String[] args) throws IOException {
		getFarePaterns();		
	}
}
