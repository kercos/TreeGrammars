package tsg.mb;

import java.io.StringReader;

import tsg.TSNodeLabel;
import edu.berkeley.nlp.PCFGLA.Binarization;
import edu.berkeley.nlp.PCFGLA.TreeAnnotations;
import edu.berkeley.nlp.syntax.Tree;
import edu.berkeley.nlp.syntax.Trees;

public class MarkoBinarizationBerkeley extends TreeMarkoBinarization {
		
	Binarization binType; //Binarization.RIGHT; Binarization.LEFT; Binarization.PARENT; Binarization.HEAD;   
	
	public MarkoBinarizationBerkeley(Binarization binType) {
		super();
		this.binType = binType;
	}
	
	@Override
	public TSNodeLabel performMarkovBinarization(TSNodeLabel t) {
		String treeString = t.toString();
		Trees.PennTreeReader reader = new Trees.PennTreeReader(new StringReader(treeString));
	    Tree<String> tree = reader.next();
	    Tree<String> binTree = TreeAnnotations.processTree(tree, markV, markH, binType, false);
	    treeString = binTree.toString();
		try {
			return new TSNodeLabel(treeString);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public TSNodeLabel undoMarkovBinarization(TSNodeLabel t) {
		String treeString = t.toString();
		Trees.PennTreeReader reader = new Trees.PennTreeReader(new StringReader(treeString));
	    Tree<String> tree = reader.next();
	    Tree<String> unbinTree = TreeAnnotations.unAnnotateTree(tree);
	    treeString = unbinTree.toString();
	    try {
			return new TSNodeLabel(treeString);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public String getDescription() {
		return "MarkoBinarizationBerkeley:" + binType;
	}
	
	public static void main(String[] args) throws Exception {
		TSNodeLabel t = new TSNodeLabel("(S (NP (DT the) (JJ quick) (JJ (AA (BB (CC brown)))) (NN fox)) (VP (VBD jumped) (PP (IN over) (NP (DT the) (JJ lazy) (NN dog)))) (. .))");
		TreeMarkoBinarization.markH = 1;
		TreeMarkoBinarization.markV = 2;
		System.out.println(t);
		System.out.println(t.toStringQtree());
		MarkoBinarizationBerkeley mb = new MarkoBinarizationBerkeley(Binarization.LEFT);
		System.out.println(mb.getDescription());
		TSNodeLabel tbin = mb.performMarkovBinarization(t);
		System.out.println(tbin);
		System.out.println(tbin.toStringQtree());
		TSNodeLabel tunbin = mb.undoMarkovBinarization(tbin);
		System.out.println(tunbin);
		System.out.println(tunbin.toStringQtree());
		System.out.println(tunbin.equals(t));		
	}


	
}
