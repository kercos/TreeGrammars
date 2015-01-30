package tdg;

import java.io.File;
import java.io.PrintWriter;
import java.util.*;

import tdg.corpora.MstSentenceUlab;
import util.Utility;
import util.file.FileUtil;

public class TDNodeTransformation {

	static String mstSentence =
	//	"Questa	e'	una	Repubblica	indipendente	,	urlano	i	valonesi	trincerati	dietro	carcasse	di	auto	rovesciate	,	bidoni	e	travi	di	legno	affastellate	per	bloccare	la	strada	.\n"+
	//	"PRON	VERB	ART	NOUN	ADJ	PUNCT	VERB	ART	NOUN	VERB	PREP	NOUN	PREP	NOUN	VERB	PUNCT	NOUN	CONJ	NOUN	PREP	NOUN	VERB	PREP	VERB	ART	NOUN	PUNCT\n"+
	//	"2	7	2	3	4	7	0	7	8	9	10	11	12	13	14	12	16	17	18	19	20	19	22	23	24	25	7\n";
		"Piovono	pietre	e	insulti	,	anche	contro	gli	stranieri	e	gli	italiani	.\n"+
		"VERB	NOUN	CONJ	NOUN	PUNCT	ADV	PREP	ART	NOUN	CONJ	ART	NOUN	PUNCT\n"+
		"0	1	2	3	1	7	1	7	8	8	10	11	1";
	
	public static void main(String[] args) {
		File outputFile = new File("tmp/transforms.txt");
		File outputFile1 = new File("tmp/transforms1.txt");
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		PrintWriter pw1 = FileUtil.getPrintWriter(outputFile1);
		TDNode t = TDNode.from(mstSentence, TDNode.FORMAT_MST);
		pw.println(t.toStringMSTulab(false)+ "\n");
		pw1.println(t.toStringMSTulab(false)+ "\n");
		
		ArrayList<TDNode> variations = t.oneStepVariations();		
		for(TDNode v : variations) {
			pw.println(v.toStringMSTulab(false)+ "\n");
		}
		String[] mstSentenceSplit = mstSentence.split("\n");
		MstSentenceUlab s = new MstSentenceUlab(
				mstSentenceSplit[0].split("\t"),
				mstSentenceSplit[1].split("\t"),
				Utility.parseIndexList(mstSentenceSplit[2].split("\t"))
				);
		ArrayList<MstSentenceUlab> variations1 = s.getOneStepVariation();
		for(MstSentenceUlab v : variations1) {
			pw1.println(v.toString() + "\n");
		}
		pw.close();
		pw1.close();
	}

	
}
