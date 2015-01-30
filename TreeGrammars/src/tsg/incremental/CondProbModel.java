package tsg.incremental;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import settings.Parameters;
import tsg.TermLabel;
import tsg.incremental.CondProbAttach.CondProbAttachLex;

public class CondProbModel implements Serializable {

	private static final long serialVersionUID = 1L;
	
	String modelName;
	CondProbInit initModel;
	CondProbAttach subBackwardModel;
	CondProbAttach subForwardModel;
	boolean normalizeAttachemnts;
	
	public CondProbModel(String modelName,
			CondProbInit initModel, 
			CondProbAttach subBackwardModel, 
			CondProbAttach subForwardModel,
			boolean normalizeAttachemnts) {		
		
		this.modelName = modelName;
		this.initModel = initModel;
		this.subBackwardModel = subBackwardModel;
		this.subForwardModel = subForwardModel;
		this.normalizeAttachemnts = normalizeAttachemnts;
	}
	
	public String idString() {
		return modelName;
	}
	
	public void writeToFile(File outputFile) {		
		 try{
			 
			   FileOutputStream fin = new FileOutputStream(outputFile);
			   ObjectOutputStream ois = new ObjectOutputStream(fin);
			   ois.writeObject(this);			   
			   ois.close();	 
		   }catch(Exception ex){
			   ex.printStackTrace();
		   } 
	}
	
	public static CondProbModel readFromFile(File inputFile) {	
		try {
			FileInputStream fin = new FileInputStream(inputFile);
			ObjectInputStream ois = new ObjectInputStream(fin);
			return (CondProbModel) ois.readObject();
		} catch (Exception ex) {
			ex.printStackTrace();
		} 
		return null;
	}
	
	public void addAllTrainingEvents(CondProbModel source) {
		initModel.addAllTrainingEvents(source.initModel);
		subBackwardModel.addAllTrainingEvents(source.subBackwardModel);
		subForwardModel.addAllTrainingEvents(source.subForwardModel);
	}
	
	public void addTrainEventInit(FragFringeUnambigous a) {
		initModel.addTrainEvent(a, 1d);
	}
	
	public void addTrainEventInit(FragFringeUnambigous a, double d) {
		initModel.addTrainEvent(a, d);
	}

	public void addTrainEventBasicInit(FragFringeUnambigous a) {
		initModel.addTrainEventBasic(a);		
	}

	/**
	 * 
	 * @param a the left fringe
	 * @param b the right fringe
	 */
	public void addTrainEventSubBackward(SymbolicChartState a, FragFringeUnambigous b) {
		subBackwardModel.addTrainEvent(a, b, 1d);
	}
	
	public void addTrainEventBasicSubBackward(FragFringeUnambigous a) {
		subBackwardModel.addTrainEventBasic(a);
		
	}

	public void addTrainEventSubBackward(SymbolicChartState a, FragFringeUnambigous b, double d) {
		subBackwardModel.addTrainEvent(a, b, d);
	}
	
	public void addTrainEventSetSubBackward(HashSet<SymbolicChartState> historySet, FragFringeUnambigous b) {
		double count = (normalizeAttachemnts) ? 1d/historySet.size() : 1d;
		for(SymbolicChartState a : historySet) {
			subBackwardModel.addTrainEvent(a, b, count);
		}		
	}
	
	/**
	 * 
	 * @param a the left fringe
	 * @param b the right fringe
	 */
	public void addTrainEventSubForward(SymbolicChartState a, FragFringeUnambigous b) {
		subForwardModel.addTrainEvent(a, b, 1d);
	}
	
	public void addTrainEventBasicSubForward(FragFringeUnambigous ff) {
		subForwardModel.addTrainEventBasic(ff);		
	}

	public void addTrainEventSubForward(SymbolicChartState a, FragFringeUnambigous b, double d) {
		subForwardModel.addTrainEvent(a, b, d);
	}
	
	public void addTrainEventSetSubForward(HashSet<SymbolicChartState> historySet, FragFringeUnambigous b) {
		double count = (normalizeAttachemnts) ? 1d/historySet.size() : 1d;
		for(SymbolicChartState a : historySet) {
			subForwardModel.addTrainEvent(a, b, count);
		}		
	}
	
	/**
	 * To run after training is finished
	 */
	public void estimateLogProbabilities() {						
		Parameters.reportLine("Total events in backward model: " + 
				Arrays.toString(subBackwardModel.totalEvents()));
		Parameters.reportLine("Total events in forward model: " + 
				Arrays.toString(subForwardModel.totalEvents()));
		
		initModel.estimateLogProbabilities();
		subBackwardModel.estimateLogProbabilities();
		subForwardModel.estimateLogProbabilities();
		
		//debug
		/*
		String outputDir = Parameters.logFile.getParent() + "/";
		String word = "house";
		TermLabel tl = TermLabel.getTermLabel(word, true);
		File outputFileSampleBackward = new File(outputDir + "backward_table_" + word + ".txt");
		File outputFileSampleForward = new File(outputDir + "forward_table_" + word + ".txt");
		subBackwardModel.printLexEntryToFile(tl, outputFileSampleBackward);
		subForwardModel.printLexEntryToFile(tl, outputFileSampleForward);
		*/
	}
	
	/**
	 * 
	 * @param a the left fringe
	 * @param b the right fringe
	 * @return p(b|a)
	 */
	public HashMap<FragFringeUnambigous, double[]> getEventsLogProbInit(TermLabel lex) {
		return initModel.getEventsLogProb(lex);
	}
	
	public CondProbModelLex getCondProbModelLex(TermLabel lex) {
		return new CondProbModelLex(lex);
	}
	
	class CondProbModelLex {
		
		CondProbAttachLex subBackwardModelLex;
		CondProbAttachLex subForwardModelLex;
		
		public CondProbModelLex(TermLabel lex) {
			subBackwardModelLex = subBackwardModel.getConProbLex(lex);
			subForwardModelLex = subForwardModel.getConProbLex(lex);
		}
		
		public boolean hasBackwardEvents() {
			return subBackwardModelLex.hasEvents();
		}
		
		public boolean hasForwardEvents() {
			return subForwardModelLex.hasEvents();
		}
		
		public HashMap<FragFringeUnambigous, double[]> getEventsLogProbBackward(
				SymbolicChartState a) {
			return subBackwardModelLex.getEventsLogProb(a);
		}
		
		public HashMap<FragFringeUnambigous, double[]> getEventsLogProbForward(
				SymbolicChartState a) {
			return subForwardModelLex.getEventsLogProb(a);
		}
		
	}
}
