package kernels.parallel;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Scanner;
import java.util.TreeSet;

import util.IdentityArrayList;
import util.PrintProgress;
import util.file.FileUtil;

public class ExtractExactIndexes {
	
	static final boolean debug = true;
	
	HashMap<IdentityArrayList<String>,TreeSet<Integer>> sourceTable, targetTable;
	
	public ExtractExactIndexes(File sourceNgrams, File sourceIdx, 
			File targetNgrams, File targetIdx,
			File approxTable, File outputFile) {
		
		sourceTable = getNgramIndexes(sourceNgrams, sourceIdx, "Reading Source files");
		System.out.println("Number source key: " + sourceTable.size());
		targetTable = getNgramIndexes(targetNgrams, targetIdx, "Reading Target files");
		System.out.println("Number target key: " + targetTable.size());
		buildExactTable(approxTable, outputFile);
	}

	private HashMap<IdentityArrayList<String>, TreeSet<Integer>> getNgramIndexes(
			File ngrams, File idx, String message) {
		
		PrintProgress pp = new PrintProgress(message, 10000, 0);
		HashMap<IdentityArrayList<String>, TreeSet<Integer>> result = 
				new HashMap<IdentityArrayList<String>, TreeSet<Integer>>();
		Scanner scanNgram = FileUtil.getGzipScanner(ngrams);
		Scanner scanIdx = FileUtil.getGzipScanner(idx);
		while(scanNgram.hasNextLine()) {
			pp.next();
			String lineNgram = scanNgram.nextLine();
			String lineIdx = scanIdx.nextLine();
			IdentityArrayList<String> phrase = ParallelSubstrings.getIdentityArrayList(lineNgram, " ");
			TreeSet<Integer> indexeSet = ParallelSubstrings.getIndexeSetFromParenthesis(lineIdx);
			result.put(phrase, indexeSet);			
		}
		pp.end();
		return result;
	}
	
	@SuppressWarnings("unchecked")
	private void buildExactTable(File approxTable, File outputFile) {
		Scanner scanTable = FileUtil.getGzipScanner(approxTable);
		PrintWriter pw = FileUtil.getGzipPrintWriter(outputFile);
		PrintProgress pp = new PrintProgress("Building Exact Index Table", 10000, 0);
		IdentityArrayList<String> source=null, target=null;
		TreeSet<Integer> sourceIndexes = null, targetIndexes = null, intesectionIndexes = null; 
		while(scanTable.hasNextLine()) {
			String line = scanTable.nextLine();
			String[] split = line.split("\t");
			if (split.length==1) {
				pw.println(line);
				continue; //2: //new implementation
			}
			if (split.length==2) {
				//	[check, for] 
				pw.println(line);
				source = ParallelSubstrings.getIdentityArrayListFromBracket(split[1]);
				sourceIndexes = sourceTable.get(source);
				if (sourceIndexes==null) {
					System.err.println("Not found source key: " + source);
					return;
				}
				continue;
			}
			// split.length==4 //\t\t[che, per, la]  [23687, 34596, 186687]
			pp.next();
			target = ParallelSubstrings.getIdentityArrayListFromBracket(split[2]);			
			targetIndexes = targetTable.get(target);
			if (targetIndexes==null) {
				System.err.println("Not found target key: " + target);
				return;
			}
			intesectionIndexes = (TreeSet<Integer>) sourceIndexes.clone();
			intesectionIndexes.retainAll(targetIndexes);
			pw.println("\t\t" + split[2] + "\t" + intesectionIndexes.toString());
			if (debug) {
				TreeSet<Integer> approxIndexes = ParallelSubstrings.getIndexeSetFromParenthesis(split[3]);
				if (!intesectionIndexes.containsAll(approxIndexes)) {
					System.err.println("Error in pair:");
					System.err.println("\tsource:" + source);
					System.err.println("\texact source indexes:" + sourceIndexes);
					System.err.println("\ttarget:" + target);
					System.err.println("\texact target indexes:" + targetIndexes);
					System.err.println("\tapprox indexes:" + approxIndexes);
					System.err.println("\texact intersection indexes:" + intesectionIndexes);
					return;
				}
			}
			
		}
		pp.end();
		pw.close();
		
	}
	
	public static void main(String[] args) {
		
		String usage = "java ExtractExactIndexes "				
				+ "source.txt.gz source.idx.gz "
				+ "target.txt.gz target.idx.gz "
				+ "approxTable.gz outputTable.gz";
		
		/*
		String workingDir = "/Users/fedja/Dropbox/ted_experiment/en_it/kernels/";
		args = new String[]{
			workingDir + "source.txt.gz",
			workingDir + "source.txt.idx.gz",
			workingDir + "target.txt.gz",
			workingDir + "target.txt.idx.gz",
			workingDir + "kernels.table.m2.gz",
			workingDir + "kernels.table.m2.exact.gz"
		};
		*/
		
		if (args.length!=6) {
			System.err.println("Wrong number of arguments");
			System.err.println(usage);
		}
				
		File sourceNgrams = new File(args[0]);
		File sourceIdx = new File(args[1]);
		File targetNgrams = new File(args[2]);
		File targetIdx = new File(args[3]);
		File approxTable = new File(args[4]);
		File outputTable = new File(args[5]);

		new ExtractExactIndexes(sourceNgrams, sourceIdx, targetNgrams, targetIdx, approxTable, outputTable);
	}

}
