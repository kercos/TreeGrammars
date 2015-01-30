package tsg.corpora;

import java.io.*;
import java.util.*;

import tsg.TSNode;
import util.*;
import util.file.FileUtil;


public class Tiger extends ConstCorpus{
	
	private static final long serialVersionUID = 0L;
	private static final String traceTag = "-NONE-";
	private static final File fdscDir = new File("/scratch/fsangati/CORPUS/Tiger/fdsc-Aug07");
	
	static public ConstCorpus annotateHeads(ConstCorpus tiger) {
		ConstCorpus result = new ConstCorpus();
		int wrongAssignment = 0;		
		//File xmlDB = new File("/home/fsangati/CORPUS/Tiger/tiger_DB.xml");
		File outputFileDependency = new File("/home/fsangati/CORPUS/Tiger/tiger_DB_readable");
		PrintWriter dependencyWriter = FileUtil.getPrintWriter(outputFileDependency);
		//tiger.removeQuotationMarks();
		//String[][] wordsIndex =  scanXmlFile(xmlDB);
		File[] fileList = fdscDir.listFiles();		
		Arrays.sort(fileList);
		int[] indexes = new int[fileList.length]; 
		for(int i=0; i<fileList.length; i++) {
			String fileName = fileList[i].getName();
			int number = Integer.parseInt(fileName.substring(9,14));
			TSNode tree = tiger.treeBank.get(number-3);
			result.treeBank.add(tree);
			indexes[i] = number-3;
			tree.removeHeadAnnotations();
			//String flatTree = tree.toFlat();
			//int lengthFlatTree = flatTree.split(" ").length;			
			String[] dependecyStrings = scanDependencyFile(fileList[i]);
			int[][] dependency = readDependency(dependecyStrings, tree, fileName);
			//String depSentence = dependecies[0];
			String[] output = new String[] {""};
			boolean right = DepConstConverter.assignHeadsFromDependencyTable(tree, dependency, output);
			if (!right) wrongAssignment++;
			dependencyWriter.println(tree.collectTerminals().toString());
			dependencyWriter.println(output[0]);	
		}
		System.out.println("Wrong assignments: " + wrongAssignment + "/" + fileList.length);
		System.out.println(Arrays.toString(indexes));
		return result;
	}
	
	public static void printTiger() {
		boolean quotations = false;
		File inputFile = new File("/home/fsangati/CORPUS/Tiger/Complete/tiger_release_july03.penn");
		String outputComplete = "/home/fsangati/CORPUS/Tiger/Complete/tiger_Complete";
		ConstCorpus corpora = new ConstCorpus(inputFile, "TIGER");
		corpora.removeTraces(traceTag);
		corpora.toFile_Complete(new File(outputComplete), quotations);
	}
	
	public static int[][] readDependency(String[] dependecyStrings, TSNode tree, String fileName) {
		int countDep = 0, countRoot = 0, indexRoot = -1;
		for(int i=1; i<dependecyStrings.length; i++) {
			dependecyStrings[i] = dependecyStrings[i].replaceAll("\\)\\)", ")");
			String dep = dependecyStrings[i];
			int firstTilde = dep.indexOf('~');
			int secondTilde = dep.indexOf('~', firstTilde+1);
			if (secondTilde!=-1) {
				int comaIndex = dep.lastIndexOf(", ");
				int closeIndex = dep.lastIndexOf(')');
				int firstIndex = Integer.parseInt(dep.substring(firstTilde+1, comaIndex).trim());
				int secondIndex = Integer.parseInt(dep.substring(secondTilde+1, closeIndex).trim());
				if (firstIndex<500 && secondIndex<500) countDep++;
			}
			else if (dep.startsWith("tiger_id") || dep.startsWith("coord_form")) {				
				int comaIndex = dep.lastIndexOf(", ");
				int openIndex = dep.indexOf('(');
				int closeIndex = dep.lastIndexOf(')');
				int firstIndex = Integer.parseInt(dep.substring(firstTilde+1, comaIndex).trim());
				if (firstIndex==0) {
					String realIndex = dep.substring(comaIndex+1, closeIndex).trim();
					if (dep.startsWith("coord_form")) indexRoot = getConjunctionIndex(tree, realIndex) + 1;												
					else indexRoot = Integer.parseInt(realIndex);					
					String root = dep.substring(openIndex+1, firstTilde).trim();
					countRoot++;
					for(int j=1; j<dependecyStrings.length; j++) {
						String replaceFrom = root + "~0,";
						String replaceTo = root + "~" + indexRoot + ",";
						dependecyStrings[j] = dependecyStrings[j].replaceAll(replaceFrom, replaceTo);
					}	
				}
			}
		}
		if (countRoot>1) {
			//System.out.println("Problems ROOTS!");
		}
		if (countRoot==0) {
			//System.out.println("NO ROOTS");
		}
		int[][] result = new int[countDep][2];
		int depIndex = 0;
		for(int i=1; i<dependecyStrings.length; i++) {
			String dep = dependecyStrings[i];
			int firstTilde = dep.indexOf('~');
			int secondTilde = dep.indexOf('~', firstTilde+1);
			if (secondTilde!=-1) {
				int comaIndex = dep.lastIndexOf(", ");
				int closeIndex = dep.lastIndexOf(')');
				int firstIndex = Integer.parseInt(dep.substring(firstTilde+1, comaIndex).trim());
				int secondIndex = Integer.parseInt(dep.substring(secondTilde+1, closeIndex).trim());
				if (firstIndex!=0 && firstIndex<500 && secondIndex<500) {
					result[depIndex][0] = firstIndex - 1;
					result[depIndex][1] = secondIndex - 1;
					depIndex++;
				}
			}
		}
		return result;
	}
	
	public static int getConjunctionIndex(TSNode tree, String conj) {
		List<TSNode> list = tree.collectTerminals();
		int minHight = Integer.MAX_VALUE;
		int countMinHight = 0;
		int bestIndex = -1;
		for(ListIterator<TSNode> i = list.listIterator(); i.hasNext();) {
			TSNode term = i.next();			
			if (term.toString(false, false).toLowerCase().equals(conj)) {
				int termHight = term.hight();				
				if ( termHight <= minHight ) {
					if ( termHight == minHight ) countMinHight++;
					else countMinHight = 1;
					bestIndex = i.previousIndex();
					minHight = termHight;					
				}
				
			}
		}
		if (countMinHight==0 || countMinHight>1) {
			System.out.print("");
		}
		return bestIndex;
	}
	
	public static String[][] scanXmlFile(File xmlFile) {
		LinkedList<String> sentenceWords = new LinkedList<String>(); 
		String[][] result = new String[2000][];
		int sentenceIndex = 0;
		try {
			Scanner scanfdsc = new Scanner(xmlFile, "ISO-8859-1");
			while ( scanfdsc.hasNextLine()) {	
				String dependencyLine = scanfdsc.nextLine();
				dependencyLine = dependencyLine.trim();
				if (dependencyLine.indexOf("<t id=")==0) {
					int startIndex = dependencyLine.indexOf("word=")+6;
					int endIndex = dependencyLine.indexOf("lemma=")-2;
					String word = dependencyLine.substring(startIndex, endIndex);
					sentenceWords.add(word);
					continue;
				}
				if (dependencyLine.indexOf("</terminals>")==0) {
					if (!sentenceWords.isEmpty()) {
						result[sentenceIndex] = new String[sentenceWords.size()];
						result[sentenceIndex] = sentenceWords.toArray(result[sentenceIndex]);
						sentenceIndex++;
						sentenceWords.clear();
					}					
				}					
			}
		} catch (IOException e) {FileUtil.handleExceptions(e);}
		return result;
	}
	
	public static String[] scanDependencyFile(File depFile) {
		LinkedList<String> sentenceDependency = new LinkedList<String>(); 
		//the first element is the sentence, the following elements the dependencies  
		try {
			Scanner scanfdsc = new Scanner(depFile, "ISO-8859-1");
			while ( scanfdsc.hasNextLine()) {	
				String dependencyLine = scanfdsc.nextLine();
				dependencyLine = dependencyLine.trim();
				if (dependencyLine.length()==0) continue;
				if (dependencyLine.indexOf("sentence(")==0) continue;
				if (dependencyLine.indexOf("id(TiGerDB")==0) continue;	
				if (dependencyLine.indexOf("structure(")==0) continue;
				if (dependencyLine.equals(")")) {
					String[] result = new String[sentenceDependency.size()];
					result = sentenceDependency.toArray(result);
					return result;
				}
				if (dependencyLine.indexOf("sentence_form(")==0) {
					int openParenthesis = dependencyLine.indexOf('(');
					int closeParenthesis = dependencyLine.lastIndexOf(')');
					dependencyLine = dependencyLine.substring(openParenthesis+1, closeParenthesis-1);
				}	
				sentenceDependency.add(dependencyLine);
			}
		} catch (IOException e) {FileUtil.handleExceptions(e);}
		return null;
	}
	
	public static void buildBinaryTiger() {
		File tigerPennComplete = new File("/home/fsangati/CORPUS/Tiger/Complete/tiger_release_july03_compressed.penn");		
		ConstCorpus tiger = new ConstCorpus(tigerPennComplete, "TIGER");		
		tiger.toBinaryFile(new File("/home/fsangati/CORPUS/Tiger/Complete/tiger_binary_complete"));
	}
	
	public static void main(String args[]) {
		buildBinaryTiger();
	}
	
}
