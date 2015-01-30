package tsg.mwe;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Scanner;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import util.StringsAlignment;
import util.Utility;

public class CorpusElex {

	static final String path = "/Volumes/HardDisk/Scratch/CORPORA/MWE_Datasets/elex1.1/lexdata/";
	static final File dtdFile = new File(path + "tstlex.dtd");
	static final File elexXmlFile = new File(path + "elex-mw.xml");
	static final File outputFile = new File(path + "elex-mw_LengthPOSmwe.txt");
	static HashMap<Integer, int[]> lengthStats = new HashMap<Integer, int[]>();
	
	public static void extractExpressions() throws SAXException, IOException, ParserConfigurationException {
		
		PrintWriter pw = new PrintWriter(outputFile);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(elexXmlFile);
		
		//optional, but recommended
		//read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
		doc.getDocumentElement().normalize();
		
		//System.out.println("Root element :" + doc.getDocumentElement().getNodeName());
		
		int countMWE=0;
		NodeList nList = doc.getElementsByTagName("entry");
		for (int temp = 0; temp < nList.getLength(); temp++) {
			Node nNode = nList.item(temp);
			
			//System.out.println("\nCurrent Element :" + nNode.getNodeName());
			 
			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
				
				countMWE++;
				Element eElement = (Element) nNode;
	 				
				//Orthographic representation of the multi-word expression
				String orth = eElement.getElementsByTagName("orth").item(0).getTextContent();
				//System.out.println("Orth : " + orth);
				
				int length = orth.split("\\s").length;
				Utility.increaseInHashMap(lengthStats, length);
				
				//The part-of-speech of a multi-word expression where from a grammatical point of view the full expression can be regarded as one word.
				String mpos = eElement.getElementsByTagName("mpos").item(0).getTextContent();
				
				//System.out.println("MPOS : " + mpos);
				/*
				ADJ: adjectief (= adjective)
				BW: bijwoord (= adverb)
				LID: lidwoord (= article)
				N: substantief (= noun)
				SPEC(afgebr): code that is used exclusively in the lexicon for parts of contracted multi-word expressions (eg 'in- en uitvoer')
				SPEC(deeleigen): code for part of a multi-word proper name
				SPEC(meta): code for a word mention
				SPEC(onverst): code for an incomprehensible utterance
				SPEC(vreemd): code for an utterance in a foreign language, or for an originally foreign part of a multi- word expression that in itself has not been assimilated
				TSW: tussenwerpsel (= interjection)
				TW: telwoord (= numeral)
				VG: voegwoord (= conjunction)
				VNW: voornaamwoord (= pronoun)
				VZ: voorzetsel (= preposition)
				WW: werkwoord (= verb)
				COMB(eigen): code for compound proper name or title to which no specific attributes like gender or number have been assigned
				SPEC(samentr): code for the complex (multi-word) contraction da's (= that's) which cannot be assigned to any of the common parts-of-speech
	 			*/
				
				pw.println(length + "\t" + mpos + "\t" + orth);
			}
			
			//break;
		}
		
		pw.close();
		
		System.out.println("Total MWE: " + countMWE);
		System.out.println("Length stats: ");
		TreeSet<Integer> lengthSet = new TreeSet<Integer>(lengthStats.keySet());
		for(int l : lengthSet) {
			System.out.println(l + "\t" + lengthStats.get(l)[0]);
		}
				
	}
	

	
	public static void main(String[] args) throws SAXException, IOException, ParserConfigurationException {
		extractExpressions();
	}
	
}
