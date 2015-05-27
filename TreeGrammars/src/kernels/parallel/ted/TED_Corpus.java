package kernels.parallel.ted;

import java.io.File;

public class TED_Corpus {
	
	public final static String workingPath = "/Users/fedja/Dropbox/ted_experiment/";
	public final static String annotationPath = workingPath + "annotation/";
	public final static String studentAnnotationPath = annotationPath + "StudentFiles/";
	public final static String studentFeedbackPath = annotationPath + "StudentFeedback/";
	public final static File enSentMTCleanFile = new File(workingPath + "corpus_en_it/train.tags.en-it.clean.tok.lc.en");
	public final static File itSentMTCleanFile = new File(workingPath + "corpus_en_it/train.tags.en-it.clean.tok.lc.it");
	public final static File kernelM2FileEnIt = new File(workingPath + "en_it/kernels/kernels.table.m2.gz");
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
