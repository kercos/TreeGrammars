package tsg.LTSG;

import settings.Parameters;
import tsg.corpora.*;
import tsg.parser.Parser;
import java.io.*;

public class LTSG_Naive extends LTSG{
		
	public static final String Magerman = "Magerman";
	public static final String Collins97 = "Collins97";
	public static final String Collins99 = "Collins99";
	public static final String YM = "YM";
	public static final String Random = "Random";
	public static final String FirstLeft = "FirstLeft";
	public static final String FirstRight = "FirstRight";
	
	public LTSG_Naive() {
		super();
	}
	
	public static void setStartingHead() {
		if (Parameters.LTSGtype.equals(Magerman)) Wsj.initialHeads=1;
		else if (Parameters.LTSGtype.equals(Collins97)) Wsj.initialHeads=2;
		else if (Parameters.LTSGtype.equals(Collins99)) Wsj.initialHeads=3;
		else if (Parameters.LTSGtype.equals(YM)) Wsj.initialHeads=4;
		else if (Parameters.LTSGtype.equals(FirstLeft)) Wsj.initialHeads=5;
		else if (Parameters.LTSGtype.equals(FirstRight)) Wsj.initialHeads=6;
		else if (Parameters.LTSGtype.equals(Random)) Wsj.initialHeads=7;
		else Wsj.initialHeads=0;		
	}
		
	public void assignNaiveAnnotations() {
		if (Wsj.initialHeads>0) return; //already assigned by default
		if (Parameters.LTSGtype.equals(Random)) Parameters.trainingCorpus.assignRandomHeads();
		else if (Parameters.LTSGtype.equals(FirstLeft)) Parameters.trainingCorpus.assignFirstLeftHeads();
		else if (Parameters.LTSGtype.equals(FirstRight)) Parameters.trainingCorpus.assignFirstRightHeads();		
	}
	
	public static void main(String args[]) {				
		Parameters.setDefaultParam();		
		
		//Parameters.lengthLimitTraining = 5;
		//Parameters.lengthLimitTest = 5;
		
		Parameters.LTSGtype = LTSG_Naive.YM; //Collins97 Magerman YM Random FirstLeft FirstRight
		if (args.length>0) Parameters.LTSGtype = args[0];
		LTSG_Naive.setStartingHead();		
		Parameters.naive_iterations = 1;
		Parameters.outputPath = Parameters.resultsPath + "TSG/LTSG/" + Parameters.LTSGtype + "/";
		
		LTSG_Naive Grammar = new LTSG_Naive();
		
		for (int iter=0; iter<Parameters.naive_iterations; iter++) {
			//Grammar.assignNaiveAnnotations();
			Grammar.readTreesFromCorpus();
			//java.io.File templateFile = new java.io.File("/home/fsangati/PROJECTS/TSG/RESULTS/LTSG/Random/Tue_Jun_24_14_57_19/TemplatesFile");
			//Grammar.readTreesFromFile(templateFile);
		}			
		
		Grammar.printTemplatesToFile();
									
		Grammar.treatTreeBank();
		Grammar.toPCFG();
		Grammar.printLexiconAndGrammarFiles();		
		
		new Parser(Grammar);
	}
	

	
}
