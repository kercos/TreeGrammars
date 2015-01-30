package util;

import java.io.*;
import java.util.*;

import util.file.FileUtil;

public class PiersonCorrelation {
	
	public static float getR(File inputFile) {
		Scanner scan = FileUtil.getScanner(inputFile);
		float sum_sq_x = 0f;
		float sum_sq_y = 0f;
		float sum_coproduct = 0f;
		String line = scan.nextLine();
		float[] pair = getLinePairs(line);	
		float mean_x = pair[0];
		float mean_y = pair[1];
		int i=2;
		while(scan.hasNextLine()) {
			line = scan.nextLine();
			if (line.length()==0) continue;
			pair = getLinePairs(line);
			float sweep = (i - 1.0f) / i;
		    float delta_x = pair[0] - mean_x;
		    float delta_y = pair[1] - mean_y;
		    sum_sq_x += delta_x * delta_x * sweep;
		    sum_sq_y += delta_y * delta_y * sweep;
		    sum_coproduct += delta_x * delta_y * sweep;
		    mean_x += delta_x / i;
		    mean_y += delta_y / i;
		    i++;
		}
		int N = i-1;
		scan.close();
		float pop_sd_x = (float)Math.sqrt( sum_sq_x / N );
		float pop_sd_y = (float)Math.sqrt( sum_sq_y / N );
		float cov_x_y = sum_coproduct / N;
		float correlation = cov_x_y / (pop_sd_x * pop_sd_y);
		return correlation;
		
	}

	public static float[] getLinePairs(String line) {
		String[] numbers = line.split("\\s+");
		float[] result = new float[2];
		result[0] = Float.parseFloat(numbers[0]);
		result[1] = Float.parseFloat(numbers[1]);
		return result;
	}	

	public static void main(String[] args) {
		File inputFile = new File(args[0]);
		System.out.println(getR(inputFile));
		
	}
}
