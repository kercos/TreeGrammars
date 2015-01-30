package tsg.parsingExp;

import tsg.TSNode;
import tsg.corpora.Tiger2;
import util.*;
import util.file.FileUtil;
import util.file.UtilXml;

import java.io.*;
import java.util.Hashtable;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class ParseTiger_old {

	public static File corpusXmlShort = Tiger2.TigerTBxmlShort;
	public static File corpusXml = Tiger2.TigerTBxml;
	public static File corpusPennShort = Tiger2.TigerTBPennShort;
	public static File corpusPenn = Tiger2.TigerTBPenn;
	public static String headLabel = "HD";
	public static int corpusSize = 50474;
	public static int corpusShortSize = 4;
	

	
	public static void tigerXmlToPenn(File inputFile, File outputFile, int size, boolean headMarks){
		PrintWriter out = FileUtil.getPrintWriter(outputFile);
		Document dom = UtilXml.getDOM_xml(inputFile);		
		//get the root elememt
		Element docEle = dom.getDocumentElement();
		
		//get a nodelist of <employee> elements
		NodeList nl = docEle.getElementsByTagName("s");
		//if(nl != null && nl.getLength() > 0) {
		for(int i = 0 ; i < size;i++) {			
			//get the employee element
			Element s = (Element)nl.item(i);
			String id = s.getAttribute("id");
			TSNode TSNsentence = parserSentence(s, headMarks);
			out.println(TSNsentence.toString(headMarks, false));				
		}
		out.close();
	}


	private static TSNode parserSentence(Element s, boolean headMarks) {
		
		Hashtable<String, TSNode> idNode = new Hashtable<String, TSNode>(); 
		
		NodeList terminals = s.getElementsByTagName("t");
		for(int i = 0 ; i < terminals.getLength(); i++) {
			Element t = (Element)terminals.item(i);
			String id = t.getAttribute("id");
			String word = t.getAttribute("word");
			String pos = t.getAttribute("pos");
			TSNode lexical = TSNode.TSNodeLexical(word);
			TSNode posNode = new TSNode(pos, new TSNode[]{lexical});
			idNode.put(id, posNode);
		}
		
		NodeList nodes = s.getElementsByTagName("nt");
		for(int i = 0 ; i < nodes.getLength(); i++) {
			Element n = (Element)nodes.item(i);
			String id = n.getAttribute("id");
			String cat = n.getAttribute("cat");
			NodeList edges = n.getElementsByTagName("edge");
			TSNode[] daughters = new TSNode[edges.getLength()];
			for(int j = 0 ; j < edges.getLength(); j++) {
				Element e = (Element)edges.item(j);
				String idref = e.getAttribute("idref");				
				daughters[j] = idNode.get(idref);
				if (headMarks) {
					String edgeLabel = e.getAttribute("label");
					if (edgeLabel.equals(headLabel)) daughters[j].headMarked = true;
				}
			}
			TSNode catNode = new TSNode(cat, daughters);
			if (i == nodes.getLength()-1) return catNode;
			idNode.put(id, catNode);
		}
		
		return null;
	}

	/**
	 * I take a xml element and the tag name, look for the tag and get
	 * the text content 
	 * i.e for <employee><name>John</name></employee> xml snippet if
	 * the Element points to employee node and tagName is name I will return John  
	 * @param ele
	 * @param tagName
	 * @return
	 */
	private String getTextValue(Element ele, String tagName) {
		String textVal = null;
		NodeList nl = ele.getElementsByTagName(tagName);
		if(nl != null && nl.getLength() > 0) {
			Element el = (Element)nl.item(0);
			textVal = el.getFirstChild().getNodeValue();
		}

		return textVal;
	}
	

	
	public static void main(String[] args){
		tigerXmlToPenn(corpusXml, corpusPenn, corpusSize, true);
	}

}
