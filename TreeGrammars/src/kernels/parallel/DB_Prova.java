package kernels.parallel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Map.Entry;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;

public class DB_Prova {

	/**
	 * @param args
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException {
		File file = new File("/gardner0/data/Results/ParalleSubstring_TED_NoAlignment_Min1/prova.db");
		DB db1 = DBMaker.newFileDB(file).mmapFileEnable().transactionDisable().closeOnJvmShutdown().make();
		HTreeMap<String, Integer> hashFile1 = db1.getHashMap("prova");
		for(int i=0; i<1000000000; i++) {		
			//int r = Utility.randomInteger(2000000000);
			hashFile1.put("Random_" + i, i);
		}
		File fileTxt = new File(file.getAbsoluteFile().toString().replace(".db", ".txt"));
		PrintWriter pw = new PrintWriter(fileTxt);
		for(Entry<String, Integer> e : hashFile1.entrySet()) {
			pw.println(e.getKey() + "\t" + e.getValue());
		}
		pw.close();
	}

}
