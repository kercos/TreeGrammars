package wordModel;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.TreeSet;

import tsg.TSNodeLabel;
import tsg.corpora.Wsj;
import util.Utility;
import util.file.FileUtil;

public class UkWordMappingFrenchBenoit extends UkWordMapping {
	
	HashSet<String> knownWordsLower;
	
	public UkWordMappingFrenchBenoit() {				
	}
	
	public String getName() {
		return "French_Benoit";
	}
	
	protected void loadDefaultParameters() {
		
		compareTrainTest = false;
		
		knownWordsLower = new HashSet<String>();
		for(Entry<String, int[]> e : lexFrequency.entrySet()) {
			int freq = e.getValue()[0];
			if (freq>ukThreashold) {
				String wordLower = e.getKey().toLowerCase();
				knownWordsLower.add(wordLower);
			}			
		}
				
	}
	
	protected void printParametersInfo() {
		
		System.out.println("Unknown Word Threashold: " + ukThreashold);
						
		System.out.println("\n");
	}
	
	protected void printModelStats() {
	}

	@Override
	public String getFeatureOfWord(String word, boolean firstWord,
			int trainingDevelop) {
		
	    //    int unknownLevel = Options.get().useUnknownWordSignatures;
	    StringBuffer sb = new StringBuffer("UNK");
	    //Highly French Specific Stuff
	          // { -CAPS, -INITC ap, -LC lowercase, 0 } +
	          // { -KNOWNLC, 0 } +          [only for INITC]
	          // { -NUM, 0 } +
	          // { -DASH, 0 } +
	          // { -last lowered char(s) if known discriminating suffix, 0}

	    int wlen = word.length();
          int numCaps = 0;
          boolean hasDigit = false;
          boolean hasDash = false;
          boolean hasLower = false;
          for (int i = 0; i < wlen; i++) {
            char ch = word.charAt(i);
            if (Character.isDigit(ch)) {
              hasDigit = true;
            } else if (ch == '-') {
              hasDash = true;
            } else if (Character.isLetter(ch)) {
              if (Character.isLowerCase(ch)) {
                hasLower = true;
              }else if (Character.isTitleCase(ch)) {
                hasLower = true;
                numCaps++;
              }else{
                numCaps++;
              }
            }
          }
          char ch0 = word.charAt(0);
          String lowered = word.toLowerCase();
          if (Character.isUpperCase(ch0) || Character.isTitleCase(ch0)) {
            if (firstWord && numCaps == 1) {
              sb.append("-INITC");
              if (knownWordsLower.contains(lowered)) {
            	  sb.append("-KNOWNLC");  
              }
            } else {
              sb.append("-CAPS");
            }
          } else if (!Character.isLetter(ch0) && numCaps > 0) {
            sb.append("-CAPS");
          } else if (hasLower) { // (Character.isLowerCase(ch0)) {
            sb.append("-LC");
          }
          if (hasDigit) {
            sb.append("-NUM");
          }
          if (hasDash) {
            sb.append("-DASH");
          }
          if (lowered.endsWith("s") && wlen >= 3) {
      // here length 3, so you don't miss out on ones like 80s
        	  char ch2 = lowered.charAt(wlen - 2);
        	  if (ch2 != 's') {
        		  sb.append("-s");
        	  }
          } else if (word.length() >= 5 && !hasDash && !(hasDigit && numCaps > 0)) {
            // don't do for very short words;
            // Implement common discriminating suffixes
/*          	if (Corpus.myLanguage==Corpus.GERMAN){
          		sb.append(lowered.substring(lowered.length()-1));
          	}else{*/
            if (lowered.endsWith("âtre")) {
              sb.append("-âtre");
            } else if (lowered.endsWith("aphe")) {
              sb.append("-aphe");
            } else if (lowered.endsWith("aphie")) {
              sb.append("-aphie");
            } else if (lowered.endsWith("ment")) {
              sb.append("-ment");
            } else if (lowered.endsWith("aire")) {
              sb.append("-aire");
            } else if (lowered.endsWith("if")) {
              sb.append("-if");
            } else if (lowered.endsWith("ien")) {
              sb.append("-ien");
            } else if (lowered.endsWith("age")) {
              sb.append("-age");
            } else if (lowered.endsWith("al")) {
              sb.append("-al");
            } else if (lowered.endsWith("ale")) {
              sb.append("-ale");
            } else if (lowered.endsWith("ère")) {
              sb.append("-ère");
            }else if (lowered.endsWith("ique")) {
              sb.append("-ique");
            }else if (lowered.endsWith("-tion")) {
              sb.append("-tion");
            }else if (lowered.endsWith("able")) {
              sb.append("-able");
            }else if (lowered.endsWith("aux")) {
              sb.append("-aux");
            }else if (lowered.endsWith("enne")) {
              sb.append("-enne");
            }else if (lowered.endsWith("ive")) {
              sb.append("-ive");
            }else if (lowered.endsWith("al")) {
                sb.append("-al");
            }else if (lowered.endsWith("eur")) {
                sb.append("-eur");
            }else if (lowered.endsWith("ois")) {
                sb.append("-ois");
            }else if (lowered.endsWith("oise")) {
                sb.append("-oise");
            }else if (lowered.endsWith("eux")) {
                sb.append("-eux");
            }
            else if (lowered.endsWith("issons")) {
                sb.append("-Vissons");
            }else if (lowered.endsWith("issez")) {
                sb.append("-Vissez");
            }else if (lowered.endsWith("issent")) {
                sb.append("-Vissent");
            }else if (lowered.endsWith("isse")) {
                sb.append("-Visse");
            }else if (lowered.endsWith("isses")) {
                sb.append("-Visses");
            }else if (lowered.endsWith("issions")) {
                sb.append("-Vissions");
            }else if (lowered.endsWith("issiez")) {
                sb.append("-Vissiez");
            }else if (lowered.endsWith("issant")) {
                sb.append("-Vissant");
            }else if (lowered.endsWith("issais")) {
                sb.append("-Vissais");
            }else if (lowered.endsWith("issait")) {
                sb.append("-Vissait");
            }else if (lowered.endsWith("issaient")) {
                sb.append("-Vissaient");
            }else if (lowered.endsWith("îmes")) {
                sb.append("-Vîmes");
            }else if (lowered.endsWith("îtes")) {
                sb.append("-Vîtes");
            }else if (lowered.endsWith("irent")) {
                sb.append("-Virent");
            }else if (lowered.endsWith("irai")) {
                sb.append("-Virai");
            }else if (lowered.endsWith("iras")) {
                sb.append("-Viras");
            }else if (lowered.endsWith("irons")) {
                sb.append("-Virons");
            }else if (lowered.endsWith("iront")) {
                sb.append("-Viront");
            }else if (lowered.endsWith("irez")) {
                sb.append("-Virez");
            }else if (lowered.endsWith("irais")) {
                sb.append("-Virais");
            }else if (lowered.endsWith("irait")) {
                sb.append("-Virait");
            }else if (lowered.endsWith("irions")) {
                sb.append("-Virions");
            }else if (lowered.endsWith("iriez")) {
                sb.append("-Viriez");
            }else if (lowered.endsWith("iraient")) {
                sb.append("-Viraient");
            }else if (lowered.endsWith("erai")) {
                sb.append("-Verai");
            }else if (lowered.endsWith("eras")) {
                sb.append("-Veras");
            }else if (lowered.endsWith("erons")) {
                sb.append("-Verons");
            }else if (lowered.endsWith("erez")) {
                sb.append("-Verez");
            }else if (lowered.endsWith("eront")) {
                sb.append("-Veront");
            }else if (lowered.endsWith("erais")) {
                sb.append("-Verais");
            }else if (lowered.endsWith("erait")) {
                sb.append("-Verait");
            }else if (lowered.endsWith("erions")) {
                sb.append("-Verions");
            }else if (lowered.endsWith("eriez")) {
                sb.append("-Veriez");
            }else if (lowered.endsWith("eraient")) {
                sb.append("-Veraient");
            }else if (lowered.endsWith("ions")) {
                sb.append("-Vions");
            }else if (lowered.endsWith("iez")) {
                sb.append("-Viez");
            }else if (lowered.endsWith("ant")) {
                sb.append("-Vant");
            }else if (lowered.endsWith("ais")) {
                sb.append("-Vais");
            }else if (lowered.endsWith("ait")) {
                sb.append("-Vait");
            }else if (lowered.endsWith("aient")) {
                sb.append("-Vaient");
            }else if (lowered.endsWith("as")) {
                sb.append("-Vas");
            }else if (lowered.endsWith("âmes")) {
                sb.append("-Vâmes");
            }else if (lowered.endsWith("âtes")) {
                sb.append("-Vâtes");
            }else if (lowered.endsWith("èrent")) {
                sb.append("-Vèrent");
            }else if (lowered.endsWith("ons")) {
                sb.append("-Vons");
            }else if (lowered.endsWith("ez")) {
                sb.append("-Vez");
            }else if (lowered.endsWith("ent")) {
                sb.append("-Vent");
            }else if (lowered.endsWith("es")) {
                sb.append("-Ves");
            }else if (lowered.endsWith("ées")) {
                sb.append("-Vées");
            }else if (lowered.endsWith("er")) {
                sb.append("-Ver");
            }else if (lowered.endsWith("ir")) {
                sb.append("-Vir");
            }else if (lowered.endsWith("oir")) {
                sb.append("-Voir");
            }else if (lowered.endsWith("dre")) {
                sb.append("-Vdre");
            }
            
            
              // } else if (lowered.endsWith("ble")) {
              // sb.append("-ble");
              // } else if (lowered.endsWith("e")) {
              // sb.append("-e");
           
          }
          return sb.toString();

	}
	
	
	

}
