package tsg.incremental;

import java.util.ArrayList;
import java.util.Arrays;

import tsg.Label;
import tsg.TSNodeLabel;
import tsg.TermLabel;

public class FragFringe {
	
	static final Label nullaryLabel = Label.getLabel("_");
	static final TermLabel nullaryNode = TermLabel.getTermLabel("_", true);
	
	static final Label STOPlabel = Label.getLabel("||STOP||");
	static final TermLabel STOPnodeLex = TermLabel.getTermLabel(STOPlabel, true);
	static final TermLabel STOPnodeRoot = TermLabel.getTermLabel(STOPlabel, false);

	
	public TermLabel root;
	public TermLabel[] yield;
	
	public FragFringe(TermLabel root, TermLabel[] yield) {
		this.root = root;
		this.yield = yield;
	}
	
	public static FragFringe getStopFringe(Label firstSub) {
		TermLabel firstSubTL = TermLabel.getTermLabel(firstSub, false);
		TermLabel[] yield = new TermLabel[] {firstSubTL, STOPnodeLex};
		return new FragFringe(STOPnodeRoot, yield);
	}
	
	public static TSNodeLabel getStopFragment(Label firstSub) {
		TSNodeLabel root = new TSNodeLabel(STOPlabel, false);		
		TSNodeLabel sub = new TSNodeLabel(firstSub, false);
		TSNodeLabel lex = new TSNodeLabel(STOPlabel, true);
		root.assignDaughters(new TSNodeLabel[]{sub, lex});
		return root;
	}
	
	public boolean firstTerminalIsLexical() {
		return yield[0].isLexical;
	}

	
	public FragFringe(TSNodeLabel frag) {
		this(frag, false);
	}
	
	public FragFringe(TSNodeLabel frag, boolean nullary) {
		root = TermLabel.getTermLabel(frag);
		ArrayList<TSNodeLabel> terms = frag.collectTerminalItems();
		if (!nullary) {						
			int length = terms.size();
			yield = new TermLabel[length];
			int i=0;
			for(TSNodeLabel term : frag.collectTerminalItems()) {
				yield[i++] = TermLabel.getTermLabel(term);
			}
		}
		else {
			int countNullary = 0;
			for(TSNodeLabel t : terms) {
				if (t.label==nullaryLabel)
					countNullary++;
			}
			int length = terms.size()-countNullary;
			yield = new TermLabel[length];
			int i=0;
			for(TSNodeLabel term : frag.collectTerminalItems()) {
				if (term.label==nullaryLabel)
					continue;
				yield[i++] = TermLabel.getTermLabel(term);
			}
		}
	}
	
	public FragFringe cloneFringe() {
		return new FragFringe(this.root, Arrays.copyOf(this.yield, this.yield.length));
	}
	
	public TermLabel root() {
		return root;
	}
	
	public TermLabel secondTerminal() {
		return yield[1]; 
	}

	public TermLabel firstTerminal() {
		return yield[0];
	}

	@Override
	public int hashCode() {
		return 31 * Arrays.hashCode(yield) + root.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (this==o)
			return true;
		if (o instanceof FragFringe) {
			FragFringe oFF = (FragFringe)o;
			return oFF.root==this.root &&				
				Arrays.equals(oFF.yield, this.yield);
		}
		return false;
	}		
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(root);
		sb.append(" <");
		for(TermLabel s : yield) {
			sb.append(" " + s);
		}
		return sb.toString();
	}

	public String toStringTex() {
		StringBuilder sb = new StringBuilder();
		sb.append("$" + root.toStringTex() + "$ & $\\yields$ & ");
		sb.append("$ ");
		for(TermLabel t : yield) {
			sb.append(t.toStringTex() + " \\;\\; ");
		}
		sb.append("$ ");
		return sb.toString();
	}

	public void setInYield(int i, TermLabel w) {
		yield[i] = w;
		
	}

	public TermLabel firstLexNonNullary() {
		for (TermLabel t : yield) {
			if (t.isLexical && t!=nullaryNode)
				return t;
		}
		return null;
	}
	
	public TermLabel firstLex() {
		for (TermLabel t : yield) {
			if (t.isLexical)
				return t;
		}
		return null;
	}

	
	public boolean isNullaryFragFringe() {
		if (this.yield.length>2) return false;
		TermLabel firstTerm = this.firstTerminal();
		if (firstTerm.isLexical)
			return firstTerm==nullaryNode;
		return this.secondTerminal()==nullaryNode;
	}
	
	public boolean isStopFragFringe() {
		return this.root==STOPnodeRoot;
	}
	
	public static boolean isStopFrag(TSNodeLabel f) {
		return f.label==STOPlabel;
	}

	public ArrayList<Label> collectLexLabels() {
		ArrayList<Label> result = new ArrayList<Label>();
		for (TermLabel t : yield) {
			if (t.isLexical)
				result.add(t.label);
		}
		return result;		
	}
}
