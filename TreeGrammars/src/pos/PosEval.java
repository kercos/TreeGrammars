package pos;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class PosEval {
	
	int total;
	int correct;

	public PosEval(File goldFile, File testFile) throws FileNotFoundException {
		Scanner scanGold = new Scanner(goldFile);
		Scanner scanTest = new Scanner(testFile);
		while(scanGold.hasNextLine()) {
			String goldLine = scanGold.nextLine();
			String testLine = scanTest.nextLine();
			score(goldLine, testLine);
		}
		System.out.println("Total:\t" + total);
		System.out.println("Correct:\t" + correct);
		System.out.println("Accuracy:\t" + ((double)correct)/total);
	}
	
	private void score(String goldLine, String testLine) {
		String[] goldPos = getPos(goldLine);
		String[] testPos = getPos(testLine);
		if (goldPos.length != testPos.length) {
			System.err.println("Sentence in two files differs in length:");
			System.err.println("goldLine: " + goldLine);
			System.err.println("testLine: " + testLine);
			return;
		}
		total += goldPos.length;
		for(int i=0; i<goldPos.length; i++) {
			if (goldPos[i].equals(testPos[i]))
				correct++;
		}
		
	}

	private static String[] getPos(String line) {
		String[] wordPos = line.split("\\s");
		int length = wordPos.length;
		String[] result = new String[length];
		for(int i=0; i<length; i++) {
			String w = wordPos[i];
			result[i] = w.substring(w.indexOf('_')+1);
		}
		return result;
	}

	public static void main(String args[]) throws FileNotFoundException {
		new PosEval(new File(args[0]),new File(args[1]));
	}
}
