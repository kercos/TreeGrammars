package util;

public class Sampling {

	public static void main(String[] args) {
		int tasks = 1500;
		int workers = 12;
		//int redundancy = 2; //workers per task
		
		assert tasks % workers == 0;		
		
		int tasksPerFirstWorker = tasks/workers; //125
		int minTasksPerPairs = tasksPerFirstWorker/workers +1 ; //11 
		int maxTasksPerPairs = tasksPerFirstWorker/workers + 2; //12 (some will have 12)
				
		int filledMinimum = minTasksPerPairs*workers*(workers-1); //1320	
		
		int[][] tasksPerPairs = new int[workers][workers];		
		
		int[][] taskPlan = new int[tasks][2];			
		//PrintProgress pp = new PrintProgress("Assigning task", 1, 0);
		for(int i=0; i<tasks; i++) {
			int firstWorker = i % workers;			
			int coworker = -1;
			boolean accept = false;
			do {
				coworker = Utility.randomInteger(workers);
				int tpp = tasksPerPairs[firstWorker][coworker];
				accept = coworker!=firstWorker && 
						(tpp<minTasksPerPairs || (tpp!=maxTasksPerPairs && i>=filledMinimum));
			} while(!accept);
			//pp.next();
			tasksPerPairs[firstWorker][coworker]++;
			taskPlan[i][0] = firstWorker+1;
			taskPlan[i][1] = coworker+1;
		}
		
		//pp.end();
		
		//Utility.printChart(tasksPerPairs);
		
		//System.out.println("----------------------------------");
		
		Utility.printChart(taskPlan);
		
		
	}
	
	

}
