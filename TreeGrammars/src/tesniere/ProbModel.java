package tesniere;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

import settings.Parameters;
import symbols.Symbol;
import tsg.HeadLabeler;
import tsg.corpora.ConstCorpus;
import util.file.FileUtil;

public abstract class ProbModel {
	
	public static boolean skipSentencesWithNullCategories = true;
	ArrayList<Box> trainingTreebank;

	
	
	public ProbModel(){
		
	}
	
	public ProbModel(ArrayList<Box> trainingTreebank) {
		this.trainingTreebank = trainingTreebank;
	}	
	
	public ProbModel(File trainingFile) throws Exception {
		HeadLabeler HL =  new HeadLabeler(new File("resources/fede.rules_22_01_10"));
		trainingTreebank = Conversion.getTesniereTreebank(trainingFile, HL);
	}
	
	public void preprocessTrainig() {
		int sentenceNumber = 1;
		for(Box b : trainingTreebank) {
			//b.fixOriginalCategories();
			if (skipSentencesWithNullCategories) {
				if (b.hasNullCategories()) {
					Parameters.logStdOutPrintln("Skipping training sentence " + sentenceNumber + 
							" since it contains blocks without categories.");
				}
				if (b.hasStdBlocksWithoutFullWords()) {
					Parameters.logStdOutPrintln("Skipping training sentence " + sentenceNumber + 
					" since it contains standard blocks without full words.");
				}
			}			
			sentenceNumber++;
		}						
	}
	
	public void train() {
		for(Box b : trainingTreebank) {
			trainFromStructure(b);
		}
	}
	
	public void trainFromStructure(Box b) {
		ArrayList<Event> trainingEvents = new ArrayList<Event>(); 
		extractEventsFromStructure(b, trainingEvents);
		storeEvents(trainingEvents);
	}
	
	public abstract void extractEventsFromStructure(Box structure, ArrayList<Event> trainingEvents);
	
	public void storeEvents(ArrayList<Event> trainingEvents) {
		for(Event e : trainingEvents) {
			e.storeEvent();
		}
	}
	
	public ArrayList<Event> getEventsList(Box structure) {
		ArrayList<Event> eventsList = new ArrayList<Event>(); 
		extractEventsFromStructure(structure, eventsList);
		return eventsList;
	}
	
	public static double getProb(ArrayList<Event> eventsList) {
		double result = 0d;
		for(Event e : eventsList) {
			double prob = Math.log(e.getProb()); 
			result += prob; 
		}
		return Math.exp(result);		
	}

	public double getProb(Box structure) {
		ArrayList<Event> eventsList = getEventsList(structure); 		
		return getProb(eventsList);		
	}
	
	public double getProb(ArrayList<Event> eventsList, boolean reportEvents) {
		double totalProb = 1d;
		for(Event e : eventsList) {
			double prob = e.getProb();
			if (reportEvents) {
				//e.reportEvent();
				System.out.println("\tProb: " + prob + "\n");
			}
			totalProb *= prob;
		}
		return totalProb;		
	}
	
	public double getProb(Box structure, boolean reportEvents) {
		ArrayList<Event> eventsList = getEventsList(structure);
		return getProb(eventsList, reportEvents);
	}
		
	
	protected abstract class Event implements Serializable {
		
		private static final long serialVersionUID = 0L;
		
		public abstract void storeEvent();
		
		public abstract double getProb();
		
		public abstract boolean equals(Object otherObject);
		
		public abstract int[] getCondFreqEventFreq(int decompNumb , int backoffLevel);
		
		public abstract String encodingToStringFreq(int decompNumb , int backoffLevel);		
		
	}

	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub

	}


}
