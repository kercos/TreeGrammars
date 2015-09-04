package kernels.parallel.ted;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import util.IdentityArrayList;
import util.file.FileUtil;

public class CleanTED_Symbols {
	
	
	//static Pattern symbRE = Pattern.compile("\\&\\S+;"); //&apos;
	static Pattern symbRE = Pattern.compile("\\&[^\\s,\\&]+;"); //&apos;
	static String[] findSymb = new String[]{"&quot;","&amp;","&apos;", "&#91;", "&#93;", "&lt;", "&gt;"};
	static char[] replaceSymb = new char[]{  '"',     '&',    '\'',     '[',     ']',    '<',    '>'};
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
		while(scan.hasNextLine()) {
			String line = scan.nextLine();						
			pw.println(replaceSymbols(line));
		}
		pw.close();
		/*
		for(String s : matches) {
			System.out.println(s);
		}
		*/
	}
	
	static StringBuffer sb;
	static Matcher m;
	
	public static String replaceSymbols(String line) {
		sb = new StringBuffer();			
		m = symbRE.matcher(line);
		while (m.find()) {
			char replacement = subSymb.get(m.group());
			m.appendReplacement(sb, Character.toString(replacement));
		}
		m.appendTail(sb);
		return sb.toString();
	}
	
	public static void cleanIdentityArray(IdentityArrayList<String> mweList) {
		ListIterator<String> iter = mweList.listIterator();
		while(iter.hasNext()) {
			String mwe = iter.next();
			String cleanMwe = replaceSymbols(mwe);
			if (!cleanMwe.equals(mwe)) {
				iter.remove();
				iter.add(cleanMwe);
			}
		}		
	}
	
	public static void main(String[] args) {
		/*
		String workingDir = "/Users/fedja/Dropbox/ted_experiment/annotation/";
		File enTest2010 = new File(workingDir + "IWSLT14.TED.tst2010.en-it.tok.lc.en");
		File itTest2010 = new File(workingDir + "IWSLT14.TED.tst2010.en-it.tok.lc.it");
		File mtItTest2010 = new File(workingDir + "hypo_IWSLT14.TED.tst2010.en-it.tok.lc.it");
		replaceSymbols(enTest2010);
		replaceSymbols(itTest2010);
		replaceSymbols(mtItTest2010);
		*/
		
		String a = "fdsa fd &quot;&quot; dsafd asdf asdf as fzsd";
		System.out.println(replaceSymbols(a));
	}




}
