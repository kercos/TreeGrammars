package tsg.incremental;

public class CondProbModelFactory {

	static final String modelId_basic = "BASIC"; 
	static final String modelId_basicNorm = "BASIC_NORM";
	static final String modelId_anchor = "ANCHOR"; 
	static final String modelId_anchorNorm = "ANCHOR_NORM";
	
	public static CondProbModel getCondProbModel(String identifier) {
		if (identifier.equals(modelId_basic))
			return getModelBasic(false);
		else if (identifier.equals(modelId_basicNorm))
			return getModelBasic(true);
		else if (identifier.equals(modelId_anchor))
			return getModelAnchor(false);
		else if (identifier.equals(modelId_anchorNorm))
			return getModelAnchor(true);
		return null;
	}
	
	private static CondProbModel getModelBasic(boolean norm) {
		CondProbInit initModel = new CondProbInit_Basic();
		CondProbAttach subBackwardModel = new CondProbAttach_Basic_Backward();
		CondProbAttach subForwardModel = new CondProbAttach_Basic_Forward();
		boolean normalizeAttachemnts = norm;
		String name = normalizeAttachemnts ? modelId_basicNorm : modelId_basic;
		return new CondProbModel(name, initModel, subBackwardModel, 
				subForwardModel, normalizeAttachemnts);
	}
	
	private static CondProbModel getModelAnchor(boolean norm) {
		CondProbInit initModel = new CondProbInit_Basic();
		CondProbAttach subBackwardModel = new CondProbAttach_Anchor_Backward();
		CondProbAttach subForwardModel = new CondProbAttach_Anchor_Forward();
		boolean normalizeAttachemnts = norm;
		String name = (normalizeAttachemnts ? modelId_anchorNorm : modelId_anchor) + 
			"_" + CondProbAttach_Anchor.multFactor;
		return new CondProbModel(name, initModel, subBackwardModel, 
			subForwardModel, normalizeAttachemnts);
	}
	
}
