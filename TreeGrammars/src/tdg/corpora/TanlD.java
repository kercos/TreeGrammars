package tdg.corpora;

import settings.Parameters;

public class TanlD {

	public static String TanlDBase = Parameters.corpusPath + 
		"Evalita09/Treebanks/Dependency/Tanl/";
	public static String TanlD_Train_Connl = TanlDBase + "isst_train.evalita.conll";
	public static String TanlD_Dev_Connl = TanlDBase + "isst_dev.evalita.conll";
	public static String TanlD_Dev_Blind_Connl = TanlDBase + "isst_dev.blind.evalita.conll";
	public static String TanlD_Test_Connl = TanlDBase + "isst_test.evalita.conll";
	
	public static String TanlD_Train_MstUlab = TanlDBase + "isst_train.evalita.mst.ulab";
	public static String TanlD_Train_MstUlab_EOS = TanlDBase + "isst_train.evalita.mst.ulab.eos";
	public static String TanlD_Train_MstUlab_NoLoops_EOS = TanlDBase + "isst_train.evalita.noLoops.mst.ulab.eos";	
	public static String TanlD_Dev_MstUlab = TanlDBase + "isst_dev.evalita.mst.ulab";
	public static String TanlD_Dev_MstUlab_EOS = TanlDBase + "isst_dev.evalita.mst.ulab.eos";
	public static String TanlD_Dev_MstUlab_NoLoops_EOS = TanlDBase + "isst_dev.evalita.noLoops.mst.ulab.eos";
	public static String TanlD_NoLoops_Name = "TanlD_NoLoops";
	
	
}
