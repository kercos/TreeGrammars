package tsg.incremental;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Vector;

import settings.Parameters;
import tsg.Label;
import tsg.TSNodeLabel;
import tsg.parseEval.EvalB;
import tsg.parseEval.EvalC;
import util.Utility;
import util.file.FileUtil;

public class EvaluateIncrementalParsers {

	// static String[] quotesLex = new String[]{"'", "''", "`", "``"};
	static String[] quotesPos = new String[] { "''", "``" };
	static int treeLengthLimit = -1;

	// already sorted

	private static void cleanRoarkOutput(File parsedOutput, File goldFile, 
			File parsedOutputCleaned, File goldFiltered) throws Exception {

		Scanner scanParsed = FileUtil.getScanner(parsedOutput);
		Scanner scanGold = FileUtil.getScanner(goldFile);
		PrintWriter pwParsedCleaned = FileUtil.getPrintWriter(parsedOutputCleaned);
		boolean writeGoldFiltered = goldFiltered!=null;
		PrintWriter pwGoldFiltered = writeGoldFiltered ? 
				FileUtil.getPrintWriter(goldFiltered) : null;
		while (scanParsed.hasNextLine()) {
			String lineGold = scanGold.nextLine();
			String lineParsed = scanParsed.nextLine();
			TSNodeLabel goldTree = new TSNodeLabel(lineGold);
			if (treeLengthLimit>0 && goldTree.countLexicalNodes()>treeLengthLimit)
				continue;
			int parenthesis = lineParsed.indexOf('(');
			lineParsed = lineParsed.substring(parenthesis);
			pwParsedCleaned.println(lineParsed);
			if (writeGoldFiltered)
				pwGoldFiltered.println(lineGold);
		}
		pwParsedCleaned.close();
		if (writeGoldFiltered)
			pwGoldFiltered.close();

	}
	
	private static void cleanRoarkOutput(File parsedOutput, 
			File parsedOutputCleaned) throws Exception {

		Scanner scanParsed = FileUtil.getScanner(parsedOutput);
		PrintWriter pwParsedCleaned = FileUtil.getPrintWriter(parsedOutputCleaned);
		while (scanParsed.hasNextLine()) {
			String lineParsed = scanParsed.nextLine();
			//TSNodeLabel goldTree = new TSNodeLabel(lineGold);
			//if (treeLengthLimit>0 && goldTree.countLexicalNodes()>treeLengthLimit)
			//	continue;
			int parenthesis = lineParsed.indexOf('(');
			lineParsed = lineParsed.substring(parenthesis);
			pwParsedCleaned.println(lineParsed);
		}
		pwParsedCleaned.close();

	}

	private static void cleanOutputPartial(File parsedOutput,
			File goldFile, File parsedOutputCleaned) throws Exception {

		Scanner scanParsedPartial = FileUtil.getScanner(parsedOutput);
		Scanner scanGold = FileUtil.getScanner(goldFile);
		PrintWriter pw = FileUtil.getPrintWriter(parsedOutputCleaned);
		boolean skipSentence = false;
		while (scanParsedPartial.hasNextLine()) {
			String lineParsedPartial = scanParsedPartial.nextLine();
			if (treeLengthLimit>0 && lineParsedPartial.indexOf("1-1\t")!=-1) {
				String goldLine = scanGold.nextLine();
				TSNodeLabel goldTree = new TSNodeLabel(goldLine); 
				skipSentence = goldTree.countLexicalNodes()>treeLengthLimit;				
			}
			if (skipSentence)
				continue;
			if (lineParsedPartial.startsWith("Partial") || lineParsedPartial.startsWith("prefix"))
				continue;
			int parenthesis = lineParsedPartial.indexOf('(');
			int open = Utility.countCharInString(lineParsedPartial, '(');
			int closed = Utility.countCharInString(lineParsedPartial, ')');
			int diff = open - closed;
			lineParsedPartial = lineParsedPartial.substring(parenthesis) + Utility.repeat(")", diff);
			pw.println(lineParsedPartial);
		}
		pw.close();

	}

	private static void fixSlashes(File input, File output) throws Exception {
		Scanner scan = FileUtil.getScanner(input);
		PrintWriter pw = FileUtil.getPrintWriter(output);
		int lineNumber = 0;
		while (scan.hasNextLine()) {
			lineNumber++;
			String line = scan.nextLine();
			TSNodeLabel t = new TSNodeLabel(line);
			fixSlashes(t);
			pw.println(t.toString());
		}
		pw.close();
	}

	private static void fixSlashes(TSNodeLabel t) {
		String nodeLabelString = t.label();
		int slashIndex = nodeLabelString.indexOf('/');
		if (!t.isLexical && slashIndex != -1) {
			TSNodeLabel p = t.parent;
			TSNodeLabel g = p.parent;
			int pIndex = g.indexOfDaughter(p);
			String firstLabel = nodeLabelString.substring(0, slashIndex);
			String secondLabel = nodeLabelString.substring(slashIndex + 1);
			if (!secondLabel.equals(p.label())) {
				System.err.println("Problem A");
				return;
			}
			int dIndex = p.indexOfDaughter(t);
			int pProle = p.prole();
			TSNodeLabel[] afterTSiblings = Arrays.copyOfRange(p.daughters,
					dIndex + 1, pProle);
			p.daughters = Arrays.copyOfRange(p.daughters, 0, dIndex);

			int tProle = t.prole();
			TSNodeLabel newParentNode = new TSNodeLabel(
					Label.getLabel(firstLabel), false);

			TSNodeLabel[] newParentNodeDaughters = new TSNodeLabel[1 + tProle
					+ afterTSiblings.length];
			newParentNodeDaughters[0] = p;
			int i = 0;
			for (; i < tProle; i++) {
				newParentNodeDaughters[i + 1] = t.daughters[i];
			}
			for (int j = 0; j < afterTSiblings.length; j++) {
				newParentNodeDaughters[++i] = afterTSiblings[j];
			}
			newParentNode.assignDaughters(newParentNodeDaughters);

			g.daughters[pIndex] = newParentNode;
			newParentNode.parent = g;
		}
		if (t.isTerminal())
			return;
		for (TSNodeLabel d : t.daughters) {
			fixSlashes(d);
		}

	}

	private static void checkSlashes(File parsedOutputPartialClean) {
		HashSet<String> slashedCat = new HashSet<String>();
		Scanner scan = FileUtil.getScanner(parsedOutputPartialClean);
		while (scan.hasNextLine()) {
			String line = scan.nextLine();
			String[] cats = line.split("\\s+");
			for (String c : cats) {
				if (c.indexOf('/') != -1)
					slashedCat.add(c.replaceAll("\\(", "")
							.replaceAll("\\)", ""));
			}
		}
		System.out.println(slashedCat);
	}

	public static void splitPrefixes(File goldFile, int maxLength,
			File parsedOutputPartial, String prefixMinConnFile) throws Exception {
		
		String path = goldFile.getParent() + "/";
		prefixMinConnFile += "_";
		Scanner scan = FileUtil.getScanner(parsedOutputPartial);
		Scanner goldScan = FileUtil.getScanner(goldFile);
		Vector<PrintWriter> pwMinConnPrefix = new Vector<PrintWriter>();
		File minConnAllFile = new File(path + prefixMinConnFile + "All");
		PrintWriter pwMinConnAll = FileUtil.getPrintWriter(minConnAllFile);
		ArrayList<Label> previousLex = new ArrayList<Label>();
		int skipped = 0; // complete trees
		
		for(int i=1; i<=maxLength; i++) {
			PrintWriter pwMinConn = new PrintWriter(new File(
					path + prefixMinConnFile + Utility.padZero(3, i)));
			pwMinConnPrefix.add(pwMinConn);
		}
		
		boolean skipLine = false;
		
		while (scan.hasNextLine()) {
			String line = scan.nextLine();			
			TSNodeLabel t = new TSNodeLabel(line);			
			int length = t.countLexicalNodes();
			/*
			if (length==1) {
				if (!goldScan.hasNextLine()) {
					System.out.println("Not enough trees in gold. Expected tree starting with: " + t.getLeftmostLexicalNode());
					break;
				}					
				TSNodeLabel goldTree = new TSNodeLabel(goldScan.nextLine());
				int goldLength = goldTree.countLexicalNodes();
				skipLine = goldLength>maxLength;
			}
			if (skipLine)
				continue;
			*/
			t.prunePos(quotesPos);
			ArrayList<Label> lex = t.collectLexicalLabels();
			if (lex.equals(previousLex)) {
				// complete trees should not be included
				previousLex = lex;
				skipped++;
				continue;
			}
			previousLex = lex;
			PrintWriter pwMinConn =  pwMinConnPrefix.get(length - 1);
			t = t.getMinimalConnectedStructure(length);
			t.removeRedundantRules();
			String minConnStructString = t.toString();
			pwMinConn.println(minConnStructString);
			pwMinConnAll.println(minConnStructString);
		}
		for (PrintWriter pw : pwMinConnPrefix) {
			pw.close();
		}
		pwMinConnAll.close();
		System.out.println("Skipped: " + skipped);
	}

	static HashSet<Label> skipGoldLex = new HashSet<Label>(
			Arrays.asList(new Label[] { Label.getLabel("``"),
					Label.getLabel("''"), }));

	private static void printGoldCheckLex(File goldFile,
			File parsedOutputCleaned, File goldOutput) throws Exception {

		Scanner scanGold = FileUtil.getScanner(goldFile);
		Scanner scanParse = FileUtil.getScanner(parsedOutputCleaned);
		PrintWriter pw = FileUtil.getPrintWriter(goldOutput);

		int lineNumber = 0;
		while (scanGold.hasNextLine()) {
			lineNumber++;
			String goldLine = scanGold.nextLine();
			
			if (!scanParse.hasNextLine()) {
				System.err.println("Parsed has not enough lines");
				return;
			}
			String parsedLine = scanParse.nextLine();						
			TSNodeLabel parsedTree = new TSNodeLabel(parsedLine);
			TSNodeLabel goldTree = new TSNodeLabel(goldLine);
			ArrayList<TSNodeLabel> goldLex = goldTree.collectLexicalItems();
			ArrayList<TSNodeLabel> parsedLex = parsedTree.collectLexicalItems();
			
			/*
			if (goldLex.size()!=parsedLex.size()) {
				System.err.println("Gold and Parsed tree don't mach in lex number");
				return;
			}
			*/
			
			if (!goldLex.equals(parsedLex)) {
				Iterator<TSNodeLabel> goldLexIter = goldLex.iterator();
				Iterator<TSNodeLabel> parsedLexIter = parsedLex.iterator();
				while (goldLexIter.hasNext()) {
					TSNodeLabel goldNextLex = goldLexIter.next();
					if (skipGoldLex.contains(goldNextLex.parent.label))
						continue;
					if (!parsedLexIter.hasNext()) {
						System.err.println("ah");
						return;
					}
					TSNodeLabel parsedNextLex = parsedLexIter.next();
					if (goldNextLex.label != parsedNextLex.label)
						goldNextLex.label = parsedNextLex.label;
				}
			}
			pw.println(goldTree);
		}

		pw.close();

	}

	private static int maxLength(File goldFile) throws Exception {
		Scanner goldFileScanner = FileUtil.getScanner(goldFile);
		int maxLength = -1;
		while (goldFileScanner.hasNext()) {
			TSNodeLabel goldTree = new TSNodeLabel(goldFileScanner.nextLine());
			goldTree.prunePos(quotesPos);
			int l = goldTree.countLexicalNodes();
			if (l > maxLength)
				maxLength = l;
		}
		return maxLength;
	}

	public static void printGoldPartials(File goldFile, int maxLength,
			String prefixMinConnGoldFile) throws Exception {

		String path = goldFile.getParent() + "/";
		prefixMinConnGoldFile += "_";
		PrintWriter[] pwMinConn = new PrintWriter[maxLength];
		PrintWriter pwMinConnAll = FileUtil.getPrintWriter(new File(
				path + prefixMinConnGoldFile + "All"));

		for (int i = 0; i < maxLength; i++) {
			String prefixLength = Utility.padZero(3, i + 1);
			pwMinConn[i] = FileUtil.getPrintWriter(new File(
					path + prefixMinConnGoldFile + prefixLength));
		}

		Scanner goldFileScanner = FileUtil.getScanner(goldFile);
		while (goldFileScanner.hasNext()) {
			String line = goldFileScanner.nextLine();
			TSNodeLabel goldTree = new TSNodeLabel(line);
			int length = goldTree.countLexicalNodes();
			if (length>maxLength)
				continue;
			goldTree.prunePos(quotesPos);
			length = goldTree.countLexicalNodes();
			for (int i = 0; i < length; i++) {
				int prefixLength = i + 1;
				TSNodeLabel goldTreeChopped = goldTree.getMinimalConnectedStructure(prefixLength);
				goldTreeChopped.removeRedundantRules();
				String goldTreeChoppedString = goldTreeChopped.toString();
				pwMinConn[i].println(goldTreeChoppedString);
				pwMinConnAll.println(goldTreeChoppedString);
			}
		}

		for (int i = 0; i < maxLength; i++) {
			pwMinConn[i].close();
		}
		pwMinConnAll.close();
	}

	public static void evalPartial(int maxLength,
			String path, String prefixPartialGold, String prefixPartialTest)
			throws Exception {
		
		prefixPartialGold += "_";
		prefixPartialTest += "_";
		PrintWriter pwResults = FileUtil.getPrintWriter(new File(path + "partialResults.txt"));
		
		String test = path + prefixPartialTest + "All";
		String gold = path + prefixPartialGold + "All";
		File testFile = new File(test);
		File goldFile = new File(gold);
		checkCompatibility(testFile, goldFile);
		new EvalC(goldFile, testFile, new File(test + ".EvalC")).makeEval();
		new EvalB(goldFile, testFile, new File(test + ".EvalB"));
		
		for (int i = 0; i < maxLength; i++) {
						
			//EvalC.CONSTITUENTS_UNIT = 0; // span evaluation
			
			String prefixLength = Utility.padZero(3, i + 1);
			
			test = path + prefixPartialTest + prefixLength;
			gold = path + prefixPartialGold + prefixLength;
			testFile = new File(test);
			goldFile = new File(gold);
			
			File parsedMinConn = new File(test);
			File goldMinConn = new File(gold);
			File evalBFile = new File(test + ".evalB");
			checkCompatibility(goldFile, testFile);
			new EvalC(goldFile, testFile, new File(test + ".evalC")).makeEval();
			new EvalB(goldMinConn, parsedMinConn, evalBFile);
			
			String[] recPrecF1 = EvalB.getAllRecallPrecisionF1(evalBFile);
			pwResults.println(prefixLength + "\t" + Utility.arrayToString(recPrecF1, '\t'));

		}
		
		pwResults.close();

	}
	
	public static void splitPrefixesGoldTestGroupLength(int maxLength, File goldFile,
			File parsedOutputPartial, String prefixPartial) throws Exception {
		
		String path = goldFile.getParent() + "/";
		prefixPartial += "_";
		Scanner testScan = FileUtil.getScanner(parsedOutputPartial);
		Scanner goldScan = FileUtil.getScanner(goldFile);
		Vector<Vector<File>> filePartialGold = new Vector<Vector<File>>(maxLength);
		Vector<Vector<File>> filePartialTest = new Vector<Vector<File>>(maxLength);
				
		ArrayList<Label> previousLex = new ArrayList<Label>();
		int skipped = 0; // complete trees
		
		for(int i=1; i<=maxLength; i++) {
			Vector<File> filePartialGoldLength = new Vector<File>(i);
			Vector<File> filePartialTestLength = new Vector<File>(i);
			String iString = Utility.padZero(3, i);
			for(int j=1; j<=i; j++) {
				String jString = Utility.padZero(3, j);
				File filePartialGoldPrefix = new File(path + prefixPartial + "Gold_" + iString + "_" + jString);
				File filePartialTestPrefix = new File(path + prefixPartial + "Test_" + iString + "_" + jString);				
				filePartialGoldLength.add(filePartialGoldPrefix);
				filePartialTestLength.add(filePartialTestPrefix);				
			}
			filePartialGold.add(filePartialGoldLength);
			filePartialTest.add(filePartialTestLength);
		}
		
		//boolean skipLine = false;
		int goldLength = -1;
		TSNodeLabel goldTree = null;
		int testLineNumber=0, goldLineNumber=0;
		int testPrefixLength = 0;
		
		while (testScan.hasNextLine()) {
			String testLine = testScan.nextLine();
			testLineNumber++;
			TSNodeLabel t = new TSNodeLabel(testLine);
			testPrefixLength++;
			//int testPrefixLength = t.countLexicalNodes();
			
			t.prunePos(quotesPos);
			ArrayList<Label> lex = t.collectLexicalLabels();
			if (lex.equals(previousLex) && lex.size()==goldLength) {
				// complete trees should not be included
				skipped++;
				testPrefixLength=0;
				continue;
			}
			previousLex = lex;
			
			if (testPrefixLength==1) {
				if (!goldScan.hasNextLine()) {
					System.out.println("Not enough trees in gold. Expected tree starting with: " + t.getLeftmostLexicalNode());
					break;
				}					
				goldTree = new TSNodeLabel(goldScan.nextLine());
				goldLineNumber++;
				goldLength = goldTree.countLexicalNodes();
				//skipLine = goldLength>maxLength;
				//if (!skipLine) {
					for (int i = 0; i < goldLength; i++) {
						int prefixLength = i + 1;
						TSNodeLabel goldTreeChopped = goldTree.getMinimalConnectedStructure(prefixLength);
						goldTreeChopped.removeRedundantRules();
						String goldTreeChoppedString = goldTreeChopped.toString();
						FileUtil.appendReturn(goldTreeChoppedString, filePartialGold.get(goldLength-1).get(prefixLength-1));
					}
				//}
			}
			//if (skipLine)
			//	continue;
						
			t = t.getMinimalConnectedStructure(testPrefixLength);
			t.removeRedundantRules();
			String minConnStructString = t.toString();
			if (testPrefixLength>goldLength) {
				System.err.println("Errror");
				System.err.println("Gold line number: " + goldLineNumber);
				System.err.println("Test line number: " + testLineNumber);
				System.err.println("Gold tree: " + goldTree);
				System.err.println("Test partial tree: " + goldTree);
				return;
			}
			FileUtil.appendReturn(minConnStructString, filePartialTest.get(goldLength-1).get(testPrefixLength-1));
			
			if (testPrefixLength==goldLength)
				testPrefixLength=0;
		}
		System.out.println("Skipped: " + skipped);
	}
	
	public static void evalPartialGroupLength(int maxLength,
			String path, String prefixPartial)
			throws Exception {
		
		prefixPartial += "_";
			
		for (int i = 0; i < maxLength; i++) {
			
			String length = Utility.padZero(3, i + 1);
			File result = new File(path + "ResultsPartialLength_" + length + ".txt");
			PrintWriter pwResults = FileUtil.getPrintWriter(result);
			
			for (int j = 1; j <= i; j++) {
										
				String prefixLength = Utility.padZero(3, j + 1);
				
				File filePartialGoldPrefix = new File(path + prefixPartial + "Gold_" + length + "_" + prefixLength);
				File filePartialTestPrefix = new File(path + prefixPartial + "Test_" + length + "_" + prefixLength);
				
				if (!filePartialGoldPrefix.exists())
					continue;
				
				File evalBFile = new File("Eval_" + length + "_" + prefixLength + ".evalB");
				checkCompatibility(filePartialGoldPrefix, filePartialTestPrefix);
				//new EvalC(goldFile, testFile, new File(test + ".evalC")).makeEval();
				new EvalB(filePartialGoldPrefix, filePartialTestPrefix, evalBFile);
								
				String[] recPrecF1 = EvalB.getAllRecallPrecisionF1(evalBFile);
				//pwResults.println(prefixLength + "\t" + Utility.arrayToString(recPrecF1, '\t'));
				pwResults.println(prefixLength + "\t" + recPrecF1[2]);
			
			}
			
			pwResults.close();

		}

	}

	// assuming more lines in parsed
	private static void checkCompatibility(File parsed, File gold) {

		boolean compatibe = true;

		long linesParsed = FileUtil.getNumberOfLines(parsed);
		long linesGold = FileUtil.getNumberOfLines(gold);
		if (linesParsed == linesGold)
			return;

		//File tmp = FileUtil.changeExtention(parsed, "tmp");
		Scanner parsedScanner = FileUtil.getScanner(parsed);
		Scanner goldScanner = FileUtil.getScanner(gold);
		int lineNumber = 0;
		while (goldScanner.hasNextLine()) {
			lineNumber++;
			String goldLine = goldScanner.nextLine();
			String parsedLine = parsedScanner.nextLine();
			TSNodeLabel goldTree = null;
			try {
				goldTree = new TSNodeLabel(goldLine);
			} catch (Exception e) {
				System.out.println("Error in '" + gold.getName() + "'  line " + lineNumber);
				System.out.println(goldLine);
				return;
			}
			TSNodeLabel parsedTree = null;
			try {
				parsedTree = new TSNodeLabel(parsedLine);
			} catch (Exception e) {
				System.out.println("Error in '" + parsed.getName() + "'  line " + lineNumber);
				System.out.println(parsedLine);
				return;
			}
			if (goldTree.countLexicalNodes() != parsedTree.countLexicalNodes()) {
				compatibe = false;
				break;
			}
		}
		//tmp.renameTo(parsed);

		System.err.println("Non compatibleL\n\t" + parsed + "\n\t" + gold);
	}
	
	public static void mainCleanRoarkOutput(String[] args) throws Exception {
		treeLengthLimit = Integer.parseInt(args[0]);
		File parsedOutput = new File(args[1]);
		File goldFile = new File(args[2]);
		File parsedOutputCleaned = new File(args[3]);
		File goldFiltered = args.length>4 ? new File(args[4]) : null;
		cleanRoarkOutput(parsedOutput, goldFile, parsedOutputCleaned, goldFiltered);
	}
	
	/**
	 * @param args
	 * @throws Exception
	 */
	/*
	public static void mainIncrementalParser(String[] args) throws Exception {
		
		treeLengthLimit = -1;
		
		String treeLengthLimitString = treeLengthLimit==-1 ? "All" : 
			Integer.toString(treeLengthLimit);
		
		// String basePath = System.getProperty("user.home") + "/";
		// String roarkParserDir = basePath +
		// "Work/Edinburgh/roarkparser/parse_wsj24/";
		File goldFile = new File(Parameters.corpusPath
				+ "WSJ/ORIGINAL_READABLE_CLEANED_TOP_SEMTAGSOFF/wsj-23.mrg");
	
		String srcDir = Parameters.scratchPath + "roarkparser/";
		String dstDir = srcDir + "parse_wsj23_" + treeLengthLimitString + "/";
		new File(dstDir).mkdir();
		
		File parsedOutput = new File(srcDir + "parse_wsj23.output");
		File parsedOutputPartial = new File(srcDir + "parse_wsj23.output.partial");
		
		File parsedOutputCleaned = new File(dstDir+ "parse.output.cleaned");
		File goldOuputFiltered = new File(dstDir+ "gold.filtered");
		cleanRoarkOutput(parsedOutput, goldFile, parsedOutputCleaned, goldOuputFiltered);
		File goldOutput = new File(dstDir + "parse.output.cleaned.GOLD");
		printGoldCheckLex(goldOuputFiltered, parsedOutputCleaned, goldOutput);
		File parsedOutputCleanedEvalB = new File(dstDir + "parse.output.cleaned.EvalB");
		File parsedOutputCleanedEvalC = new File(dstDir + "parse.output.cleaned.EvalC");
		
		new EvalC(goldOutput, parsedOutputCleaned, parsedOutputCleanedEvalC).makeEval();
		new EvalB(goldOutput, parsedOutputCleaned, parsedOutputCleanedEvalB);
		
		File parsedOutputPartialClean = new File(dstDir + "parse.output.partial.cleaned");
		cleanOutputPartial(parsedOutputPartial, goldFile, parsedOutputPartialClean);
		//checkSlashes(parsedOutputPartialClean);
	
		File parsedOutputPartialCleanFixSlashes = new File(dstDir + "parse.output.partial.cleaned.fixed");
		fixSlashes(parsedOutputPartialClean, parsedOutputPartialCleanFixSlashes);
	
		String prefixMaxPredFile = dstDir
				+ "parse.output.partial.cleaned.fixed.MaxPred_prefix_";
		String prefixMinConnFile = dstDir
				+ "parse.output.partial.cleaned.fixed.MinConn_prefix_";
		File maxPredAllFile = new File(dstDir
				+ "parse.output.partial.cleaned.fixed.MaxPred_ALL");
		File minConnAllFile = new File(dstDir
				+ "parse.output.partial.cleaned.fixed.MinConn_ALL");		
		
		int maxLength = treeLengthLimit==-1 ? maxLength(goldOuputFiltered) : treeLengthLimit;
		
		splitPrefixes(goldOuputFiltered, maxLength, parsedOutputPartialCleanFixSlashes, prefixMaxPredFile,
				prefixMinConnFile, maxPredAllFile, minConnAllFile);
		
		printGoldPartials(goldOuputFiltered, maxLength, prefixMaxPredFile,
				prefixMinConnFile, maxPredAllFile, minConnAllFile);
		
		File allMinConnPrefixResults = new File(dstDir
				+ "Prefix_MinConn_Results.txt");
		
		evalPartial(maxLength, prefixMaxPredFile, prefixMinConnFile,
				maxPredAllFile, minConnAllFile, allMinConnPrefixResults);
		
	}
	*/
	
	private static void partialParsingITSGnoQuotes() {
		String path = "/Users/fedja/Work/Edinburgh/PLTSG/Results/wsj23/ITSG_wsj23_NoSmoothing_all_MRP/incremental/";
		File originalPartialAll = new File(path + "PARSED_MinConnected_All.mrg");
		File newPartialAll = new File(path + "NoQuotes/PARSED_MinConnected_All.mrg");
		
	}

	private static void checkRedundantRules(File file) throws Exception {
		Scanner scan = FileUtil.getScanner(file);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			TSNodeLabel t = new TSNodeLabel(line);
			if (t.hasRedundantRules()) {
				System.out.println("Found redundant rule: " + t);
				break;
			}
		}
		System.out.println("No redundant rules");
	}

	private static void mergeResults() {
		String path = "/Users/fedja/Work/Edinburgh/PLTSG/Results/wsj23/";
		String itsgPath = path + "ITSG_wsj23_NoSmoothing_all_MRP/incremental/NoRedundant/LengthSplit/";
		String roarkPath = path + "roark/parse_wsj23_All/LengthSplit/";
		String cmpPath = path + "PartialLength_ITSGNoSmooth_Roark/";
		
		for(int i=2; i<=65; i++) {
			String iString = Utility.padZero(3, i);
			File itsgFile = new File(itsgPath + "ResultsPartialLength_" + iString + ".txt");
			File roarkFile = new File(roarkPath + "ResultsPartialLength_" + iString + ".txt");
			File outputFile = new File(cmpPath + "ResultsPartialLength_" + iString + ".txt");
			PrintWriter pw = FileUtil.getPrintWriter(outputFile);
			Scanner itsgScan = FileUtil.getScanner(itsgFile);
			Scanner roarkScan = FileUtil.getScanner(roarkFile);
			while(itsgScan.hasNextLine()) {
				String itsgLine = itsgScan.nextLine();
				String roarkLine = roarkScan.nextLine();
				pw.println(itsgLine + "\t" + roarkLine.split("\t")[1]);
			}
			pw.close();
		}		
	}

	public static void main(String[] args) throws Exception {
		
		/*
		//ROARK
		String path = "/Users/fedja/Work/Edinburgh/PLTSG/Results/wsj23/roark/parse_wsj23_All/";
		File goldFile = new File(path + "Gold_Original_NoQuotes.mrg");  
		//printGoldPartials(goldFile, 100, "partialGoldNoQuotes");
		File parsedOutputPartial = new File(path + "parse.output.partial.cleaned.fixed");
		//splitPrefixes(goldFile, 70, parsedOutputPartial, "partialTest");
		//evalPartial(65, path, "partialGoldNoQuotes", "partialTest");		
		//checkRedundantRules(new File(path + "partialTest_All"));
		path += "LengthSplit/";
		goldFile = new File(path + "Gold_Original_NoQuotes.mrg");
		parsedOutputPartial = new File(path + "parse.output.partial.cleaned.fixed");
		//splitPrefixesGoldTestGroupLength(65, goldFile, parsedOutputPartial, "partial");
		//evalPartialGroupLength(65, path, "partial");
		*/
		

		//ITSG
		String path = "/Users/fedja/Work/Edinburgh/PLTSG/Results/wsj23/" +
				//"ITSG_wsj23_NoSmoothing_all_MRP/incremental/NoRedundant/";
				//"ITSG_wsj23_Smoothing_Pos_all/incremental/NoRedundant/";
				"ITSG_wsj23_Smoothing_Init_all/incremental/NoRedundant/";
				//"ITSG_wsj23_Smoothing_Init_Pos_all_MRP/incremental/NoRedundant/";
				//"ITSG_wsj23_Smoothing_Init_Pos_Temp_all/incremental/NoRedundant/";
		File goldFile = new File(path + "GOLD.mrg");
		printGoldPartials(goldFile, 65, "partialGoldNoQuotes");
		File parsedOutputPartial = new File(path + "PARSED_partial_output.mrg");
		splitPrefixes(goldFile, 65, parsedOutputPartial, "partialTest");
		evalPartial(65, path, "partialGoldNoQuotes", "partialTest");		
		//checkRedundantRules(new File(path + "partialTest_All"));
		
		path += "LengthSplit/";
		goldFile = new File(path + "GOLD.mrg");
		//parsedOutputPartial = new File(path + "PARSED_partial_output.mrg");
		//splitPrefixesGoldTestGroupLength(65, goldFile, parsedOutputPartial, "partial");
		//evalPartialGroupLength(65, path, "partial");
		//cleanRoarkOutput(new File(args[0]),new File(args[1]));

		
		//partialParsingITSGnoQuotes();
		//mainMinConnectEval(args);
				
		//mergeResults();
		
		
	}

	





}
