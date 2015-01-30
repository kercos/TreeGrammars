package tsg.evalHeads;

import java.io.*;
import java.util.*;

import settings.Parameters;
import tsg.*;
import tsg.LTSG.LTSG_EM;
import tsg.LTSG.LTSG_Entropy;
import tsg.LTSG.LTSG_Greedy;
import tsg.corpora.*;
import util.*;
import util.file.FileUtil;

public class parcEval {
	
	static int[] indexesSec23 = new int[] {1001, 1005, 1006, 1007, 1008, 99, 1020, 1021, 1022, 1029, 1030, 1037, 1043, 1046, 1055, 1058, 1065, 1069, 1071, 1078, 1087, 1091, 1097, 1098, 1104, 1108, 1109, 1113, 1126, 1140, 1142, 1144, 1145, 1148, 1158, 1164, 1165, 1173, 1174, 1186, 1188, 1190, 1191, 1203, 1204, 1212, 1213, 1222, 1226, 1228, 121, 1231, 1233, 122, 1240, 1244, 1247, 1252, 1267, 1273, 1277, 1279, 1282, 1289, 1298, 128, 11, 1299, 1300, 129, 1313, 1317, 1331, 1346, 1350, 1353, 1356, 134, 1365, 1366, 1372, 1381, 1383, 1392, 1397, 1398, 1400, 1401, 1402, 1409, 1411, 1413, 1416, 1418, 1420, 1428, 1433, 1437, 1438, 1444, 1453, 1455, 1461, 1464, 1465, 145, 1469, 1471, 1472, 1476, 1477, 1482, 1491, 1493, 1495, 1499, 1512, 1513, 1514, 1518, 151, 1546, 1548, 1553, 1556, 1560, 1563, 1576, 1580, 1586, 1594, 1595, 158, 1599, 1602, 1604, 159, 1610, 1612, 1614, 1615, 1620, 161, 1630, 2142, 1644, 1645, 163, 1650, 1651, 1656, 1659, 1660, 1663, 1665, 1669, 1670, 1677, 166, 1680, 1688, 1689, 1698, 1701, 1705, 1709, 1712, 1714, 1716, 1718, 1726, 1728, 1732, 1744, 1747, 173, 1752, 1753, 1754, 1756, 174, 1760, 1762, 1763, 1769, 1771, 1776, 176, 1782, 1785, 1793, 1797, 16, 1802, 1806, 1807, 1809, 1817, 180, 1820, 1822, 1826, 181, 1831, 1837, 1840, 1843, 1846, 1855, 184, 1859, 1863, 185, 1876, 1880, 1883, 1885, 1890, 1896, 1897, 188, 1902, 1906, 1916, 1917, 1919, 1923, 1931, 1932, 1943, 1955, 1959, 1965, 1968, 1972, 1976, 1978, 1980, 1981, 1986, 1992, 1993, 1994, 2004, 2006, 2010, 2011, 2015, 2022, 202, 2040, 2050, 2052, 2056, 2058, 2061, 2066, 2068, 205, 2070, 2073, 2087, 2089, 2092, 2097, 208, 2109, 2113, 2115, 2116, 2117, 210, 2119, 2120, 2123, 2125, 2127, 2128, 2131, 2133, 2138, 2141, 2143, 2144, 2146, 2147, 2153, 2157, 2168, 2169, 2171, 2173, 2178, 216, 2190, 20, 2201, 2215, 2218, 2227, 2229, 2233, 2236, 2241, 2250, 2254, 2255, 224, 2264, 2280, 2283, 2284, 2286, 2293, 2300, 2304, 2305, 2306, 229, 2319, 2320, 2325, 2334, 2338, 2357, 2362, 2363, 2372, 2376, 2379, 2386, 2391, 2397, 2398, 238, 2399, 2398, 2410, 245, 258, 24, 276, 26, 286, 287, 27, 289, 292, 297, 298, 300, 304, 307, 308, 310, 317, 319, 327, 329, 334, 335, 32, 340, 351, 364, 371, 372, 375, 36, 380, 37, 394, 410, 411, 415, 422, 423, 426, 432, 433, 446, 447, 451, 453, 458, 459, 460, 461, 466, 469, 471, 473, 474, 479, 480, 481, 485, 486, 495, 49, 516, 518, 521, 536, 542, 545, 548, 53, 556, 557, 558, 566, 569, 570, 572, 577, 589, 595, 596, 601, 604, 605, 59, 611, 617, 621, 635, 643, 650, 655, 669, 674, 677, 681, 683, 686, 67, 689, 5, 705, 728, 71, 729, 731, 734, 740, 746, 753, 758, 74, 761, 766, 75, 776, 777, 78, 6, 799, 800, 803, 820, 823, 825, 831, 833, 83, 852, 855, 864, 85, 871, 885, 886, 890, 7, 910, 917, 90, 919, 922, 926, 929, 930, 932, 940, 942, 948, 949, 953, 966, 968, 969, 971, 973, 981, 982, 983, 995, 997, 998, 1003, 1013, 1033, 103, 1060, 1077, 1079, 1092, 108, 9, 1105, 1111, 1139, 1154, 1163, 115, 1176, 1177, 1178, 1184, 1192, 1221, 1232, 1239, 1250, 1254, 1268, 1270, 1286, 1288, 1291, 1306, 1315, 1325, 133, 1360, 1364, 1368, 136, 1395, 1410, 1414, 1430, 1475, 1496, 1508, 1510, 1533, 1538, 1540, 1544, 1562, 1567, 1573, 1590, 1613, 1624, 1643, 1731, 1733, 1755, 1787, 177, 1798, 1799, 1811, 1814, 1827, 1834, 1839, 1842, 183, 1849, 1857, 1860, 1861, 1873, 1881, 1904, 1912, 1939, 1950, 1951, 1953, 1957, 1964, 1977, 1979, 1984, 2003, 2008, 2012, 2018, 2029, 2033, 2038, 204, 206, 2095, 2112, 2114, 2132, 2159, 2164, 2175, 2179, 2188, 2220, 2224, 222, 2248, 2276, 226, 2308, 2317, 2323, 231, 2340, 233, 2355, 2356, 2359, 2371, 237, 2401, 2408, 239, 2413, 246, 254, 257, 269, 275, 282, 294, 295, 350, 352, 367, 369, 383, 390, 38, 400, 407, 418, 40, 441, 449, 488, 517, 527, 528, 534, 535, 56, 585, 609, 612, 624, 626, 642, 651, 653, 660, 688, 699, 711, 713, 714, 720, 725, 726, 747, 751, 760, 764, 769, 780, 781, 785, 77, 824, 840, 853, 867, 873, 876, 883, 884, 895, 896, 902, 920, 957, 974, 976, 977, 96, 979};

	/*public static LinkedList<String> selectParc700FromCollins() {
		File sec23selectionTree = new File("/home/fsangati/CORPUS/PARC/parc700_headGold");
		File sec23CollinsTree = new File(Wsj.WsjOriginalCollins97 + "wsj-23.mrg");		
		LinkedList<String> selectionParc = new LinkedList<String>();
		LinkedList<String> selectionParcFlat = new LinkedList<String>();
		LinkedList<String> selectionCollins = new LinkedList<String>();
		LinkedList<String> selectionCollinsFlat = new LinkedList<String>();
		try {
			Scanner scanParc = new Scanner(sec23selectionTree, "ISO-8859-1");
			while ( scanParc.hasNextLine()) {		
				String line = new TreeNode(scanParc.nextLine()).toString();
				line = line.trim();
				selectionParc.add(line);
				selectionParcFlat.add(new TreeNode(line).toFlat());
			}
			Scanner scanCollinsTrees = new Scanner(sec23CollinsTree, "ISO-8859-1");
			while ( scanCollinsTrees.hasNextLine()) {		
				String collinsTree = new TreeNode(scanCollinsTrees.nextLine()).toString();
				String collinsFlat = new TreeNode(collinsTree).toFlat();
				selectionCollins.add(collinsTree);
				selectionCollinsFlat.add(collinsFlat);
			}	
			scanParc.close();
			scanCollinsTrees.close();
		}
		catch (Exception e) {FileUtil.handleExceptions(e);}
		LinkedList<String> finalSelectionCollins = new LinkedList<String>();
		for(ListIterator<String> i = selectionParc.listIterator(); i.hasNext(); ) {
			String parcTree = i.next();
			int index = selectionCollins.indexOf(parcTree);
			if (index==-1) {
				System.out.println("not found");
			}
			else {
				System.out.print(index + ", ");
				finalSelectionCollins.add(selectionCollins.get(index));
			}
		}
		return finalSelectionCollins;
	}*/
	
	public static void buildParcCleanNoTop() {
		File parcGoldFile =  new File(Parc.PARC_GOLD);
		File parcGoldFileNoTop =  new File(Parc.PARC_GOLD_CLEANED_NOTOP);
		ConstCorpus parcGoldCorpus = new ConstCorpus(parcGoldFile, "parcGold");
		parcGoldCorpus.removeNumbersInLables();
		parcGoldCorpus.removeRedundantRules();
		parcGoldCorpus.toFile_Complete(parcGoldFileNoTop, true, false);
	}
	
	public static void reportParcStatistics() {
		//File statisticFile = new File("/home/fsangati/CORPUS/PARC/parc700_statistic.txt");
		File parcGoldFile =  new File(Parc.PARC_GOLD_CLEANED_NOTOP);
		ConstCorpus parcGoldCorpus = new ConstCorpus(parcGoldFile, "parcGold", false);
		parcGoldCorpus.checkHeadAnnotationStatistics(null);
	}
	
	public static void reportStatistics() {
		//File magermanSec23 = new File(Wsj.WsjOriginalCleanedMagerman + "wsj-23.mrg");
		//File collins97Sec23 = new File(Wsj.WsjOriginalCleanedCollins97 + "wsj-23.mrg");
		//File collins99Sec23 = new File(Wsj.WsjOriginalCleanedCollins99 + "wsj-23.mrg");
		File testFile = new File(Wsj.WsjOriginalCleanedYM + "wsj-23.mrg");
		ConstCorpus testParc = new ConstCorpus(testFile, "collinsSec23", false);		
		testParc = testParc.returnIndexes(indexesSec23);
		//collinsParc.fixUnaryHeadConsistency();
		testParc.checkHeadAnnotationStatistics(null);
	}
	
	public static void magermanVsGold() {
		File parcGoldFile =  new File(Parc.PARC_GOLD_CLEANED_NOTOP);
		File parcMagermanFile = new File(Wsj.WsjOriginalCleanedMagerman + "wsj-23.mrg");
		ConstCorpus parcGoldCorpus = new ConstCorpus(parcGoldFile, "parcGold", false);
		ConstCorpus parcMagermanCorpus = new ConstCorpus(parcMagermanFile, "magermanSec23", false);
		parcMagermanCorpus = parcMagermanCorpus.returnIndexes(indexesSec23);
		int[] recall = EvalDependency.evalHeads(parcMagermanCorpus, parcGoldCorpus, -2);
		float ratio = (float)recall[1]/recall[0];
		System.out.println("Total/Recall " + Arrays.toString(recall) + " " + ratio);
	}
	
	public static void collins97VsGold() {
		File collins97Sec23 = new File(Wsj.WsjOriginalCleanedCollins97 + "wsj-23.mrg");
		File parcGoldFile =  new File(Parc.PARC_GOLD_CLEANED_NOTOP);
		//File parcCollinsFile = new File(Parameters.corpusPath + "PARC/parc700_Collins97Annotation");
		ConstCorpus parcGoldCorpus = new ConstCorpus(parcGoldFile, "parcGold", false);
		ConstCorpus parcCollinsCorpus = new ConstCorpus(collins97Sec23, "collinsSec23", false);
		parcCollinsCorpus = parcCollinsCorpus.returnIndexes(indexesSec23);
		int[] recall = EvalDependency.evalHeads(parcCollinsCorpus, parcGoldCorpus, -2);
		float ratio = (float)recall[1]/recall[0];
		System.out.println("Total/Recall " + Arrays.toString(recall) + " " + ratio);
	}
	
	public static void collins99VsGold() {
		File parcGoldFile =  new File(Parc.PARC_GOLD_CLEANED_NOTOP);
		File collins97Sec23 = new File(Wsj.WsjOriginalCleanedCollins99 + "wsj-23.mrg");
		ConstCorpus parcGoldCorpus = new ConstCorpus(parcGoldFile, "parcGold", false);
		ConstCorpus parcCollinsCorpus = new ConstCorpus(collins97Sec23, "parcCollins", false);
		parcCollinsCorpus = parcCollinsCorpus.returnIndexes(indexesSec23);
		parcCollinsCorpus.fixUnaryHeadConsistency();
		int[] recall = EvalDependency.evalHeads(parcCollinsCorpus, parcGoldCorpus, -2);
		float ratio = (float)recall[1]/recall[0];
		System.out.println("Total/Recall " + Arrays.toString(recall) + " " + ratio);
	}
	
	public static void YmVsGold() {
		File parcGoldFile =  new File(Parc.PARC_GOLD_CLEANED_NOTOP);
		File YMSec23 = new File(Wsj.WsjOriginalCleanedYM + "wsj-23.mrg");
		ConstCorpus parcGoldCorpus = new ConstCorpus(parcGoldFile, "parcGold", false);
		ConstCorpus parcYMCorpus = new ConstCorpus(YMSec23, "parcCollins", false);
		parcYMCorpus = parcYMCorpus.returnIndexes(indexesSec23);
		parcYMCorpus.fixUnaryHeadConsistency();
		int[] recall = EvalDependency.evalHeads(parcYMCorpus, parcGoldCorpus, -2);
		float ratio = (float)recall[1]/recall[0];
		System.out.println("Total/Recall " + Arrays.toString(recall) + " " + ratio);
	}
	
	public static void cfgVsGold() {
		Parameters.corpusName = "Parc";
		Parameters.semanticTags = true;
		
		Parameters.outputPath += "CFG/";	 		
		
		File trainingCorpusFile = new File(Parameters.corpusPath + "PARC/02_21_upto40_parc.binary");
		Parameters.trainingCorpus = ConstCorpus.fromBinaryFile(trainingCorpusFile);
		File parcGoldFile =  new File(Parameters.corpusPath + "PARC/parc700_headGold_noQuotationsNew");
		Parameters.testCorpus = new ConstCorpus(parcGoldFile, "parcGold");
		//Parameters.lexiconTable = Parameters.trainingCorpus.buildLexFreq();		
		
		Parameters.trainingCorpus.removeHeadAnnotations();
		Parameters.trainingCorpus.removeNumbersInLables();
		if (!Parameters.semanticTags) Parameters.trainingCorpus.removeSemanticTags();
				
		CFSG<Integer> Grammar = new CFSG<Integer>();
		boolean allowPunctuation = false;
		boolean onlyExternalChoices = false;
		Grammar.assignHeadAnnotations(allowPunctuation, onlyExternalChoices);		
		
		//Grammar.readTreesFromCorpus();
		File lexTableFile = new File(Parameters.outputPath + "lexiconTable.txt");
		//FileUtil.printHashtableToFile(Parameters.lexiconTable, lexTableFile);
		
		ConstCorpus parcEval = Parameters.trainingCorpus.returnLast(700);
		File outputFile = new File(Parameters.outputPath + "Parc700_CFG_heads");
		parcEval.toFile_Complete(outputFile, true);
		int[] recall = EvalDependency.evalHeads(parcEval, Parameters.testCorpus, -2);
		float ratio = (float)recall[1]/recall[0];
		String report = "Total/Recall " + Arrays.toString(recall) + " " + ratio;
		FileUtil.appendReturn(report, Parameters.logFile);
		System.out.println(report);
	}
	
	public static void greedyVsGold() {
		Parameters.corpusName = "Parc";
		Parameters.semanticTags = true;
		
		Parameters.spineConversion = false;
		Parameters.removeRedundencyInSpine = false;
		Parameters.posTagConversion = true;
		Parameters.jollyConversion = false;
		Parameters.jollyInclusion = false;
		Parameters.jollyLabels = new String[]{"NP"}; //",",":","-LRB-","-RRB-","-LCB-","-RCB-","JJ","RB","ADVP","ADJP"
		Arrays.sort(Parameters.jollyLabels);
		
		Parameters.removeTreesLimit = -1;
		
		Parameters.LTSGtype = LTSG_Greedy.GreedyTop; //GreedyTop GreedyBottom GreedyTopEntropy
		Parameters.greedy_punctuation = false;
		Parameters.greedy_ambiguityChoice = 2; //0random, 1left, 2right, 3backoffLex 	
		
		Parameters.outputPath += Parameters.LTSGtype + "/";	 		
		
		File trainingCorpusFile = new File(Parameters.corpusPath + "PARC/02_21_upto40_parc.binary");
		Parameters.trainingCorpus = ConstCorpus.fromBinaryFile(trainingCorpusFile);
		File parcGoldFile =  new File(Parameters.corpusPath + "PARC/parc700_headGold_noQuotationsNew");
		Parameters.testCorpus = new ConstCorpus(parcGoldFile, "parcGold");
		Parameters.lexiconTable = Parameters.trainingCorpus.buildLexFreq();		
		
		Parameters.trainingCorpus.removeHeadAnnotations();
		Parameters.trainingCorpus.removeNumbersInLables();
		if (!Parameters.semanticTags) Parameters.trainingCorpus.removeSemanticTags();
				
		LTSG_Greedy Grammar = new LTSG_Greedy();
		Grammar.assignGreedyAnnotations();		
		
		//Grammar.readTreesFromCorpus();
		Grammar.printTemplatesToFile();
		File lexTableFile = new File(Parameters.outputPath + "lexiconTable.txt");
		FileUtil.printHashtableToFile(Parameters.lexiconTable, lexTableFile);
		
		ConstCorpus parcEval = Parameters.trainingCorpus.returnLast(700);
		File outputFile = new File(Parameters.outputPath + "Parc700_Greedy_heads");
		parcEval.toFile_Complete(outputFile, true);
		int[] recall = EvalDependency.evalHeads(parcEval, Parameters.testCorpus, -2);
		float ratio = (float)recall[1]/recall[0];
		String report = "Total/Recall " + Arrays.toString(recall) + " " + ratio;
		FileUtil.appendReturn(report, Parameters.logFile);
		System.out.println(report);
	}
	
	public static void entropyVsGold() {
		Parameters.LTSGtype = LTSG_Greedy.GreedyTop;
		Parameters.corpusName = "Parc";
		Parameters.semanticTags = true;
		
		Parameters.posTagConversion = true;
		Parameters.spineConversion = true;
		
		Parameters.removeTreesLimit = -1;
		
		Parameters.startingHeads = LTSG_Entropy.Random; //Random Current FirstLeft FirstRight
		Parameters.maxNumberOfChanges = -1; //-1 = no limits  
		Parameters.orderOfChange = 2; //0:random, 1:biggest change first, 2: smallest change first
		Parameters.maxEntropyCycles = -1; //-1 = no limits  
		Parameters.entropy_delta_threshold = 0.01;
		
		Parameters.outputPath += Parameters.LTSGtype + "/";	 	
		
		File trainingCorpusFile = new File(Parameters.corpusPath + "PARC/02_21_upto40_parc.binary");
		Parameters.trainingCorpus = ConstCorpus.fromBinaryFile(trainingCorpusFile);
		File parcGoldFile =  new File(Parameters.corpusPath + "PARC/parc700_headGold_noQuotations");
		Parameters.testCorpus = new ConstCorpus(parcGoldFile, "parcGold");	
		Wsj.removeQuotations(Parameters.testCorpus);
		
		LTSG_Entropy Grammar = new LTSG_Entropy();
		Grammar.hillClimbing();		
		ConstCorpus parcEval = Parameters.trainingCorpus.returnLast(700);
		//File outputFile = new File(Parameters.outputPath + "Parc700_Entropy_heads");
		//parcEval.toFile_Complete(outputFile, true);
		int[] recall = EvalDependency.evalHeads(parcEval, Parameters.testCorpus, -2);
		float ratio = (float)recall[1]/recall[0];
		System.out.println("Total/Recall " + Arrays.toString(recall) + " " + ratio);
	}
	
	public static void naiveVsGold() {
		File parcGoldFile =  new File(Parameters.corpusPath + "PARC/parc700_headGold");
		File parcCollinsFile = new File(Parameters.corpusPath + "PARC/parc700_CollinsAnnotation");
		ConstCorpus parcGoldCorpus = new ConstCorpus(parcGoldFile, "parcGold");
		ConstCorpus parcNaiveCorpus = new ConstCorpus(parcCollinsFile, "parcCollins");
		//parcNaiveCorpus.assignRandomHeads();
		//parcNaiveCorpus.assignFirstLeftHeads();
		parcNaiveCorpus.assignFirstRightHeads();		
		//parcNaiveCorpus.toFile_Complete(new File(Parameters.corpusPath + "PARC/parc700_firstRight"), true);
		int[] recall = EvalDependency.evalHeads(parcNaiveCorpus, parcGoldCorpus, -2);
		float ratio = (float)recall[1]/recall[0];
		System.out.println("Total/Recall " + Arrays.toString(recall) + " " + ratio);
	}
	
	
	public static void EMVsGold() {
		Parameters.corpusName = "Parc";
		Parameters.LTSGtype = "EM";
		Parameters.semanticTags = true;
		
		Parameters.posTagConversion = true;
		Parameters.spineConversion = true;
		
		Parameters.removeTreesLimit = -1;
		
		Parameters.EM_initialization = LTSG_EM.initializeUNIFORM;
		Parameters.EM_nBest = -1;
		Parameters.EM_deltaThreshold = 1;
		Parameters.EM_maxCycle = Integer.MAX_VALUE;;
		
		Parameters.outputPath += Parameters.LTSGtype + "/";
		
		File trainingCorpusFile = new File(Parameters.corpusPath + "PARC/02_21_upto40_parc.binary");
		Parameters.trainingCorpus = ConstCorpus.fromBinaryFile(trainingCorpusFile);
		File parcGoldFile =  new File(Parameters.corpusPath + "PARC/parc700_headGold_noQuotations");
		Parameters.testCorpus = new ConstCorpus(parcGoldFile, "parcGold");	
		Wsj.removeQuotations(Parameters.testCorpus);
		
		LTSG_EM Grammar = new LTSG_EM();
		Grammar.EMalgorithm();		
		
		ConstCorpus parcEval = Parameters.trainingCorpus.returnLast(700);
		File outputFile = new File(Parameters.outputPath + "Parc700_EM_heads");
		parcEval.toFile_Complete(outputFile, true);
		int[] recall = EvalDependency.evalHeads(parcEval, Parameters.testCorpus, -2);
		float ratio = (float)recall[1]/recall[0];
		System.out.println("Total/Recall " + Arrays.toString(recall) + " " + ratio);
	}
	
	public static void EMHeldOutVsGold() {
		Parameters.corpusName = "Parc";
		Parameters.LTSGtype = "EM";
		Parameters.semanticTags = true;
		
		Parameters.posTagConversion = true;
		Parameters.spineConversion = true;
		
		Parameters.removeTreesLimit = -1;
		
		Parameters.EM_initialization = LTSG_EM.initializeDOP;
		Parameters.EM_nBest = -1;
		Parameters.EM_deltaThreshold = 1;
		Parameters.EM_maxCycle = 50;
		
		Parameters.outputPath += Parameters.LTSGtype + "/";
		
		File trainingCorpusFile = new File(Parameters.corpusPath + "PARC/02_21_upto40_parc.binary");
		Parameters.trainingCorpus = ConstCorpus.fromBinaryFile(trainingCorpusFile);
		Parameters.trainingCorpus.removeNumbersInLables();
		File parcGoldFile =  new File(Parameters.corpusPath + "PARC/parc700_headGold_noQuotations");
		Parameters.testCorpus = new ConstCorpus(parcGoldFile, "parcGold");	
		Wsj.removeQuotations(Parameters.testCorpus);		
		 
		LTSG_EM Grammar = new LTSG_EM();
		
		ConstCorpus parcEval = Parameters.trainingCorpus.returnLast(700);
		int size = Parameters.trainingCorpus.size();
		Parameters.trainingCorpus = Parameters.trainingCorpus.returnFirst(size-700);
		
		Grammar.EMHeldOutAlgorithm(parcEval);		
		
		File outputFile = new File(Parameters.outputPath + "Parc700_EM_Heldout_heads");
		parcEval.toFile_Complete(outputFile, true);
		int[] recall = EvalDependency.evalHeads(parcEval, Parameters.testCorpus, -2);
		float ratio = (float)recall[1]/recall[0];
		System.out.println("Total/Recall " + Arrays.toString(recall) + " " + ratio);
	}
	
	
	public static void greedyVsGold2() {
		File parcGoldFile =  new File(Parameters.corpusPath + "PARC/parc700_headGold");
		File parcGreedyFile = new File(Parameters.corpusPath + "PARC/parc700_GreedyAnnotation_Anon_Spine");
		ConstCorpus parcGoldCorpus = new ConstCorpus(parcGoldFile, "parcGold");
		ConstCorpus parcGreedyCorpus = new ConstCorpus(parcGreedyFile, "parcCollins");
		//parcGoldCorpus.cleanCorpus(false, false, false, true, false);
		Wsj.removeQuotations(parcGoldCorpus);
		Wsj.removeQuotations(parcGreedyCorpus);
		//parcGoldCorpus.toFile_Complete(new File(Parameters.corpusPath + "PARC/parc700_headGold_noQuotations"), true);

		int[] recall = EvalDependency.evalHeads(parcGreedyCorpus, parcGoldCorpus, -2);
		float ratio = (float)recall[1]/recall[0];
		System.out.println("Total/Recall " + Arrays.toString(recall) + " " + ratio);
		
		//int[] recall_noCC = EvalDependency.evalDependency(parcGreedyCorpus, parcGoldCorpus, -2, false);
		//float ratio_noCC = (float)recall_noCC[1]/recall_noCC[0];
		//System.out.println("Total/Recall_noCC " + Arrays.toString(recall_noCC) + " " + ratio_noCC);
	}
	
	public static void interannotation() {
		File parcGoldFile =  new File(Parameters.corpusPath + "PARC/Parc700_GreedyBottom_heads");
		File parcTestFile = new File(Parameters.corpusPath + "PARC/Parc700_EM_Heldout_heads");
		ConstCorpus parcGoldCorpus = new ConstCorpus(parcGoldFile, "parcGold");
		ConstCorpus parcTestCorpus = new ConstCorpus(parcTestFile, "parcCollins");

		int[] recall = EvalDependency.evalHeads(parcTestCorpus, parcGoldCorpus, -2);
		float ratio = (float)recall[1]/recall[0];
		System.out.println("Total/Recall " + Arrays.toString(recall) + " " + ratio);
	}
	
	
	public static void main(String[] args) {
		//buildParcCleanNoTop();
		//reportCollinsStatistics();
		//selectIndexFromMagermanCollins();
		
		//Parameters.outputPath = "/home/fsangati/PROJECTS/EvalHeads/Parc/";
		reportParcStatistics();
		//reportStatistics();
		
		//magermanVsGold();
		//collins97VsGold();		
		//collins99VsGold();
		//YmVsGold();
		
		//cfgVsGold();
		//greedyVsGold();
		//EMVsGold();
		//EMHeldOutVsGold();
		//dumptest();
		//greedyVsGold2();
		//entropyVsGold();
		//naiveVsGold();
		//interannotation();
	}
	
}
