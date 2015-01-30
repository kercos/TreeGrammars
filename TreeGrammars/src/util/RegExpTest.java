package util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegExpTest {

	
	public static void main(String[] args) {
		//String re = "([\"])(?:(?=(\\?))\2.)*?\1";
		String re = "\"[^\"]+\"";
		String test = "fdsfd  fds \"fs\" fds f sfs d sfdfs \"fdsad\" non \"gfsdgfd ";
		Pattern pattern = Pattern.compile(re);
		Matcher matcher = pattern.matcher(test);
		while (matcher.find()) {
			System.out.print("Start index: " + matcher.start());
			System.out.print(" End index: " + matcher.end() + " ");
			System.out.println(matcher.group());
		}
	}
}
