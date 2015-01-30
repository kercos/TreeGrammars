package tdg.corpora;

import settings.Parameters;

public class TutD {

	public static String TutDepBase = Parameters.corpusPath + 
		"Evalita09/Treebanks/Dependency/TUT/";;
	public static String TutNewspaperConnl = TutDepBase + "TUT-rel2_1newspaper-3-8-09.conll";
	public static String TutPassageConnl = TutDepBase + "TUT-rel2_1JRC-Passage-Evalita-10-9-09.conll";
	public static String TutCivilLawConnl = TutDepBase + "TUT-rel2_1civillaw-3-8-09.conll";
	public static String TutTestSetBlindConnl = TutDepBase + "TUT-Evalita09-testset10-9-09.conll";
	public static String TutTrainSetConnl = TutDepBase + "TUT-Evalita09-trainset.conll";
	
	public static String TutDepBaseDev = TutDepBase + "develop/";
	public static String TutDevTrainConnl = TutDepBaseDev + "TUT-Evalita09-trainset90.conll";
	public static String TutDevTestConnl = TutDepBaseDev + "TUT-Evalita09-trainset10.conll";
	public static String TutDevTestBlindConnl = TutDepBaseDev + "TUT-Evalita09-trainset10.blind.conll";
	
	public static String TutDepBaseDev1 = TutDepBase + "develop1/";
	public static String TutDevTrainConnl1 = TutDepBaseDev1 + "TUT-Evalita09-trainset90.conll";
	public static String TutDevTestConnl1 = TutDepBaseDev1 + "TUT-Evalita09-trainset10.conll";
	public static String TutDevTestBlindConnl1 = TutDepBaseDev1 + "TUT-Evalita09-trainset10.blind.conll";
	
	public static String TutDepBaseDev2 = TutDepBase + "develop2/";
	public static String TutDevTrainConnl2 = TutDepBaseDev2 + "TUT-Evalita09-trainset90.conll";
	public static String TutDevTestConnl2 = TutDepBaseDev2 + "TUT-Evalita09-trainset10.conll";
	public static String TutDevTestBlindConnl2 = TutDepBaseDev2 + "TUT-Evalita09-trainset10.blind.conll";
	
	//public static String TutNewspaperMstUlab = TutDepBase + "TUT-rel2_1newspaper.mst.ulab";
	
	
}
