package ngram;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import settings.Parameters;
import util.file.FileUtil;

public class IndexBlipp87 implements Comparable<IndexBlipp87> {
	
	short fileIndex;	
	short sentencePosition;
	short wordPosition;
	
	public static File[] filesArray;	
	
	public static void buildFilesArray() {
		
		ArrayList<File> filesList = new ArrayList<File>(); 
		
		File baseOriginalDir = new File(BlippTreebank87.CLEAN_PATH); //CLEAN_PATH_SIMPLE
		File[] yearsDir = baseOriginalDir.listFiles();
		Arrays.sort(yearsDir);
		for(File yDir : yearsDir) {
			String yearDirName = yDir.getName();
			if (!yearDirName.startsWith("1")) // 1987, 1988, 1989
				continue;
			File[] subDirs = yDir.listFiles();
			Arrays.sort(subDirs);			
			for(File singleFile : subDirs) {
				filesList.add(singleFile);
			}			
		}
		
		int size = filesList.size();
		filesArray = filesList.toArray(new File[size]);
		System.out.println("Built array of files (" + size + ")");
		
	}
	
	public IndexBlipp87(short fileIndex, short sentencePosition, short wordPosition) {
		this.fileIndex = fileIndex;
		this.sentencePosition = sentencePosition;
		this.wordPosition = wordPosition;
	}
	
	public String toString() {
		return fileIndex + ":" + sentencePosition + ":" + wordPosition; 
	}

	@Override
	public int compareTo(IndexBlipp87 o) {
		int result = new Short(fileIndex).compareTo(o.fileIndex);
		if (result!=0)
			return result;		
		result = new Short(sentencePosition).compareTo(o.sentencePosition);
		if (result!=0)
			return result;
		return new Short(wordPosition).compareTo(o.wordPosition);
	}
	
	public static void main(String[] args) {
		ArrayList<IndexBlipp87> indexList = new ArrayList<IndexBlipp87>(100);
		for(int i=0; i<100; i++) {
			short fileIndex = (short)(Math.random()*2);	
			short sentencePosition = (short)(Math.random()*3);
			short wordPosition = (short)(Math.random()*5);
			IndexBlipp87 index = new IndexBlipp87(fileIndex, sentencePosition, wordPosition);
			indexList.add(index);
		}
		Collections.sort(indexList);
		for(IndexBlipp87 i : indexList) {
			System.out.println(i);
		}
		
	}
	
	
	
}
