package tsg.mwe;

import java.io.File;
import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Scanner;

import util.PrintProgress;

public class Fix_Parenthesis {

	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		
		String usage = "java Fix_Parenthesis inputDir";
		
		if (args.length!=1) {
			System.err.println("Wrong number of arguments");
			System.err.println(usage);
			return;
		}
		
		File dir = new File(args[0]);
		if (!dir.exists() || !dir.isDirectory()) {
			System.err.println("Input dir does not exist or is not a dirctory");
			System.err.println(usage);
			return;
		}
		
		File[] files = dir.listFiles(new FilenameFilter() {			
			@Override
			public boolean accept(File dir, String name) {				
				return name.startsWith("nyt_eng_");
			}
		});
		Arrays.sort(files);
		
		System.out.println("Input dir: " + dir);
		System.out.println("Total files: " + files.length);
		
		String line = "", outputLine = "";
		int currentParBalance = 0;
		for(File f : files) {
			Scanner scan = new Scanner(f);
			System.out.print("Fixing file " + f + "... ");
			PrintWriter pw = new PrintWriter(f + ".fixed");
			int fixes = 0;
			while(scan.hasNextLine()) {						
				line = scan.nextLine();
				currentParBalance += parBalance(line);
				outputLine += line;
				if (currentParBalance==0) {
					pw.println(outputLine);
					outputLine = "";
				}
				else {
					fixes++;
				}
			}	
			pw.close();
			System.out.println(fixes + " lines fixed");
		}
	}

	private static int parBalance(String line) {
		char[] chars = line.toCharArray();
		int p = 0;
		for(char c : chars) {
			if (c=='(') p++;
			else if (c==')') p--;
		}
		return p;
	}
	
	/*

     281562 nyt_eng_199407.mrg.fixed
     292823 nyt_eng_199408.mrg.fixed
     287976 nyt_eng_199409.mrg.fixed
     296472 nyt_eng_199410.mrg.fixed
     260787 nyt_eng_199411.mrg.fixed
     267038 nyt_eng_199412.mrg.fixed
     313711 nyt_eng_199501.mrg.fixed
     298940 nyt_eng_199502.mrg.fixed
     376988 nyt_eng_199503.mrg.fixed
     368441 nyt_eng_199504.mrg.fixed
     559974 nyt_eng_199505.mrg.fixed
     657120 nyt_eng_199506.mrg.fixed
     586034 nyt_eng_199507.mrg.fixed
     658286 nyt_eng_199508.mrg.fixed
     563300 nyt_eng_199509.mrg.fixed
     670466 nyt_eng_199510.mrg.fixed
     664618 nyt_eng_199511.mrg.fixed
     641305 nyt_eng_199512.mrg.fixed
     733538 nyt_eng_199601.mrg.fixed
     667202 nyt_eng_199602.mrg.fixed
     759791 nyt_eng_199603.mrg.fixed
     763918 nyt_eng_199604.mrg.fixed
     820726 nyt_eng_199605.mrg.fixed
     462000 nyt_eng_199606.mrg.fixed
     747807 nyt_eng_199607.mrg.fixed
     719533 nyt_eng_199608.mrg.fixed
     733381 nyt_eng_199609.mrg.fixed
     791582 nyt_eng_199610.mrg.fixed
     698280 nyt_eng_199611.mrg.fixed
     694578 nyt_eng_199612.mrg.fixed
     755466 nyt_eng_199701.mrg.fixed
     680965 nyt_eng_199702.mrg.fixed
     713553 nyt_eng_199703.mrg.fixed
     730474 nyt_eng_199704.mrg.fixed
     721768 nyt_eng_199705.mrg.fixed
     709184 nyt_eng_199706.mrg.fixed
     787127 nyt_eng_199707.mrg.fixed
     725347 nyt_eng_199708.mrg.fixed
     782773 nyt_eng_199709.mrg.fixed
     794977 nyt_eng_199710.mrg.fixed
     692742 nyt_eng_199711.mrg.fixed
     736701 nyt_eng_199712.mrg.fixed
     718391 nyt_eng_199801.mrg.fixed
     493256 nyt_eng_199802.mrg.fixed
     554718 nyt_eng_199803.mrg.fixed
     455449 nyt_eng_199804.mrg.fixed
     560825 nyt_eng_199805.mrg.fixed
     565110 nyt_eng_199806.mrg.fixed
     525672 nyt_eng_199807.mrg.fixed
     544546 nyt_eng_199808.mrg.fixed
     567033 nyt_eng_199809.mrg.fixed
     548499 nyt_eng_199810.mrg.fixed
     544734 nyt_eng_199811.mrg.fixed
     572612 nyt_eng_199812.mrg.fixed
     581715 nyt_eng_199901.mrg.fixed
     513593 nyt_eng_199902.mrg.fixed
     569625 nyt_eng_199903.mrg.fixed
     569300 nyt_eng_199904.mrg.fixed
     552465 nyt_eng_199905.mrg.fixed
     545614 nyt_eng_199906.mrg.fixed
     532108 nyt_eng_199907.mrg.fixed
     527361 nyt_eng_199908.mrg.fixed
     495811 nyt_eng_199909.mrg.fixed
     529100 nyt_eng_199910.mrg.fixed
     502425 nyt_eng_199911.mrg.fixed
     422044 nyt_eng_199912.mrg.fixed
     520079 nyt_eng_200001.mrg.fixed
     509311 nyt_eng_200002.mrg.fixed
     488409 nyt_eng_200003.mrg.fixed
     383507 nyt_eng_200004.mrg.fixed
     419525 nyt_eng_200005.mrg.fixed
     454366 nyt_eng_200006.mrg.fixed
     511913 nyt_eng_200007.mrg.fixed
     552034 nyt_eng_200008.mrg.fixed
     423883 nyt_eng_200009.mrg.fixed
     540055 nyt_eng_200010.mrg.fixed
     527682 nyt_eng_200011.mrg.fixed
     495548 nyt_eng_200012.mrg.fixed
     543370 nyt_eng_200101.mrg.fixed
     499901 nyt_eng_200102.mrg.fixed
     511668 nyt_eng_200103.mrg.fixed
     557465 nyt_eng_200104.mrg.fixed
     524130 nyt_eng_200105.mrg.fixed
     462290 nyt_eng_200106.mrg.fixed
     496307 nyt_eng_200107.mrg.fixed
     525849 nyt_eng_200108.mrg.fixed
     557746 nyt_eng_200109.mrg.fixed
     550912 nyt_eng_200110.mrg.fixed
     532064 nyt_eng_200111.mrg.fixed
     469653 nyt_eng_200112.mrg.fixed
     465601 nyt_eng_200201.mrg.fixed
     496877 nyt_eng_200202.mrg.fixed
     544189 nyt_eng_200203.mrg.fixed
     528810 nyt_eng_200204.mrg.fixed
     508694 nyt_eng_200205.mrg.fixed
     499510 nyt_eng_200206.mrg.fixed
     227329 nyt_eng_200207.mrg.fixed
     455279 nyt_eng_200208.mrg.fixed
     486726 nyt_eng_200209.mrg.fixed
     509912 nyt_eng_200210.mrg.fixed
     458243 nyt_eng_200211.mrg.fixed
     477256 nyt_eng_200212.mrg.fixed
     273097 nyt_eng_200301.mrg.fixed
      59237 nyt_eng_200302.mrg.fixed
      65984 nyt_eng_200303.mrg.fixed
      68190 nyt_eng_200304.mrg.fixed
      62013 nyt_eng_200305.mrg.fixed
      59524 nyt_eng_200306.mrg.fixed
      61723 nyt_eng_200307.mrg.fixed
      51286 nyt_eng_200308.mrg.fixed
      64606 nyt_eng_200309.mrg.fixed
      64986 nyt_eng_200310.mrg.fixed
      57195 nyt_eng_200311.mrg.fixed
      56270 nyt_eng_200312.mrg.fixed
      54305 nyt_eng_200401.mrg.fixed
      48917 nyt_eng_200402.mrg.fixed
      56097 nyt_eng_200403.mrg.fixed
      52544 nyt_eng_200404.mrg.fixed
      11599 nyt_eng_200405.mrg.fixed
     369808 nyt_eng_200407.mrg.fixed
     400656 nyt_eng_200408.mrg.fixed
     382129 nyt_eng_200409.mrg.fixed
     407065 nyt_eng_200410.mrg.fixed
     369241 nyt_eng_200411.mrg.fixed
     323296 nyt_eng_200412.mrg.fixed
     366912 nyt_eng_200501.mrg.fixed
     304418 nyt_eng_200502.mrg.fixed
     346506 nyt_eng_200503.mrg.fixed
     313963 nyt_eng_200504.mrg.fixed
     317323 nyt_eng_200505.mrg.fixed
     349983 nyt_eng_200506.mrg.fixed
     396463 nyt_eng_200507.mrg.fixed
     400152 nyt_eng_200508.mrg.fixed
     283183 nyt_eng_200509.mrg.fixed
     386755 nyt_eng_200510.mrg.fixed
     355394 nyt_eng_200511.mrg.fixed
     361989 nyt_eng_200512.mrg.fixed
     358276 nyt_eng_200601.mrg.fixed
     376515 nyt_eng_200602.mrg.fixed
     372452 nyt_eng_200603.mrg.fixed
     364586 nyt_eng_200604.mrg.fixed
     374919 nyt_eng_200605.mrg.fixed
     346386 nyt_eng_200606.mrg.fixed
     302430 nyt_eng_200607.mrg.fixed
     341741 nyt_eng_200608.mrg.fixed
     377695 nyt_eng_200609.mrg.fixed
     370456 nyt_eng_200610.mrg.fixed
     362864 nyt_eng_200611.mrg.fixed
     340204 nyt_eng_200612.mrg.fixed
     344228 nyt_eng_200701.mrg.fixed
     303668 nyt_eng_200702.mrg.fixed
     331059 nyt_eng_200703.mrg.fixed
     308812 nyt_eng_200704.mrg.fixed
     321781 nyt_eng_200705.mrg.fixed
     315762 nyt_eng_200706.mrg.fixed
     310250 nyt_eng_200707.mrg.fixed
     300886 nyt_eng_200708.mrg.fixed
     287729 nyt_eng_200709.mrg.fixed
     344426 nyt_eng_200710.mrg.fixed
     308094 nyt_eng_200711.mrg.fixed
     302715 nyt_eng_200712.mrg.fixed
     317522 nyt_eng_200801.mrg.fixed
     291184 nyt_eng_200802.mrg.fixed
     305109 nyt_eng_200803.mrg.fixed
     287472 nyt_eng_200804.mrg.fixed
     289086 nyt_eng_200805.mrg.fixed
     284369 nyt_eng_200806.mrg.fixed
     272889 nyt_eng_200807.mrg.fixed
     291083 nyt_eng_200808.mrg.fixed
     272629 nyt_eng_200809.mrg.fixed
     296038 nyt_eng_200810.mrg.fixed
     264702 nyt_eng_200811.mrg.fixed
     259704 nyt_eng_200812.mrg.fixed
     268523 nyt_eng_200901.mrg.fixed
     232645 nyt_eng_200902.mrg.fixed
     261920 nyt_eng_200903.mrg.fixed
     228398 nyt_eng_200904.mrg.fixed
     233922 nyt_eng_200905.mrg.fixed
     208215 nyt_eng_200906.mrg.fixed
     216175 nyt_eng_200907.mrg.fixed
     212971 nyt_eng_200908.mrg.fixed
     214020 nyt_eng_200909.mrg.fixed
     207633 nyt_eng_200910.mrg.fixed
     199129 nyt_eng_200911.mrg.fixed
     195014 nyt_eng_200912.mrg.fixed
     204334 nyt_eng_201001.mrg.fixed
     190244 nyt_eng_201002.mrg.fixed
     205898 nyt_eng_201003.mrg.fixed
     211950 nyt_eng_201004.mrg.fixed
     228996 nyt_eng_201005.mrg.fixed
     229533 nyt_eng_201006.mrg.fixed
     224315 nyt_eng_201007.mrg.fixed
     236523 nyt_eng_201008.mrg.fixed
     234898 nyt_eng_201009.mrg.fixed
     245537 nyt_eng_201010.mrg.fixed
     233068 nyt_eng_201011.mrg.fixed
     227205 nyt_eng_201012.mrg.fixed
   80630949 total
   
	 */
}
