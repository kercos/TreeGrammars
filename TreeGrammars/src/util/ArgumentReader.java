package util;

import java.io.File;

public class ArgumentReader {

	public static boolean readBooleanOption(String option) {
		return Boolean.parseBoolean(option.substring(option.indexOf(':')+1));
	}
	
	public static float readFloatOption(String option) {
		return Float.parseFloat(option.substring(option.indexOf(':')+1));
	}
	
	public static double readDoubleOption(String option) {
		return Double.parseDouble(option.substring(option.indexOf(':')+1));
	}
	
	public static int readIntOption(String option) {
		return Integer.parseInt(option.substring(option.indexOf(':')+1));
	}
	
	public static File readFileOption(String option) {		
		return new File(option.substring(option.indexOf(':')+1));
	}
	
	public static File readFileOptionNoSeparation(String option) {
		if (option.equals("null")) return null;
		return new File(option);
	}
	
	public static String readStringOption(String option) {
		return option.substring(option.indexOf(':')+1);
	}
	
}
