package kernels.parallel;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import util.file.FileUtil;

public class Annotate_MWE_Ted {
	
	
	static Pattern symbRE = Pattern.compile("\\&\\S+;"); //&apos;	
	static String[] findSymb = new String[]{"&quot;","&amp;","&apos;", "&#91;", "&#93;"};
	static char[] replaceSymb = new char[]{  '"',     '&',    '\'',     '[',     ']'};
	static HashMap<String, Character> subSymb = new HashMap<String, Character>();
	
	static {
		for(int i=0; i<findSymb.length; i++) {
			subSymb.put(findSymb[i],replaceSymb[i]);
		}
	}
	
	public static void replaceSymbols(File inputFile) {
		File outputFile = FileUtil.addExtension(inputFile, "txt");
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		Scanner scan = FileUtil.getScanner(inputFile);
		//HashSet<String> matches = new HashSet<String>();
		Matcher m = null;
		StringBuffer sb = null;
		while(scan.hasNextLine()) {
			sb = new StringBuffer();
			String line = scan.nextLine();			
			m = symbRE.matcher(line);
			while (m.find()) {
				char replacement = subSymb.get(m.group());
				m.appendReplacement(sb, Character.toString(replacement));
			}
			m.appendTail(sb);
			pw.println(sb.toString());
		}
		pw.close();
		/*
		for(String s : matches) {
			System.out.println(s);
		}
		*/
	}

	public static void main(String[] args) {
		String workingDir = "/Users/fedja/Dropbox/ted_experiment/annotation/";
		File enTest2010 = new File(workingDir + "IWSLT14.TED.tst2010.en-it.tok.lc.en");
		File itTest2010 = new File(workingDir + "IWSLT14.TED.tst2010.en-it.tok.lc.it");
		File mtItTest2010 = new File(workingDir + "hypo_IWSLT14.TED.tst2010.en-it.tok.lc.it");
		replaceSymbols(enTest2010);
		replaceSymbols(itTest2010);
		replaceSymbols(mtItTest2010);
		
	}

}
