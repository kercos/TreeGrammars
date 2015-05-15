package util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.tools.bzip2.CBZip2InputStream;
import org.apache.tools.bzip2.CBZip2OutputStream;

import util.file.FileUtil;


public class Bz2Util {
	

	private static String decompressFromFile(File outputFile) {
		StringBuilder result = new StringBuilder();		 
		
		try {			
			FileInputStream inputStream = new FileInputStream(outputFile);
			inputStream.read();
			inputStream.read();
			BufferedReader gzipReader = new BufferedReader(
					new InputStreamReader(new CBZip2InputStream(inputStream)));
			int bufSize = 1000;
			char[] cbuf = new char[bufSize];
			int readChars = 0;
			while((readChars = gzipReader.read(cbuf)) != -1) {
				if (readChars==1000) result.append(cbuf);
				else {
					char[] cbufTrunc = Arrays.copyOf(cbuf, readChars);
					result.append(cbufTrunc);
				}				
			}
			/*String line = null;
			while((line = gzipReader.readLine()) != null) {
				result.append(line);
			}*/
		} catch(IOException e) {
			e.printStackTrace();
		}
		
		return result.toString();
	}
	
	private static void decompressFromFile(File inputFile, File outputFile) throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(outputFile);		 
		
		try {			
			FileInputStream inputStream = new FileInputStream(outputFile);
			inputStream.read();
			inputStream.read();
			BufferedReader gzipReader = new BufferedReader(
					new InputStreamReader(new CBZip2InputStream(inputStream)));
			int bufSize = 1000;
			char[] cbuf = new char[bufSize];
			int readChars = 0;
			while((readChars = gzipReader.read(cbuf)) != -1) {
				if (readChars==1000) pw.print(cbuf);
				else {
					char[] cbufTrunc = Arrays.copyOf(cbuf, readChars);
					pw.print(cbufTrunc);
				}				
			}
			 
		} catch(IOException e) {
			e.printStackTrace();
		}
		
		pw.close();
	}

	private static void compressInFile(String randomString, File outputFile) {		
		try {
			FileOutputStream fileStream = new FileOutputStream(outputFile);
			BufferedWriter gzipWriter = new BufferedWriter(
					new OutputStreamWriter(new CBZip2OutputStream(fileStream), "UTF-8"));
						
			gzipWriter.write(randomString);
			
			gzipWriter.close();
			fileStream.close();
		} catch (Exception e) {
			System.err.println("Problems while writing bzip2 file");
			e.printStackTrace();
			return;
		}
	}

	private static String getRandomString() {		
		StringBuilder sb = new StringBuilder();
		for(int j=0; j<10001; j++) {
			for(int i=33; i<127; i++) {
				sb.append((char)i);			
			}
		}
		return sb.toString();
	}
	
	public static void main(String[] args) throws FileNotFoundException {
		/*
		File outputFileCompressed = new File("tmp/bz2_tryout.txt.bz2");
		String randomString = getRandomString();
		String readString = decompressFromFile(outputFileCompressed);
		System.out.println(readString.equals(randomString));
		*/
		File inputFile = new File(args[0]);
		File outputFile = new File(args[1]);
		decompressFromFile(inputFile, outputFile);
	}
	
	public static void main1(String[] args) {		
		File outputFileCompressed = new File("tmp/bz2_tryout.bz2");
		File outputFile = new File("tmp/bz2_tryout.txt");
		String randomString = getRandomString();
		FileUtil.append(randomString, outputFile);
		compressInFile(randomString, outputFileCompressed);
		String readString = decompressFromFile(outputFileCompressed);
		System.out.println(readString.equals(randomString));
		System.out.println(randomString);
		System.out.println(readString);
	}	
	
	
}
