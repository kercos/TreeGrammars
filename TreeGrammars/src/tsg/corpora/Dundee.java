package tsg.corpora;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

import settings.Parameters;
import tsg.TSNodeLabel;
import util.ArgumentReader;
import util.file.FileUtil;
import wordModel.UkWordMapping;
import wordModel.UkWordMappingPetrov;

public class Dundee {

	static final String workPath = "/Users/fedja/Work/Edinburgh/PLTSG/Dudee/";	
	static final File origFile = new File(workPath + "sentsDundee.txt");
	static final File origFileFixed = new File(workPath + "sentsDundee_fixed_wsj.txt");		
	static final File origFileFixedFlatTree = new File(workPath + "sentsDundee_fixed_wsj_flatTree.txt");
	static final File origFileFixedUnk = new File(workPath + "sentsDundee_fixed_wsj_UkM4_UkT4.txt");
	static final File origFileFixedUnkFlatTree = new File(workPath + "sentsDundee_fixed_wsj_UkM4_UkT4_FlatTree.txt");
	
	static final String wsjWorkingPath = Parameters.scratchPath + "/PLTSG/";	
	static final File wsjTrainOrig = new File(wsjWorkingPath + "ROARK_original/wsj-02-21.mrg");
	
	public static void transformToFlatTree(File input, File output) {
		Scanner scan = FileUtil.getScanner(input);
		PrintWriter pw = FileUtil.getPrintWriter(output);
		while(scan.hasNextLine()) {
			String line = scan.nextLine().trim();
			pw.println("(TOP " + line + ")");
		}
		pw.close();
	}
	
	public static void replaceUnknownWords() throws Exception {
		UkWordMapping ukModel = UkWordMapping.getModel("Petrov_Level_4");
		UkWordMapping.ukThreashold = 4;
		ArrayList<TSNodeLabel> trainingTB = Wsj.getTreebank(wsjTrainOrig);
		ArrayList<TSNodeLabel> testTB = Wsj.getTreebank(origFileFixedFlatTree);
		ukModel.init(trainingTB, testTB);
		trainingTB = ukModel.transformTrainingTreebank();
		testTB = ukModel.transformTestTreebank();
		
		File transformedTrainingTB_File = new File(workPath + "wsj-02-21" + "_UkM4_UkT4.txt");
		TSNodeLabel.printTreebankToFileFlat(transformedTrainingTB_File, trainingTB);					
		TSNodeLabel.printTreebankToFileFlat(origFileFixedUnk, testTB);
		
		transformToFlatTree(origFileFixedUnk, origFileFixedUnkFlatTree);
	}
	
	public static void main(String[] args) throws Exception {
		
		//transformToFlatTree(origFileFixed, origFileFixedFlatTree);
		replaceUnknownWords();
	}


	
}
