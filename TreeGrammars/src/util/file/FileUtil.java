package util.file;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import tsg.*;
import util.Utility;

public class FileUtil {
	
	public static String defaultEncoding = "UTF-8";
	
	
	/**
	 * Return a printWriter writing a specific outoutFile with a specific encoding
	 * @param outputFile
	 * @param encoding
	 * @return
	 */
	public static PrintWriter getPrintWriter(File outputFile, String encoding) {
		try{
			PrintWriter output = new PrintWriter(outputFile, encoding);
			return output;
		} catch (IOException e) {FileUtil.handleExceptions(e);}		
		return null;
	}
	
	
	/**
	 * Retrun a printWriter writing a specific outputFile with default encoding
	 * @param outputFile
	 * @return
	 */
	public static PrintWriter getPrintWriter(File outputFile) {
		return getPrintWriter(outputFile, defaultEncoding);
	}
	
	public static void cleanFile(File outputFile) {
		PrintWriter pw = getPrintWriter(outputFile);
		pw.close();
	}
	
	/**
	 * Returns a Scanner of the inputFile using a specific encoding
	 * @param inputFile
	 * @return
	 */
	public static Scanner getScanner(File inputFile, String encoding) {
		Scanner scan = null;
		try {
			scan = new Scanner(inputFile, encoding);
		} catch (IOException e) {FileUtil.handleExceptions(e);}
		return scan;
	}
	
	/**
	 * Returns a Scanner of the inputFile using default encoding
	 * @param inputFile
	 * @return
	 */
	public static Scanner getScanner(File inputFile) {
		return getScanner(inputFile, defaultEncoding);
	}
	
	/**
	 * Convert the inputFile in a LInkedList<String> one String for each line
	 * using a specific encoding.
	 * @param inputFile
	 * @return
	 */
	public static List<String> convertFileToStringList(File inputFile, String encoding) {
		List<String> list = new ArrayList<String>();
		Scanner scan = getScanner(inputFile, encoding);
		while ( scan.hasNextLine()) {		
			String line = scan.nextLine();
			line = line.trim();
			if (line.length()==0) continue;					
			line = line.replaceAll("\n", "");
			line = line.replaceAll("\\s+", " ");
			list.add(line);
		}				
		scan.close();		
		return list;
	}
	
	/**
	 * Convert the inputFile in a LInkedList<String> one String for each line
	 * using the default encoding.
	 * @param inputFile
	 * @return
	 */
	public static List<String> convertFileToStringList(File inputFile) {
		return convertFileToStringList(inputFile, defaultEncoding);
	}

	/**
	* Append in the input file fileName the line contained 
	* in the input String line.
	*/   		
	public static void appendReturn(String line, File fileName) {
		try {
			FileWriter OutputWriter = new FileWriter(fileName, true);
			OutputWriter.write(line+"\n");	
			OutputWriter.close();
		}
		catch (Exception e) {handleExceptions(e);}
	}
	
	/**
	* Append in the input file fileName the line contained 
	* in the input String line.
	*/   		
	public static void append(String line, File fileName) {
		try {
			FileWriter OutputWriter = new FileWriter(fileName, true);
			OutputWriter.write(line);	
			OutputWriter.close();
		}
		catch (Exception e) {handleExceptions(e);}
	}

	/**
	* Clear the content of the input file fileName
	*/   		
	public static void clear(File fileName) {
		try {
			FileWriter OutputWriter = new FileWriter(fileName, false);
			//OutputWriter.write(line+"\n");	
			OutputWriter.close();
		}
		catch (Exception e) {handleExceptions(e);}
	}

	/**
	*  Returns a String giving a compact representation 
	*  of the current date and time.
	*/
	public static String dateTimeString() {
		Date d = new Date();
		String data = ((d).toString());
		data = data.substring(0,data.length()-9);
		data = data.replace(' ','_');
		data = data.replace(':','_');
		return data;
	}
	
	public static String dateTimeStringUnique() {
		String result = dateTimeString();
		result += "_" + getUniqueString(5);
		return result;
	}

	private static String getUniqueString(int length) {		
		StringBuilder sb = new StringBuilder();				
		Random random = new Random();
		int a = 97;
		for(int i=0; i<length; i++) {
			int randomLetter = random.nextInt(26);
			sb.append((char)(a+randomLetter));
		}
		return sb.toString();
	}


	/**
	* Method to compare two files of bracketted sentences.
	*/
	public static void compareTreesInFiles(File fileA, File fileB) {
		LinkedList<String> linesA = getSentences(fileA, true);
		LinkedList<String> linesB = getSentences(fileB, true);
		if (linesA.size() != linesB.size()) {
			System.out.println("Unequal number of lines: " + linesA.size() + " " + linesB.size());
			return;
		}
		for(int i=0; i<linesA.size(); i++) {
			String lineA = linesA.get(i);
			String lineB = linesB.get(i);
			TSNode TNA = new TSNode(lineA, false);
			TSNode TNB = new TSNode(lineB, false);
			if (lineA.length()==0 || lineB.length()==0) {
				if (lineA.length()==0 && lineB.length()==0) System.out.println(i + ": OK");
				else System.out.println(i + ": Length unmatch (one line is empty)");
				continue;
			}
			int lengthA = TNA.collectTerminals().size();
			int lengthB = TNB.collectTerminals().size();
			if (lengthA != lengthB) System.out.println(i + ": Length unmatch (" + lengthA + "|" + lengthB + ")");
			else System.out.println(i + ": OK");
		}
	}

	/**
	* Method to handle standard IO xceptions.
	* catch (Exception e) {Utility.handleIO_exception(e);}
	*/   		
	public static void handleExceptions(Exception e) {
		e.printStackTrace();
		System.exit(-1);
	}

	/**
	* This method returns a LinkedList of String. Each element of the list corresponds  
	* to a sentence from the untagged input file. The boolean duplicate in the input
	* determines if duplicate sentences are allowed in the output LinkedList or not.
	*/   
	static public LinkedList<String> getSentences(File file, boolean duplicate) {
		LinkedList<String> list = new LinkedList<String>();				
		String line = "";		
		try {
			FileReader InputReader = new FileReader(file);
			for(;;) { // this loop writes writes in a Sting each sentence of the file and process it
				int current = InputReader.read();
				if (current == -1 || current=='\n') {
					if (duplicate || !list.contains(line)) list.add(line);
					line = "";
					if (current==-1) break; //EOF
				}
				else line += (char)current;
			}				
			InputReader.close();
		}
		catch (Exception e) {handleExceptions(e);}
		return list;		
	}
	
	static public String getFirstLineInFile(File inputFile) {
		Scanner scan = FileUtil.getScanner(inputFile);
		if (!scan.hasNextLine()) return null;
		String line = scan.nextLine();
		scan.close();
		return line;
	}
	
	static public String getLineInFileStartingWith(File inputFile, String prefix) {
		Scanner scan = FileUtil.getScanner(inputFile);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.startsWith(prefix)) {				 
				scan.close();
				return line;
			}
		}
		scan.close();
		return null;
	}
	
	public static void toBinaryFile(File outputFile, Object o) {
		try{
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(outputFile));
			out.writeObject(o);
		} catch (Exception e) {FileUtil.handleExceptions(e);}		
	}
	
	public static Object fromBinaryFile(File inputFile) {
		Object o = null;
		try {
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(inputFile));
			o = in.readObject();
		} catch (Exception e) {FileUtil.handleExceptions(e);}
		return o;
	}
	
	public static Object fromBinaryInputStream(InputStream inputStream) {
		Object o = null;
		try {
			ObjectInputStream in = new ObjectInputStream(inputStream);
			o = in.readObject();
		} catch (Exception e) {FileUtil.handleExceptions(e);}
		return o;
	}
	
	public static void printHashtableToPw(Hashtable<String,Integer> table, PrintWriter writer) {
		for (Enumeration<String> e = table.keys(); e.hasMoreElements() ;) {
			String rule = e.nextElement();			
			Integer count = table.get(rule);										
			writer.println(count.toString() + "\t" + rule);
		}		
	}
	
	public static void printHashtableToFile(Hashtable<String,Integer> table, File outputFile) {
		PrintWriter writer = FileUtil.getPrintWriter(outputFile);
		printHashtableToPw(table, writer);
		writer.close();
	}
	
	public static void printHashtableToPwOrder(Hashtable<String,Integer> table, PrintWriter writer) {
		TreeSet<Integer> values = new TreeSet<Integer>(table.values());
		for (Integer i : values) {
			for (Enumeration<String> e = table.keys(); e.hasMoreElements() ;) {
				String rule = e.nextElement();			
				Integer count = table.get(rule);
				if (count.equals(i)) writer.println(count.toString() + "\t" + rule);					
			}
		}
	}
	
	public static void printHashtableToFileOrder(Hashtable<String,Integer> table, File outputFile) {
		PrintWriter writer = FileUtil.getPrintWriter(outputFile);
		printHashtableToPwOrder(table, writer);
		writer.close();
	}
	
	
	public static File fileWithPadding(String filePath, String extension, int n) {		
		int count = 1;
		File f = null;
		do {			
			String padding = ""+count;
			padding = Utility.fillChar(n-padding.length(), '0') + padding;
			f = new File(filePath + padding + extension);
			count++;
		} while(f.exists());
		return f;		
	}
	
	public static File changeExtension(File selectedFile, String ext) {
		String extWithDot = "." + ext;
		String path = selectedFile.getParent();
		String fileName = selectedFile.getName();
		if (fileName.endsWith(extWithDot)) return selectedFile;			
		int dotIndex = fileName.lastIndexOf('.');
		if (dotIndex!=-1) fileName = fileName.substring(0, dotIndex);
		fileName += extWithDot;		
		selectedFile = new File(path + File.separator + fileName);
		//System.out.println(selectedFile);
		return selectedFile;
	}
	
	public static File addExtension(File selectedFile, String ext) {
		String extWithDot = "." + ext;
		String path = selectedFile.getParent();
		String fileName = selectedFile.getName();
		if (fileName.endsWith(extWithDot)) return selectedFile;			
		fileName += extWithDot;		
		selectedFile = new File(path + File.separator + fileName);
		//System.out.println(selectedFile);
		return selectedFile;
	}
	
	public static File postpendAndChangeExtension(File selectedFile, String postpend, String ext) {
		String extWithDot = "." + ext;
		String path = selectedFile.getParent();
		String fileName = selectedFile.getName();
		int dotIndex = fileName.lastIndexOf('.');
		if (dotIndex!=-1) fileName = fileName.substring(0, dotIndex);
		fileName += postpend + extWithDot;		
		selectedFile = new File(path + File.separator + fileName);
		//System.out.println(selectedFile);
		return selectedFile;
	}
	
	public static File appendAndAddExtention(File selectedFile, String toAdd, String ext) {
		String fileName = selectedFile.getName();
		int dotIndex = fileName.indexOf('.'); 
		if (dotIndex!=-1) {
			fileName = fileName.substring(0, dotIndex);
			
		}
		fileName += toAdd;
		String path = selectedFile.getParent();
		selectedFile = new File(path + File.separator + fileName + "." + ext);
		//System.out.println(selectedFile);
		return selectedFile;
	}
	
	public static File appendBeforeExtention(File selectedFile, String toAdd) {
		String originalName = selectedFile.getName();		
		int dotIndex = originalName.lastIndexOf('.');
		String fileName = originalName;
		String extension = "";
		if (dotIndex!=-1) {
			fileName = originalName.substring(0, dotIndex);
			extension = originalName.substring(dotIndex);
		}		
		fileName += toAdd + extension;
		String path = selectedFile.getParent();
		selectedFile = new File(path + File.separator + fileName);
		//System.out.println(selectedFile);
		return selectedFile;
	}
	
	public static String getFileNameWithoutExtensions(File f) {
		String fileName = f.getAbsolutePath();
		int dotIndex = fileName.indexOf('.');
		if (dotIndex==-1 || dotIndex==0) return fileName;
		return fileName.substring(0, dotIndex);
	}
	
	public static String insertStringInFileNameBeforeExtension(File f, String s) throws IOException {
		String fileName = f.getCanonicalPath();
		int dotIndex = fileName.lastIndexOf('.');
		if (dotIndex==-1 || dotIndex==0) return fileName + "." + s;
		return fileName.substring(0, dotIndex) + "." + s + fileName.substring(dotIndex);
	}
	
	public static void append(File inputFile, PrintWriter pwComplete) {
		Scanner scan = getScanner(inputFile);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			pwComplete.println(line);
		}
		scan.close();
	}
	
	// Returns the contents of the file in a byte array. 
	public static byte[] getBytesFromFile(File file) throws IOException { 
		InputStream is = new FileInputStream(file); 
		// Get the size of the file 
		long length = file.length(); 
		// You cannot create an array using a long type. 
		// It needs to be an int type. 
		// Before converting to an int type, check 
		// to ensure that file is not larger than Integer.MAX_VALUE. 
		if (length > Integer.MAX_VALUE) { 
			// File is too large
			System.err.println("Lenght of the file in bytes bigger than " + Integer.MAX_VALUE);
			return null;
		}
		// Create the byte array to hold the data 
		byte[] bytes = new byte[(int)length]; 
		// Read in the bytes 
		int offset = 0; 
		int numRead = 0; 
		while (offset < bytes.length && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) { 
			offset += numRead; 
		} 
		// Ensure all the bytes have been read in 
		if (offset < bytes.length) { 
			throw new IOException("Could not completely read file "+file.getName()); 
		} 
		// Close the input stream and return bytes 
		is.close(); 
		return bytes;		
	}
	
	public static int countNonEmptyLines(File inputFile, String encoding) {
		Scanner scan = getScanner(inputFile, encoding);
		int count = 0;
		while(scan.hasNextLine() && !scan.nextLine().equals("")) {
			count++;
		}
		return count;
	}
	
	public static int countNonEmptyLines(File inputFile) {
		Scanner scan = getScanner(inputFile);
		int count = 0;
		while(scan.hasNextLine() && !scan.nextLine().equals("")) {
			count++;
		}
		return count;
	}
	
	public static long getNumberOfLines(File f) {
		long result = 0;
		Scanner scan = getScanner(f);
		while(scan.hasNextLine()) {
			result++;
			scan.nextLine();
		}		
		scan.close();	
		return result;
	}
	
	public static long getNumberOfColumns(File f, String splitRegExp) {
		long result = 0;
		Scanner scan = getScanner(f);
		while(scan.hasNextLine()) {			
			String line = scan.nextLine();
			return line.split(splitRegExp).length;
		}		
		scan.close();	
		return result;
	}
	
	public static void removeAllFileAndDirInDir(File file) {
		File[] list = file.listFiles();
		for(File f : list) {
			if (f.getName().startsWith(".")) continue;
			if (f.isDirectory())
				removeAllFileAndDirInDir(f);
			f.delete();
		}		
	}
	
	public static boolean areEquals(File file1, File file2) {
		Scanner scan1 = getScanner(file1);
		Scanner scan2 = getScanner(file2);
		while(scan1.hasNextLine()) {
			if (!scan2.hasNextLine()) return false;
			String line1 = scan1.nextLine();
			String line2 = scan2.nextLine();
			if (!line1.equals(line2)) return false;
		}
		if (scan2.hasNextLine()) return false;
		return true;
	}
	
	public static boolean differ(File file1, File file2, String ignoreLineStartingWith) {
		Scanner scan1 = getScanner(file1);
		Scanner scan2 = getScanner(file2);
		while(scan1.hasNextLine()) {
			if (!scan2.hasNextLine()) return true;
			String line1 = scan1.nextLine();
			String line2 = scan2.nextLine();
			if (!line1.equals(line2)) {
				if (!line1.startsWith(ignoreLineStartingWith)) return true;
			}
		}
		if (scan2.hasNextLine()) return true;
		return false;
	}
	
	public static void sortFile(File bibtpar_grammarFile) {
		TreeSet<String> lineSorted = new TreeSet<String>();
		Scanner scan = getScanner(bibtpar_grammarFile);
		while(scan.hasNextLine()) {
			lineSorted.add(scan.nextLine());
		}
		PrintWriter pw = getPrintWriter(bibtpar_grammarFile);
		for(String line : lineSorted) {
			pw.println(line);
		}
		pw.close();
	}
	
	public static void joinFiles(File[] input, File output) {
		PrintWriter pw = getPrintWriter(output);
		for(File f : input) {
			Scanner scan = getScanner(f);
			while(scan.hasNextLine()) {
				String line = scan.nextLine();
				if (line.isEmpty()) continue;
				pw.println(line);
			}
		}
		pw.close();		
	}
	
    public static void changeFileEncoding(File file, String inEncoding, String outEncoding) throws IOException {
        
        File outTmpfile = new File(file.getAbsolutePath() + "~");

        Reader in = new InputStreamReader(new FileInputStream(file), inEncoding);
        Writer out = new OutputStreamWriter(new FileOutputStream(outTmpfile), outEncoding);

        int c;

        while ((c = in.read()) != -1){
            out.write(c);}

        in.close();
        out.close();
        
        outTmpfile.renameTo(file);
        
    }
    
	public static PrintWriter getGzipPrintWriter(File outputFile) {
		PrintWriter writer = null;
	    try
	    {
	        GZIPOutputStream zip = new GZIPOutputStream(new FileOutputStream(outputFile));

	        writer = new PrintWriter(new OutputStreamWriter(zip, "UTF-8"));

	    } catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    return writer;
	}
	
	public static Scanner getGzipScanner(File inputFile) {
		Scanner scanner = null;
	    try
	    {
	        GZIPInputStream zip = new GZIPInputStream(new FileInputStream(inputFile));

	        scanner = new Scanner(new InputStreamReader(zip, "UTF-8"));

	    } catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    return scanner;
	}
 
	
	public static void main(String args[]) throws IOException {
		//File inputFile = new File("Negra/Parsing_Tue_Apr_05_16_30_32_/BITPAR_MPD.mrg"); //new File(args[0]);
		//String inEncoding = "Cp1250"; //args[1];
		//String outEncoding = "UTF-8"; //args[2];
		//changeFileEncoding(inputFile, inEncoding, outEncoding);
		//System.out.println(FileUtil.countNonEmptyLines(inputFile, "Cp1250"));
		//System.out.println(System.getProperty("file.encoding"));
		/*
		File outputFile = new File("CTB/test.txt");
		FileOutputStream out = new FileOutputStream(outputFile, true); 
		OutputStreamWriter pw = new OutputStreamWriter(out,FileUtil.defaultEncoding);
		String[] words = new String[]{"刘华清","会见","泰国","副总理"};
		pw.write(TSNodeLabel.defaultWSJparse(words, "TOP") + "\n");		
		pw.close();
		out.close();
		*/
		System.out.println(dateTimeString());
	}


	


	


	


	


	


	
}
