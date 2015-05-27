package depParsing;

import java.io.IOException;

import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.parser.nndep.DependencyParser;

public class Stanford {
	
	public static void test_NN_Dep_Parser() throws IOException {
		//java -Xmx2G -cp ~/git/TreeGrammars/TreeGrammars/lib/stanford-parser.jar edu.stanford.nlp.parser.nndep.DependencyParser 
		//-model /Volumes/HardDisk/Scratch/CORPORA/UniversalTreebank/langs/it/it-nndep.noEmbedding.model.txt.gz  
		//‑tagger.model /Users/fedja/Work/Code/stanford-postagger-full-2014-01-04/models/italian_UTB1_Coarse_bidir5w  
		//-textFile /Volumes/HardDisk/Scratch/CORPORA/UniversalTreebank/langs/it/it-ud-dev.flat 
		//-outFile /Volumes/HardDisk/Scratch/CORPORA/UniversalTreebank/langs/it/it-ud-dev.conllx.test
		
		//String rootPath = "/gardner0/data/Corpora/UniversalTreebank/langs/it/";
		String rootPath = "/Volumes/HardDisk/Scratch/CORPORA/UniversalTreebank/langs/it/";
		String modelFile = rootPath + "it-nndep.paisa.model.wphl.txt.gz"; //"it-nndep.paisa.model.txt.gz";
		//String tagModelFile = "/Users/fedja/Work/Code/stanford-postagger-full-2014-01-04/models/italian_UTB1_Coarse_bidir5w";
		//String textFile = rootPath + "it-ud-test.flat";
		String testFile = rootPath + "it-ud-test.conllx.wphl"; //goldPos
		//String testFile = rootPath + "it-ud-test.conllx.testPos"; //testPos
		String outputFile = rootPath + "it-ud-test.STANFORD.conllx";
		
		String[] args = new String[]{
				"-model", modelFile,
				//"-tagger.model", tagModelFile,
				//"-textFile", textFile,
				"-testFile", testFile,
				"-outFile", outputFile,				
			};
		
				
		edu.stanford.nlp.parser.nndep.DependencyParser.main(args);
		//String goldConllxFile = rootPath + "it-ud-dev.conllx.clean";
		//evalStanford(outputFile, testFile);
	}
	
	/*
	public static void evalStanford(String inputFileStanfordFile, String goldConnlxFile) throws IOException {
		
		
		DependencyScoring GS = new DependencyScoring(goldConnlxFile, true, false);
		DependencyScoring TS = new DependencyScoring(inputFileStanfordFile, false, false);
		
		List<Set<TypedDependency>> goldDeps = GS.goldDeps;
		List<Set<TypedDependency>> testDeps = TS.goldDeps;
		if (goldDeps.size() != testDeps.size()) {
			System.err.println("Gold and test have different number of sentences: " + 
					Arrays.toString(new int[]{goldDeps.size(),testDeps.size()}));
			return;
		}
		Iterator<Set<TypedDependency>> goldIter = goldDeps.iterator();
		Iterator<Set<TypedDependency>> testIter = testDeps.iterator();
		int total = 0;
		int matched = 0;
		while(goldIter.hasNext()) {
			Set<TypedDependency> gold = goldIter.next();
			Set<TypedDependency> test = testIter.next();
			if (gold.size() != test.size()) {
				System.err.println("Gold and test have different deps in sentence: ");
				System.err.println("gold: " + gold);
				System.err.println("test: " + test);
			}
			return;
		}
		
		
		//List<Collection<TypedDependency>> testDeps = DependencyScoring.readDeps(inputFileStanfordFile);
		
		//Score score = GS.score(testDeps);
		//System.out.println(score.toStringAttachmentScore(false));
		
		//edu.stanford.nlp.trees.EnglishGrammaticalStructure -treeFile testsent.tree -conllx
		
		
	}
	*/
	
	public static void train_NN_Dep_Parser() {		
		
		String rootPath = "/gardner0/data/Corpora/UniversalTreebank/langs/it/";
		//String rootPath = "/Volumes/HardDisk/Scratch/CORPORA/UniversalTreebank/langs/it/";
		
		String devFile = rootPath + "it-ud-dev.conllx.wphl";
		String itTestX = rootPath + "it-ud-test.conllx.wphl";
		String trainFile = rootPath + "it-ud-train.conllx.wphl";
		//String modelFile = rootPath + "it-nndep.noEmbedding.model.txt.gz";
		//String embedFile = null;
		
		String embedFile = rootPath + "paisa.word2vec.size50.txt"; 
		String modelFile = rootPath + "it-nndep.paisa.model.wphl.txt.gz";		
		
		int embedSize = 50;		
		
		String[] args = null;
		
		args = new String[]{
				"-trainFile", trainFile,
				"-devFile", devFile,			
				"-embedFile", embedFile, //<word embedding file> 
				"-embeddingSize", Integer.toString(embedSize), // <word embedding dimensionality>, e.g., 50 
				"-model", modelFile //e.g., nndep.model.txt.gz
			};		
		
		DependencyParser.main(args);		
	}
	
	public static void train_LexParser() {
		//java -mx200m edu.stanford.nlp.parser.lexparser.LexicalizedParser 
		//-retainTMPSubcategories 
		//-outputFormat "wordsAndTags,penn,typedDependencies" 
		//englishPCFG.ser.gz mumbai.txt
		String[] args = null;
		LexicalizedParser.main(args);
	}
	
	
	public static void main(String[] args) throws IOException {		
		//train_NN_Dep_Parser();
		test_NN_Dep_Parser();
		 
	}
	
}
