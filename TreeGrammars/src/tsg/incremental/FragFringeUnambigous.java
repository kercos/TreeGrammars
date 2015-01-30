package tsg.incremental;

import java.util.Arrays;

import tsg.TSNodeLabel;
import tsg.TermLabel;

public class FragFringeUnambigous extends FragFringe {
	
	TSNodeLabel fragment;
	
	public FragFringeUnambigous(TermLabel root, TermLabel[] yield, TSNodeLabel fragment) {
		super(root,yield);
		this.fragment = fragment;		
	}
	
	public FragFringeUnambigous(TSNodeLabel frag) {
		this(frag, false);
		this.fragment = frag;
	}
	
	public FragFringeUnambigous(TSNodeLabel frag, boolean nullary) {
		super(frag,nullary);
		this.fragment = frag;
	}
	
	public FragFringeUnambigous cloneUnambigousFringe() {
		return new FragFringeUnambigous(
				this.root, Arrays.copyOf(this.yield, this.yield.length),fragment);
	}
	
	@Override
	public boolean equals(Object o) {
		if (this==o)
			return true;
		if (o instanceof FragFringeUnambigous) {
			FragFringeUnambigous oFF = (FragFringeUnambigous)o;
			return oFF.root==this.root &&				
				Arrays.equals(oFF.yield, this.yield) &&
				oFF.fragment.equals(this.fragment);
		}
		return false;
	}		
	
}
