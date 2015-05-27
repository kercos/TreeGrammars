package kernels.parallel.ted;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;


public class DownloadData {
		
	//https://spreadsheets.google.com/feeds/download/spreadsheets/Export?key=1udVgWz0v2ip4KiozPFW5yci7Uk-WUGXLMunq__9mEm8&exportFormat=tsv

	static String[][] annotatorFilesKey = new String[][]{
		{"01 - Monica Corda.tsv", "1Pv57eBJ8Viw7AxHdDN618eEG2VsWAvwkFLuG6qbaSF8"},
		{"02 - Ambra Loi.tsv", "16stjO0BKzWh4ukUop6fFqvz676QvBuTWuRbe6V6oATQ"},
		{"03 - Manuela Cherchi.tsv", "1nzOYFD-87s9iHEHAUI5C-lQBjwAsBOAtgfBBnt22dp8"},
		{"04 - Erika Ibba.tsv", "1I3_fxCLiwj8WuLXWpF4nOBTjQWTyl3TIiTtbdk0kEyU"},
		{"05 - Anna De Santis.tsv", "15fYtPHLQF41bJKnVybrDg5cADf_9Bm_h0dKqiwB8NGQ"},
		{"06 - Giuseppe Casu.tsv", "1MtJGmGI9JSv_zZztYk84Kd7gCy_vRA6K1WFiAFS9szw"},
		{"07 - Jessica Ladu.tsv", "1U4RuM_nrIPT5ORbdcyaeB7Dld7GdpaZEc8qTy1--_w0"},
		{"08 - Ilaria Del Rio.tsv", "1-cTjSwJ5IlMj5pFlE5IGGa3MIdodo15GjY40plPnpTc"},
		{"09 - Deborah Vacca.tsv", "1udVgWz0v2ip4KiozPFW5yci7Uk-WUGXLMunq__9mEm8"},
		{"10 - Elisa Virdis.tsv", "1ACGLwLnzJpWMyACv_TXYSgYGRQDIDPwQ7Gfh_Il7uQ4"},
		{"11 - Gino Castangia.tsv", "1VfVcQ0UgJ2cRjpcGJrtHbw91zNmMgzhGTT3DBakqr_8"},
		{"12 - Isabella De Muro.tsv", "1Q33gaBZKm4fWzWOxysPgrLNdXw2hkMNm2nYNG1jLou0"},
		{"13 - Salvatore Nieddu.tsv", "1VuW9L-ja1RtHu6TMh1hYyZ-VZXHS1qCAGT0ig-_Rb8c"},	
//		{"14 - Rosanna Cossa.tsv", "1VW5KuSP-ov_4IyxxnYWUzcIlM9P5wRy-q5JMpW9re-o"},
//		{"15 - Antonella Cadeddu.tsv", "15Wzsr1kE9CeMeQ3ZrNIvNtov4x9hEzHRPEEELlx9oYY"},
//		{"16 - Simona Cadeddu.tsv", "1cDkacmrvrnsDXO7aWX5xOKNpbRxTPx5anV4mJdDmCKc"}
	};
	
	public static void main(String[] args) throws IOException {
		for(String[] fs : annotatorFilesKey) {
			File outputFile = new File(TED_Corpus.studentAnnotationPath + fs[0]);
			PrintWriter pw = new PrintWriter(outputFile);
			String key = fs[1];
			String urlAddress = "https://spreadsheets.google.com/feeds/download/spreadsheets/Export"
					+ "?key=" + key + "&exportFormat=tsv";
			URL url = new URL(urlAddress);		
			BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
			String line = null;
			boolean first = true;
	        while ((line = br.readLine()) != null) {
	        	if (first)
	        		first = false;
	        	else
	        		pw.println();
	        	pw.print(line);	        	
	        }
	        pw.close();
	        System.out.println("Downloaded " + fs[0]);
		}
	}

}
