package tsg.incremental;

import settings.Parameters;
import tsg.kernels.FragmentSeeker;
import tsg.mb.MarkoBinarizeUnknownWords;

public class PreparingTreebank {
	
	String usageMB = "USAGE: java [-Xmx1G] tsg.mb.MarkoBinarizeUnknownWords " +
			"-outputPath:null [-markoBinarizerType:Petrov_left] [-markovH:1] [-markovV:2] " +
			"[-ukModel:English_Petrov] [-ukThreshold:-1] " +
			"treebankFile testFile";
	String usageFS = "USAGE: java [-Xmx1G] -jar FragmentSeeker.jar [-partialFragments:false] " +
			"[-maxCombination:1000] [-maxMappings:1000] [-flushToDiskEvery:100] " +
			"[-resumeDir:previousDirPath] [-exactFrequencies:true] [-removeTmpFiles:true] " +
			"[-compressTmpFiles:false] [-extractUncoveredFragments:false] [-threads:1] [-outputPath:null] " +
			"[-markoBinarize:false] [-markoBinarizerType:Petrov_left] [-markovH:1] [-markovV:2] " +
			"[-ukModel:English_Petrov] [-ukThreshold:-1] treebankFile";
	
	public static void main(String[] args) throws Exception {		
		
		String workingDir = Parameters.scratchPath + "PLTSG/";
		//String workingDir = Parameters.scratchPath + "WSJ_MB_FS/";
		
		//String WSJpath = Parameters.corpusPath + "WSJ/ORIGINAL_READABLE_CLEANED_TOP_SEMTAGSOFF/";
		String WSJpath = Parameters.corpusPath + "WSJ/ROARK/NOTOP/";
		//String WSJpath = Parameters.corpusPath + "WSJ/ROARK/TOP/";
		//String WSJpath = Parameters.corpusPath + "WSJ/ROARK/STOP/";
		//String WSJpath = Parameters.corpusPath + "WSJ/chelba/ORIGINAL/";
		//String WSJpath = Parameters.corpusPath + "WSJ/chelba/STOP/";
		
		//String trainingFileName = "f0-20.unk10.txt"; 
		String trainingFileName = "wsj-02-21.mrg";
		
		//String testFileName = "f23-24.unk10.txt";
		//String testFileName = "wsj-24.mrg";
		String testFileName = "wsj-23.mrg";
		//String testFileName = "wsj-22.mrg";
			
		String WSJtrainFile = WSJpath + trainingFileName;
		String WSJtestFile = WSJpath + testFileName;
		String mbLeft = "-markoBinarizerType:Petrov_left";
		String mbRightNullary = "-markoBinarizerType:Right_LC_nullary";
		String mbRight = "-markoBinarizerType:Right_LC";
		String mbRightLeftCorner = "-markoBinarizerType:Right_LC_LeftCorner";
		String ukT = "-ukThreshold:"; //(less or equal)
		String ukM_Petrov = "-ukModel:Petrov_Level_";
		String ukM_Basic = "-ukModel:Basic";	
		String[] DIR = new String[]{
			//"MB_Right_H0_V1_UkM5_UkT4",
			//"MB_Right_H1_V1_UkM4_UkT4",
			//"MB_Right_H1_V2_UkM4_UkT4"
			//"MB_ROARK_Right_H0_V1_UkM5_UkT4",
			"MB_ROARK_Right_H0_V1_UkM4_UkT4_notop"	
			//"MB_ROARK_Right_H0_V1_UkM5_UkT4_LeftCorner",
			//"MB_ROARK_Right_H0_V1_UkM4_UkT4_Stop",
			//"MB_ROARK_Right_H1_V1_UkM4_UkT4",
			//"MB_ROARK_Right_H1_V2_UkM4_UkT4",			  
			//"MB_ROARK_RightNull_H0_V1_UkM4_UkT4",
			//"MB_ROARK_RightNull_H1_V1_UkM4_UkT4",
			//"MB_ROARK_RightNull_H1_V2_UkM4_UkT4",
			//"MB_Left_H1_V2_UkM4_UkT4"
			//"MB_Left_H1_V2_UkM5_UkT4"
			//"Chelba_Right_H0_V1",
			//"Chelba_Right_H0_V1_STOP",
			//"Chelba_Right_H1_V1",
			//"Chelba_Right_H1_V2"
			//"Chelba_Right_H2_V1" 
			//"MB_ROARK_Right_H0_V1_UkMBasic_UkT5",
		};
		
		
		
		String[][] MBsettings = new String[DIR.length][];
		String[][] FSsettings = new String[DIR.length][];
		for(int i=0; i<DIR.length; i++) {
			String dir = DIR[i];
			String wd = workingDir + dir + "/";
			String op = "-outputPath:" + wd;
			String mb = dir.indexOf("Right")==-1 ? mbLeft : 
					(dir.indexOf("Null")==-1 ? 
							( dir.indexOf("LeftCorner")==-1 ? mbRight : mbRightLeftCorner) : 
								mbRightNullary); 
			int h = Integer.parseInt(Character.toString(dir.charAt(dir.indexOf('H')+1)));
			int v = Integer.parseInt(Character.toString(dir.charAt(dir.indexOf('V')+1)));
			int ukm = 4;
			int ukt = -1;
			if (dir.contains("Uk")) {
				String ukmChart = Character.toString(dir.charAt(dir.indexOf("UkM")+3));
				ukm = ukmChart.equals("B") ? -1 : Integer.parseInt(ukmChart);
				ukt = Integer.parseInt(Character.toString(dir.charAt(dir.indexOf("UkT")+3)));							
			}
			MBsettings[i] = ukm==-1 ?
				new String[]{op, mb, "-markovH:" + h, "-markovV:" + v, ukM_Basic, ukT+ukt, WSJtrainFile, WSJtestFile} :	
				new String[]{op, mb, "-markovH:" + h, "-markovV:" + v, ukM_Petrov + ukm, ukT+ukt, WSJtrainFile, WSJtestFile};			
			FSsettings[i] = new String[]{"-exactFrequencies:false", "-threads:5", op, wd + trainingFileName };
		}
				
				
		for(int i=0; i<DIR.length; i++) {
			MarkoBinarizeUnknownWords.main(MBsettings[i]);
			System.gc();
			//FragmentSeeker.main(FSsettings[i]);
			//System.gc();			
		}
		
		
		
	}

}
