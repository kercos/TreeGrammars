package util.file;

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
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Scanner;

import org.apache.tools.bzip2.CBZip2InputStream;
import org.apache.tools.bzip2.CBZip2OutputStream;

import util.ArgumentReader;

public class FileUtilBz {

	public static int countNonEmptyLinesBz(File inputFile) throws IOException {
		FileInputStream readTwoBytes=new FileInputStream(inputFile);
		readTwoBytes.read();
		readTwoBytes.read();
		BufferedReader gzipReader = new BufferedReader(
				new InputStreamReader(new CBZip2InputStream(readTwoBytes),"UTF-8"));
		String line = null;
		int count = 0;
		while((line = gzipReader.readLine()) != null) {
			if (!line.equals("")) count++;
		}
		return count;
	}
	
	public static void compress(File inputFile, File outputFile) throws IOException {
		Scanner scan = new Scanner(inputFile);
		FileOutputStream fileStream = new FileOutputStream(outputFile);
		BufferedWriter gzipWriter = new BufferedWriter(
				new OutputStreamWriter(new CBZip2OutputStream(fileStream), "UTF-8"));
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			gzipWriter.write(line + "\n");
		}		
		
		gzipWriter.close();
		fileStream.close();
	}
	
	public static void compressBuffer(File inputFile, File outputFile) throws IOException {		
		FileInputStream inputStream = new FileInputStream(inputFile);
		BufferedReader reader = new BufferedReader(
				new InputStreamReader(inputStream));
		
		FileOutputStream fileStream = new FileOutputStream(outputFile);
		BufferedWriter gzipWriter = new BufferedWriter(
				new OutputStreamWriter(new CBZip2OutputStream(fileStream), "UTF-8"));
		
		int bufSize = 1000;
		char[] cbuf = new char[bufSize];
		int readChars = 0;
		
		while((readChars = reader.read(cbuf)) != -1) {
			if (readChars==bufSize) gzipWriter.write(cbuf);
			else {
				char[] cbufTrunc = Arrays.copyOf(cbuf, readChars);
				gzipWriter.write(cbuf);
			}				
		}
				
		gzipWriter.close();
		fileStream.close();
	}
	
	public static void uncompress(File inputFile, File outputFile, boolean readTwoBytes) throws IOException {
		PrintWriter pw = new PrintWriter(outputFile);
		FileInputStream inputFileStream = new FileInputStream(inputFile);
		
		if (readTwoBytes) {
			inputFileStream.read();
			inputFileStream.read();
		}
		
		BufferedReader gzipReader = new BufferedReader(
				new InputStreamReader(new CBZip2InputStream(inputFileStream),"UTF-8"));
		String line = null;
		while((line = gzipReader.readLine()) != null) {
			pw.println(line);			
		}
		pw.close();
	}
	
	private static void uncompressBuffer(File inputFile, File outputFile, boolean readTwoBytes) throws IOException {
		PrintWriter pw = new PrintWriter(outputFile);		 
		
		FileInputStream inputStream = new FileInputStream(inputFile);
		
		if (readTwoBytes) {
			inputStream.read();
			inputStream.read();
		}
		
		BufferedReader gzipReader = new BufferedReader(
				new InputStreamReader(new CBZip2InputStream(inputStream)));
		
		int bufSize = 1000;
		char[] cbuf = new char[bufSize];
		int readChars = 0;
		
		while((readChars = gzipReader.read(cbuf)) != -1) {
			if (readChars==bufSize) pw.print(cbuf);
			else {
				char[] cbufTrunc = Arrays.copyOf(cbuf, readChars);
				pw.print(cbufTrunc);
			}				
		}
		
		pw.close();
	}
	
	
	public static void main(String[] args) throws IOException {
		int length = args.length; 
		if (length<3) {
			System.err.println("Only accepting 3|4 parameters: -c|-u [-r2b] {inputFile} {outputFile}");
			return;
		}
		
		
			
		String op = args[0];
		
		boolean rdb = false;
		if (length>3) {
			rdb = ArgumentReader.readBooleanOption(args[2]);
		}
		
		File input = new File(args[length-2]);
		File output = new File(args[length-1]);
		
		if (op.equals("-c")) {
			compress(input, output);
			//compressBuffer(input, output); //some problems here 
		}
		else if (op.equals("-u")) {
			uncompress(input, output, rdb);
			//uncompressBuffer(input, output, rdb); //works fine
		}
		else {
			System.err.println("Unknown parameter " + op);
			return;
		}
	}

}
