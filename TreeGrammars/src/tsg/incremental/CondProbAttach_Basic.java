package tsg.incremental;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map.Entry;

import tsg.TermLabel;
import util.Utility;
import util.file.FileUtil;

public abstract class CondProbAttach_Basic extends CondProbAttach {

	HashMap<TermLabel, HashMap<TermLabel, HashMap<FragFringeUnambigous, double[]>>> table;

	// indexed on subSite and first lex
	// reversed after estimateLogProbabilities

	public CondProbAttach_Basic() {
		table = new HashMap<TermLabel, HashMap<TermLabel, HashMap<FragFringeUnambigous, double[]>>>();
	}

	@Override
	public int[] totalEvents() {
		int totalEvents = 0;
		for (HashMap<TermLabel, HashMap<FragFringeUnambigous, double[]>> subTable : table.values()) {
			for (HashMap<FragFringeUnambigous, double[]> subSubTable : subTable.values()) {
				totalEvents += subSubTable.size();
			}
		}
		return new int[] { totalEvents };
	}

	/**
	 * To run after training is finished
	 */
	public void estimateLogProbabilities() {
		for (HashMap<TermLabel, HashMap<FragFringeUnambigous, double[]>> subTable : table.values()) {
			double totalSum = Utility.getSumValueDouble(subTable);
			for (HashMap<FragFringeUnambigous, double[]> subSubTable : subTable.values()) {
				for (double[] d : subSubTable.values()) {
					d[0] = Math.log(d[0] / totalSum);
				}
			}
		}
		table = Utility.invertHashMapDouble(table);
	}

	public abstract TermLabel getCondHistory(SymbolicChartState a);

	public abstract TermLabel getCondHistoryBasic(FragFringeUnambigous a);

	@Override
	public void addTrainEvent(SymbolicChartState a, FragFringeUnambigous b, double d) {
		TermLabel firstKey = getCondHistory(a);
		TermLabel secondKey = b.firstTerminal();
		Utility.increaseHashMapTriple(table, firstKey, secondKey, b, new double[] { d });
	}

	@Override
	public void addTrainEventBasic(FragFringeUnambigous a) {
		TermLabel firstKey = getCondHistoryBasic(a);
		TermLabel secondKey = a.firstTerminal();				
		Utility.putInHashMapIfNotPresent(table, firstKey,secondKey, a, new double[] { 1d });
	}

	@Override
	public void addAllTrainingEvents(CondProbAttach attachModel) {
		CondProbAttach_Basic otherModel = (CondProbAttach_Basic) attachModel;
		Utility.increaseAllHashMapTriple(otherModel.table, table);
	}

	@Override
	public CondProbAttachLex getConProbLex(TermLabel lex) {
		return new CondProbAttachLex_Basic(table.get(lex));
	}

	public class CondProbAttachLex_Basic extends CondProbAttachLex {

		HashMap<TermLabel, HashMap<FragFringeUnambigous, double[]>> lexTable;

		public CondProbAttachLex_Basic(HashMap<TermLabel, HashMap<FragFringeUnambigous, double[]>> lexTable) {
			this.lexTable = lexTable;
		}

		@Override
		public HashMap<FragFringeUnambigous, double[]> getEventsLogProb(SymbolicChartState a) {
			TermLabel firstKey = getCondHistory(a);
			return lexTable.get(firstKey);
		}

		@Override
		public boolean hasEvents() {
			return lexTable != null;
		}

		@Override
		public void printToFile(File outputFile) {
			PrintWriter pw = FileUtil.getPrintWriter(outputFile);
			for (Entry<TermLabel, HashMap<FragFringeUnambigous, double[]>> e : lexTable.entrySet()) {
				TermLabel subSite = e.getKey();
				pw.println(subSite);
				HashMap<FragFringeUnambigous, double[]> subTable = e.getValue();
				for (Entry<FragFringeUnambigous, double[]> f : subTable.entrySet()) {
					FragFringeUnambigous ff = f.getKey();
					double d = f.getValue()[0];
					pw.println("\t" + ff.toString() + "\t" + d);
				}
			}
			pw.close();
		}

	}

}
