package tsg.incremental;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Scanner;

import util.file.FileUtil;

public class EvaluatePerplexity {

	public static void computePerplexityRoark() {
		File inputFile = new File("/disk/scratch/fsangati/roarkparser/parse.chelba_23-24_upTo10.output");
		File outputFile = new File("/disk/scratch/fsangati/roarkparser/parse.chelba_23-24_upTo10.cleanLn.output");
		Scanner scan = FileUtil.getScanner(inputFile);
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		double previous = 0;
		double sum = 0;
		int count = 0;
		int lineno = 0;
		int infCount = 0;
		int sentenceCount = 0;
		while(scan.hasNextLine()) {
			lineno++;
			String line = scan.nextLine();
			if (line.startsWith("prefix words")) {
				String prefixProbString = line.substring(line.lastIndexOf('\t')+1);
				if (prefixProbString.equals("inf")) {
					infCount++;
					pw.println("Infinity");
					continue;
				}
				double prefixProb =  Double.parseDouble(prefixProbString);
				
				double condProb = 0;
				if (line.startsWith("prefix words 1-2\t")) {
					sentenceCount++;
					condProb = prefixProb;
					sum += condProb;
				}
				else {
					condProb = (prefixProb-previous);
					sum += condProb;
				}
				pw.println(condProb); ///Math.log(10));
				count++;					
				previous = prefixProb;
			}
		}
		pw.close();
		double perplexity = Math.pow(Math.E,sum/count);
		System.out.println("Total counts: " + count);
		System.out.println("Inf counts: " + infCount);
		System.out.println("Sentence counts: " + sentenceCount);		
		System.out.println("Perplexity: " + perplexity);		
	}

	public static void computePerplexitySRILM() {		
		File inputFile = new File("/disk/scratch/fsangati/PLTSG/Chelba/srilm_f23-24.output");
		File outputLnFile = new File("/disk/scratch/fsangati/PLTSG/Chelba/srilm_f23-24.cleanLn.output");
		File outputLog10File = new File("/disk/scratch/fsangati/PLTSG/Chelba/srilm_f23-24.cleanLog10.output");
		PrintWriter pwLn = FileUtil.getPrintWriter(outputLnFile);
		PrintWriter pwLog10 = FileUtil.getPrintWriter(outputLog10File);
		Scanner scan = FileUtil.getScanner(inputFile);
		double sum = 0;
		double sumCheckLn = 0;
		int count = 0;		
		int lineno = 0;
		while(scan.hasNextLine()) {
			lineno++;
			String line = scan.nextLine();
			if (line.startsWith("\t")) {
				String prefixProbString = line.substring(line.lastIndexOf('[')+1);
				prefixProbString = prefixProbString.substring(0,prefixProbString.lastIndexOf(']'));
				double condProb =  - Double.parseDouble(prefixProbString);
				pwLog10.println(condProb);
				double condProbLn = condProb/Math.log10(Math.E); 
				pwLn.println(condProbLn);
				sum += condProb;		
				sumCheckLn += condProbLn;
				count++;					
			}
		}
		pwLn.close();
		pwLog10.close();
		double perplexity = Math.pow(10,sum/count);
		double perplexityLn = Math.exp(sumCheckLn/count);
		System.out.println("Total counts: " + count);
		System.out.println("Perplexity: " + perplexity);		
		System.out.println("Perplexity Ln: " + perplexityLn);
	}

	private static void iterpolateRoarkSRILM(double srilmContrib) {
		File roarkFileClean = new File("/disk/scratch/fsangati/roarkparser/parse.chelba_23-24_upTo10.cleanLn.output");
		File srilmFileClean = new File("/disk/scratch/fsangati/PLTSG/Chelba/srilm_f23-24_upTo10.cleanLn.output");
		Scanner scanRoark = FileUtil.getScanner(roarkFileClean);
		Scanner scanSrilm = FileUtil.getScanner(srilmFileClean);
		//double srilmContrib = .36;
		double roarkContrib = 1d-srilmContrib;
		double srilmContribLn = Math.log(srilmContrib);
		double roarkContribLn = Math.log(roarkContrib);
		int roarkInfCount = 0;
		double sumRoark = 0;
		double sumSrilm = 0;
		double sumInterpol = 0;
		int count = 0;		
		while(scanRoark.hasNextLine()) {
			double roarkProb = -Double.parseDouble(scanRoark.nextLine());
			double srilmProb = -Double.parseDouble(scanSrilm.nextLine());
			double interpProb = 0;
			if (Double.isInfinite(roarkProb)) {
				interpProb = srilmProb;
				roarkInfCount++;
			}
			else {
				//interpProb = roarkProb*roarkContrib + srilmProb*srilmContrib;
				//interpProb = Utility.logSum(roarkProb+roarkContribLn, srilmProb+srilmContribLn);
				interpProb = Math.log(Math.exp(roarkProb)*roarkContrib + Math.exp(srilmProb)*srilmContrib);
				sumRoark += roarkProb;
			}
			sumSrilm += srilmProb;
			sumInterpol += interpProb;
			count++;
		}		
		double perplexityInterpol = Math.exp(-sumInterpol/count);
		double perplexityRoark = Math.exp(-sumRoark/(count-roarkInfCount));
		double perplexitySrilm = Math.exp(-sumSrilm/count);
		System.out.println("Total counts: " + count);
		System.out.println("Roark inf counts: " + roarkInfCount);
		System.out.println("Perplexity Roark: " + perplexityRoark);
		System.out.println("Perplexity Srilm: " + perplexitySrilm);
		
		System.out.println(srilmContrib + "\tPerplexity Interpol: " + perplexityInterpol);
	}

	private static void iterpolateITSG_SRILM(double srilmContrib) {
		//File itsgFileClean = new File("/disk/scratch/fsangati/PLTSG/Chelba/Chelba_Right_H0_V1/Parsing/EPVP_wsj24_upTo10_Prune-OFF_newComplete_LEFT/prefixProbs_cleanLn");
		//File itsgFileClean = new File("/disk/scratch/fsangati/PLTSG/Chelba/Chelba_Right_H1_V1/Parsing/EPVP_wsj24_upTo10_Prune-OFF_LEFT/prefixProbs_cleanLn");
		File itsgFileClean = new File("/disk/scratch/fsangati/PLTSG/Chelba/Chelba_Right_H2_V1/Parsing/EPVP_wsj24_upTo10_Prune-OFF_LEFT/prefixProbs_cleanLn");
		File srilmFileClean = new File("/disk/scratch/fsangati/PLTSG/Chelba/srilm_f23-24_upTo10.cleanLn.output");
		Scanner scanItsg = FileUtil.getScanner(itsgFileClean);
		Scanner scanSrilm = FileUtil.getScanner(srilmFileClean);
		//double srilmContrib = .36;
		double itsgContrib = 1d-srilmContrib;
		//double srilmContribLn = Math.log(srilmContrib);
		//double itsgContribLn = Math.log(itsgContrib);
		int itsgInfCount = 0;
		double sumItsg = 0;
		double sumSrilm = 0;
		double sumInterpol = 0;
		int count = 0;		
		while(scanItsg.hasNextLine()) {
			double itsgProb = -Double.parseDouble(scanItsg.nextLine());
			double srilmProb = -Double.parseDouble(scanSrilm.nextLine());
			double interpProb = 0;
			if (Double.isInfinite(itsgProb)) {
				interpProb = srilmProb;
				itsgInfCount++;
			}
			else {
				//interpProb = Utility.logSum(itsgProb+itsgContribLn, srilmProb+srilmContribLn);
				interpProb = Math.log(Math.exp(itsgProb)*itsgContrib + Math.exp(srilmProb)*srilmContrib);
				sumItsg += itsgProb;
			}
			sumSrilm += srilmProb;
			sumInterpol += interpProb;
			count++;
		}
		double perplexityInterpol = Math.exp(-sumInterpol/count);
		double perplexityItsg = Math.exp(-sumItsg/(count-itsgInfCount));
		double perplexitySrilm = Math.exp(-sumSrilm/count);
		System.out.println("Total counts: " + count);
		System.out.println("ITSG inf counts: " + itsgInfCount);
		System.out.println("Perplexity ITSG: " + perplexityItsg);
		System.out.println("Perplexity Srilm: " + perplexitySrilm);
		
		System.out.println(srilmContrib + "\tPerplexity Interpol: " + perplexityInterpol);		
	}

	public static void computePerplexityITSG() {
		//File inputFile = new File("/disk/scratch/fsangati/PLTSG/Chelba/Chelba_Right_H0_V1/Parsing/EPVP_wsj24_upTo10_Prune-OFF_newComplete_LEFT/prefixProbs");
		//File outputFile = new File("/disk/scratch/fsangati/PLTSG/Chelba/Chelba_Right_H0_V1/Parsing/EPVP_wsj24_upTo10_Prune-OFF_newComplete_LEFT/prefixProbs_cleanLn");		
		//File inputFile = new File("/disk/scratch/fsangati/PLTSG/Chelba/Chelba_Right_H1_V1/Parsing/EPVP_wsj24_upTo10_Prune-OFF_LEFT/prefixProbs");
		//File outputFile = new File("/disk/scratch/fsangati/PLTSG/Chelba/Chelba_Right_H1_V1/Parsing/EPVP_wsj24_upTo10_Prune-OFF_LEFT/prefixProbs_cleanLn");
		File inputFile = new File("/disk/scratch/fsangati/PLTSG/Chelba/Chelba_Right_H2_V1/Parsing/EPVP_wsj24_upTo10_Prune-OFF_LEFT/prefixProbs");
		File outputFile = new File("/disk/scratch/fsangati/PLTSG/Chelba/Chelba_Right_H2_V1/Parsing/EPVP_wsj24_upTo10_Prune-OFF_LEFT/prefixProbs_cleanLn");
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		Scanner scan = FileUtil.getScanner(inputFile);		
		double previous = 0;
		double sum = 0;
		int count = 0;
		int lineno = 0;
		int infCount = 0;
		int sentenceCount = 0;
		while(scan.hasNextLine()) {
			lineno++;
			String line = scan.nextLine();
			
			String prefixProbString = line.substring(line.lastIndexOf('\t')+1);
			double prefixProb =  Math.abs(Double.parseDouble(prefixProbString));	
			
			if (Double.isInfinite(prefixProb)) {
				infCount++;
				pw.println(prefixProb);
				continue;
			}
			
			double condProb=0;
			if (line.contains("Prefix length: 1\t")) {
				sentenceCount++;
				condProb = prefixProb;
				sum += condProb;
			}
			else {
				condProb = (prefixProb-previous);
				sum += condProb;
			}				
			pw.println(condProb);
			count++;					
			previous = prefixProb;
		}
		pw.close();
		double perplexity = Math.pow(Math.E,sum/count);
		System.out.println("Sum: " + sum);
		System.out.println("Total counts: " + count);
		System.out.println("Sentence counts: " + sentenceCount);
		System.out.println("Inf counts: " + infCount);
		System.out.println("Perplexity: " + perplexity);		
	}

	private static void checkPerplexity() {
		int size = 5;
		double[] a = new double[size];
		double[] b = new double[size];
		double pplA = 0;
		double pplB = 0;
		for(int i=0; i<size; i++) {
			a[i] = Math.random();
			b[i] = Math.random();
			pplA += -Math.log(a[i]);
			pplB += -Math.log(b[i]);
		}
		
		pplA = Math.exp(pplA/size);
		pplB = Math.exp(pplB/size);		
		
		double[] c_min = null;
		double pplC_min = Double.MAX_VALUE;
		double lambda_min=0;
		
		for(int i=0; i<=100; i++) {
			double lambda = ((double)i)/100;
			double oneMinusLambda = 1d - lambda;
			double[] c = new double[size];
			for(int j=0; j<size; j++) {
				c[j] = lambda*a[j] + oneMinusLambda*b[j];
			}
			double pplC = 0;
			for(int j=0; j<size; j++) {
				pplC += -Math.log(c[j]);
			}
			pplC = Math.exp(pplC/size);	
			if (pplC<pplA && pplC<pplB && pplC<pplC_min) {	
				pplC_min = pplC;
				c_min = Arrays.copyOf(c, size);
				lambda_min = lambda;
			}
		}
		
		if (c_min!=null) {
			System.out.println("Lambda: " + lambda_min);
			System.out.println("A: " + Arrays.toString(a));
			System.out.println("B: " + Arrays.toString(b));
			System.out.println("C: " + Arrays.toString(c_min));
			System.out.println("PPL_A: " + pplA);
			System.out.println("PPL_B: " + pplB);
			System.out.println("PPL_C: " + pplC_min);
		}
	}

	public static void main(String[] args) throws Exception {		
		//cleanRoarkOutput(new File(args[0]),new File(args[1]));
		//mainMinConnectEval(args);
		//computePerplexityRoark();
		//computePerplexitySRILM();
		//computePerplexityITSG();
		//for(int i=0; i<=100; i++) {
			//double lambda = ((double)i)/100;
			//double lambda = .36;
			//iterpolateRoarkSRILM(lambda);
			//iterpolateITSG_SRILM(lambda);
		//}		
		//for(int i=0; i<100; i++) {
		//	checkPerplexity();
		//}
	}
}
