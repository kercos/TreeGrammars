package tsg.incremental;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import tsg.TermLabel;
import util.Utility;

public class ProbBox {

	// ( viterbi best previous state )
	
	double innerMargProb, forwardMargProb;
	Double outerMargProb;
	double innerViterbiProb; // what Stolke defines viterbi
	// ( it represents the prob. of the best path from the time the state
	// was created)
	double forwardViterbiProb;
	// ( it represents the prob. of the best path from the start operation)
	ProbChartState previousState;

	// ( viterbi best previous state )

	public ProbBox(double innerProb, double forwardProb, double innerViterbiProb, 
			double forwardViterbiProb, ProbChartState previousState) {
		this.innerMargProb = innerProb;
		this.forwardMargProb = forwardProb;
		this.innerViterbiProb = innerViterbiProb;
		this.forwardViterbiProb = forwardViterbiProb;
		this.previousState = previousState;
		this.outerMargProb = Double.NaN;
	}

	/**
	 * 
	 * @return the ProbBox which is introduced or modified as the result of
	 *         this operation (null if the the table is not modified)
	 */
	
	public static boolean addProbState(HashMap<TermLabel, HashMap<ProbChartState, 
			ProbBox>> table, TermLabel firstKey, ProbChartState currentState, 
			double currentInnerProb, double currentForwardProb, 
			double currentInnerViterbiProb, double currentForwardViterbiProb, 
			ProbChartState currentPreviousState) {
	
		HashMap<ProbChartState, ProbBox> subTable = table.get(firstKey);
		if (subTable == null) {
			subTable = new HashMap<ProbChartState, ProbBox>();
			table.put(firstKey, subTable);
			ProbBox newPb = new ProbBox(currentInnerProb, currentForwardProb, currentInnerViterbiProb, currentForwardViterbiProb, currentPreviousState);
			subTable.put(currentState, newPb);
			currentState.probBox = newPb;
			return true;
		}
	
		ProbBox probPresent = subTable.get(currentState);
		if (probPresent == null) {
			ProbBox pb = new ProbBox(currentInnerProb, currentForwardProb, currentInnerViterbiProb, currentForwardViterbiProb, currentPreviousState);
			subTable.put(currentState, pb);
			currentState.probBox = pb;
			return true;
		}
		probPresent.innerMargProb = Utility.logSum(probPresent.innerMargProb, currentInnerProb);
		probPresent.forwardMargProb = Utility.logSum(probPresent.forwardMargProb, currentForwardProb);
		if (probPresent.innerViterbiProb < currentInnerViterbiProb) {
			probPresent.innerViterbiProb = currentInnerViterbiProb;
			assert (probPresent.forwardViterbiProb - currentForwardViterbiProb < 0.00000001);
			probPresent.forwardViterbiProb = currentForwardViterbiProb;
			probPresent.previousState = currentPreviousState;
			currentState.probBox = probPresent;
			return true;
		}
		assert (currentForwardViterbiProb - probPresent.forwardViterbiProb < 0.00000001);
	
		currentState.probBox = probPresent;
		return false;
	}

	public double inXout() {
		return innerMargProb + outerMargProb;
	}

	public static <S, T> int countTotalDoubleAlive(HashMap<S, HashMap<T, ProbBox>> table) {
		int count = 0;
		for (HashMap<T, ProbBox> set : table.values()) {
			for (ProbBox pb : set.values()) {
				if (!pb.outerMargProb.isNaN())
					count++;
			}
		}
		return count;
	}

	public static <S, T> int countTotalDouble(HashMap<S, HashMap<T, ProbBox>> table) {
		int count = 0;
		for (HashMap<T, ProbBox> set : table.values()) {
			count += set.size();
		}
		return count;
	}

	public static <T, C> Entry<C, ProbBox> getMaxViterbiEntryDouble(HashMap<T, HashMap<C, ProbBox>> doubleTable) {

		double max = 0;
		Entry<C, ProbBox> result = null;
		for (Entry<T, HashMap<C, ProbBox>> e : doubleTable.entrySet()) {
			Entry<C, ProbBox> bestSubEntry = getMaxInnerViterbiEntry(e.getValue());
			double viterbi = bestSubEntry.getValue().innerViterbiProb;
			if (result == null || viterbi > max) {
				max = viterbi;
				result = bestSubEntry;
			}
		}
		return result;
	}

	public static <C> Entry<C, ProbBox> getMaxInnerViterbiEntry(HashMap<C, ProbBox> table) {

		Iterator<Entry<C, ProbBox>> e = table.entrySet().iterator();
		Entry<C, ProbBox> result = e.next();
		double max = result.getValue().innerViterbiProb;
		while (e.hasNext()) {
			Entry<C, ProbBox> next = e.next();
			double viterbi = next.getValue().innerViterbiProb;
			if (viterbi > max) {
				max = viterbi;
				result = next;
			}
		}
		return result;
	}

	public static <T, C> Entry<C, ProbBox> getMaxForwardViterbiEntryDouble(HashMap<T, HashMap<C, ProbBox>> doubleTable) {

		if (doubleTable.isEmpty())
			return null;

		Iterator<Entry<T, HashMap<C, ProbBox>>> e = doubleTable.entrySet().iterator();
		Entry<C, ProbBox> result = getMaxForwardViterbiEntry(e.next().getValue());
		double max = result.getValue().forwardViterbiProb;

		while (e.hasNext()) {
			Entry<C, ProbBox> bestSubEntry = getMaxForwardViterbiEntry(e.next().getValue());
			double viterbi = bestSubEntry.getValue().forwardViterbiProb;
			if (viterbi > max) {
				max = viterbi;
				result = bestSubEntry;
			}
		}
		return result;
	}

	public static <T, C> double getMaxForwardViterbiDouble(HashMap<T, HashMap<C, ProbBox>> doubleTable) {

		if (doubleTable.isEmpty())
			return Double.NEGATIVE_INFINITY;

		Iterator<Entry<T, HashMap<C, ProbBox>>> e = doubleTable.entrySet().iterator();
		Entry<C, ProbBox> result = getMaxForwardViterbiEntry(e.next().getValue());
		double max = result.getValue().forwardViterbiProb;

		while (e.hasNext()) {
			Entry<C, ProbBox> bestSubEntry = getMaxForwardViterbiEntry(e.next().getValue());
			double viterbi = bestSubEntry.getValue().forwardViterbiProb;
			if (viterbi > max) {
				max = viterbi;
				result = bestSubEntry;
			}
		}
		return max;
	}

	public static <C> Entry<C, ProbBox> getMaxForwardViterbiEntry(HashMap<C, ProbBox> table) {

		Iterator<Entry<C, ProbBox>> e = table.entrySet().iterator();
		Entry<C, ProbBox> result = e.next();
		double max = result.getValue().forwardViterbiProb;
		while (e.hasNext()) {
			Entry<C, ProbBox> next = e.next();
			double viterbi = next.getValue().forwardViterbiProb;
			if (viterbi > max) {
				max = viterbi;
				result = next;
			}
		}
		return result;
	}

	public static <C> double[] getTotalSumInnerMargProbForwardMargProb(HashMap<C, ProbBox> table) {
		Iterator<ProbBox> iter = table.values().iterator();
		ProbBox pb = iter.next();
		double innerMax = pb.innerMargProb;
		double forwardMax = pb.innerMargProb;
		int size = table.size() - 1;
		double[] innerOther = new double[size];
		double[] forwardOther = new double[size];
		int i = 0;
		while (iter.hasNext()) {
			pb = iter.next();
			double nextInner = pb.innerMargProb;
			if (nextInner > innerMax) {
				innerOther[i] = innerMax;
				innerMax = nextInner;
			} else
				innerOther[i] = nextInner;
			double nextForward = pb.forwardMargProb;
			if (nextForward > forwardMax) {
				forwardOther[i] = forwardMax;
				forwardMax = nextForward;
			} else
				forwardOther[i] = nextForward;
			i++;
		}
		double logSumInnerProb = Utility.logSum(innerOther, innerMax);
		double logSumForwardProb = Utility.logSum(forwardOther, forwardMax);
		return new double[] { logSumInnerProb, logSumForwardProb };
	}

	public static <C> double getTotalSumForwardMargProb(HashMap<C, ProbBox> table) {

		Iterator<ProbBox> iter = table.values().iterator();
		ProbBox pb = iter.next();
		double forwardMax = pb.forwardMargProb;
		int size = table.size() - 1;
		double[] forwardOther = new double[size];
		int i = 0;
		while (iter.hasNext()) {
			pb = iter.next();
			double nextForward = pb.forwardMargProb;
			if (nextForward > forwardMax) {
				forwardOther[i] = forwardMax;
				forwardMax = nextForward;
			} else
				forwardOther[i] = nextForward;
			i++;
		}
		return Utility.logSum(forwardOther, forwardMax);
	}

	public static <C> double getTotalSumInxOutProb(HashMap<C, ProbBox> table) {

		Iterator<ProbBox> iter = table.values().iterator();
		ProbBox pb = iter.next();
		double inXoutMax = pb.inXout();
		int size = table.size() - 1;
		double[] inXoutOther = new double[size];
		int i = 0;
		while (iter.hasNext()) {
			pb = iter.next();
			double nextInXout = pb.inXout();
			if (nextInXout > inXoutMax) {
				inXoutOther[i] = inXoutMax;
				inXoutMax = nextInXout;
			} else
				inXoutOther[i] = nextInXout;
			i++;
		}
		return Utility.logSum(inXoutOther, inXoutMax);
	}

	public static <C> double getTotalSumInnerMargProb(HashMap<C, ProbBox> table) {

		Iterator<ProbBox> iter = table.values().iterator();
		ProbBox pb = iter.next();
		double innerMax = pb.innerMargProb;
		int size = table.size() - 1;
		double[] innerOthers = new double[size];
		int i = 0;
		while (iter.hasNext()) {
			pb = iter.next();
			double nextInner = pb.innerMargProb;
			if (nextInner > innerMax) {
				innerOthers[i] = innerMax;
				innerMax = nextInner;
			} else
				innerOthers[i] = nextInner;
			i++;
		}
		return Utility.logSum(innerOthers, innerMax);
	}

	public static <C> double getTotalSumForwardMargProbDouble(HashMap<TermLabel, HashMap<C, ProbBox>> statesTable) {

		int size = ProbBox.countTotalDouble(statesTable) - 1;

		if (size == -1) {
			// zero elements
			return Double.NEGATIVE_INFINITY;
		}

		if (size == 0) {
			// only one element
			return statesTable.values().iterator().next().values().iterator().next().forwardMargProb;
		}

		double innerMargMax = -Double.MAX_VALUE;
		double[] innerMargOthers = new double[size];
		boolean first = true;

		int i = 0;
		for (HashMap<C, ProbBox> subTable : statesTable.values()) {
			for (ProbBox pb : subTable.values()) {
				double nextInnerMarg = pb.forwardMargProb;
				if (first) {
					first = false;
					innerMargMax = nextInnerMarg;
					continue;
				}
				if (nextInnerMarg > innerMargMax) {
					innerMargOthers[i++] = innerMargMax;
					innerMargMax = nextInnerMarg;
				} else
					innerMargOthers[i++] = nextInnerMarg;
			}
		}
		return Utility.logSum(innerMargOthers, innerMargMax);
	}

	public static <C> double getTotalSumInnerOuterMargProb(HashMap<TermLabel, HashMap<C, ProbBox>> statesTable) {

		int size = // Utility.countTotalDouble(statesTable) - 1;
		ProbBox.countTotalDoubleAlive(statesTable) - 1;

		double inXoutMax = -Double.MAX_VALUE;
		double[] inXoutOthers = new double[size];
		boolean first = true;

		int i = 0;
		for (HashMap<C, ProbBox> subTable : statesTable.values()) {
			for (ProbBox pb : subTable.values()) {
				if (pb.outerMargProb.isNaN())
					continue;
				double nextInXout = pb.inXout();
				if (first) {
					first = false;
					inXoutMax = nextInXout;
					continue;
				}
				if (nextInXout > inXoutMax) {
					inXoutOthers[i++] = inXoutMax;
					inXoutMax = nextInXout;
				} else
					inXoutOthers[i++] = nextInXout;
			}
		}
		return Utility.logSum(inXoutOthers, inXoutMax);
	}

	public void logSumOuter(double outerContribution) {
		outerMargProb = Utility.logSum(outerMargProb, outerContribution);
	}

	public String toStringForChart(boolean normalProb) {
		// double viterbi = normalProb ? Math
		// .exp(innerViterbiProb) : innerViterbiProb;
		// double totVit = normalProb ? Math
		// .exp(forwardViterbiProb)
		// : forwardViterbiProb;
		// String vitString = Float.toString((float) viterbi);
		// String totVitString = Float.toString((float) totVit);
		// return " [" + vitString + ", " + totVitString + "] ";
		double margForward = normalProb ? Math.exp(forwardMargProb) : forwardMargProb;
		double margIn = normalProb ? Math.exp(innerMargProb) : innerMargProb;
		double margOut = normalProb ? Math.exp(outerMargProb) : outerMargProb;
		String forwardString = Float.toString((float) margForward);
		String inString = Float.toString((float) margIn);
		String outString = Float.toString((float) margOut);
		return " [" + forwardString + ", " + inString + ", " + outString + "] ";
	}

}
