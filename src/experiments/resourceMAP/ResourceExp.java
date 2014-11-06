package experiments.resourceMAP;

import massim.Agent;
import massim.SimulationEngine;
import massim.Team;
import massim.TeamTask;
import massim.agents.advancedactionmap.AdvActionMAPAgent;
import massim.agents.advancedactionmap.AdvActionMAPRepAgent;
import massim.agents.advancedactionmap.AdvActionMAPRepTeam;
import massim.agents.advancedactionmap.AdvActionMAPTeam;
import massim.agents.nohelp.NoHelpTeam;
import massim.agents.nohelp.NoHelpRepTeam;
import massim.agents.nohelp.NoHelpRepAgent;
import massim.agents.resourcemap.ResourceMAPAgent;
import massim.agents.resourcemap.ResourceMAPTeam;

import java.util.*;

import experiments.tools.CsvTool;

/**
 * Experiment for comparing resource MAP to other MAP's
 * 
 * @author Devin Calado & Michael Conner
 * 
 */
public class ResourceExp {

	/*
	 * Command line arguments are: [0] = number of runs, [1] = simulation to run
	 */
	public static void main(String[] args) {

		Scanner inputScanner = new Scanner(System.in);

		try {

			int experimentNumber = 0;
			int numberOfExperiments = 0;
			int numberOfRuns = 0;
			
			
			System.out.println("Enter the experiment number:");
			experimentNumber = inputScanner.nextInt();

			System.out.println("Enter the number of experiments to run:");
			numberOfExperiments = inputScanner.nextInt();
			
			System.out.println("Enter the numbers of runs per experiment:");
			numberOfRuns = inputScanner.nextInt();

			if (experimentNumber == 1)
				runSimulation1(numberOfExperiments, numberOfRuns);
			
			// For running other simulations...
			/*
			else if (experimentNumber == 2)
				runSimulation2(numberOfRuns);
			else if (experimentNumber == 3)
				runSimulation3(numberOfRuns);
			 */
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void runSimulation1(int numberOfExperiments, int numberOfRuns) throws Exception {

		if (numberOfRuns < 1)
			throw new Exception("numberOfRuns is invalid!");

		if (numberOfExperiments < 1)
			throw new Exception("numberOfExperiments is invalid!");
			
		SimulationEngine.colorRange = new int[] { 0, 1, 2, 3, 4, 5 };
		SimulationEngine.numOfColors = SimulationEngine.colorRange.length;
		SimulationEngine.actionCostsRange = new int[] { 10, 40, 100, 150, 250, 300, 350, 500 };

		Team.teamSize = 10;

		//System.out.println("Disturbance NoHelp NoHelpOpt Difference NoHelpRep RepCount NoHelpRepOpt RepCount "
					//	+ "Difference Help HelpOpt Difference HelpRep RepCount HelpRepOpt RepCount Difference");

		// Set up the CSV file for experiment output:
		String[] csv_columns = {"Run Number",
								"Disturbance Amount",
								"No Help Score",
								"No Help Opt Assign Score",
								"Score Difference",
								"No Help Re-plan Score",
								"Re-plan Count",
								"No Help Re-plan Opt Assign Score",
								"Re-plan Count",
								"Score Difference",
								"Help Score",
								"Help Opt Assign Score",
								"Score Difference",
								"Help Re-plan Score",
								"Re-plan Count",
								"Help Re-plan Opt Assign Score",
								"Re-plan Count",
								"Score Difference",
								"ResourceMAP Score",
								"ResourceMAP Re-Plan Opt Score"
								};
		
		
		CsvTool csv_file = new CsvTool("Experiment1Output", csv_columns);
	
		/* The experiments loop */

		for (int expNumber = 0; expNumber < numberOfExperiments; expNumber++) {

			/* Create the teams involved in the simulation */
			Team[] teams = new Team[Team.teamSize];

			// No help team
			teams[0] = new NoHelpTeam();

			// No help team, optimized assignment
			teams[1] = new NoHelpTeam();
			teams[1].setOptimumAssign(true);

			// No help, re-plan team
			teams[2] = new NoHelpRepTeam();

			// No help, re-plan, optimized assignment
			teams[3] = new NoHelpRepTeam();
			teams[3].setOptimumAssign(true);

			// Advanced action MAP team
			teams[4] = new AdvActionMAPTeam();

			// Advanced action MAP, optimized assignment
			teams[5] = new AdvActionMAPTeam();
			teams[5].setOptimumAssign(true);

			// Advanced action MAP, re-plan
			teams[6] = new AdvActionMAPRepTeam();

			// Advanced action MAP, re-plan, optimized assignment
			teams[7] = new AdvActionMAPRepTeam();
			teams[7].setOptimumAssign(true);
			
			// Resource MAP 
			teams[8] = new ResourceMAPTeam();
			
			// Resource MAP, optimized assignment
			teams[9] = new ResourceMAPTeam();
			teams[9].setOptimumAssign(true);

			/* Create the SimulationEngine */
			SimulationEngine se = new SimulationEngine(teams);

			/* Set the experiment-wide parameters: */

			Team.unicastCost = 1;
			Team.broadcastCost = Team.unicastCost * (Team.teamSize - 1);
			Agent.calculationCost = 1;
			Agent.planCostCoeff = 0.025;

			TeamTask.helpOverhead = 20;
			TeamTask.cellReward = 100;
			TeamTask.achievementReward = 2000;
			TeamTask.initResCoef = 160;

			/* Set the Team Attributes */
			
			NoHelpRepAgent.WREP = -0.25;

			AdvActionMAPAgent.WLL = -0.1;
			AdvActionMAPAgent.requestThreshold = 351;
			AdvActionMAPAgent.lowCostThreshold = 50;
			AdvActionMAPAgent.importanceVersion = 2;

			AdvActionMAPRepAgent.WLL = -0.1;
			AdvActionMAPRepAgent.WREP = -0.25;
			AdvActionMAPRepAgent.requestThreshold = 351;
			AdvActionMAPRepAgent.lowCostThreshold = 50;
			AdvActionMAPRepAgent.importanceVersion = 2;
			
			
			/* vary the disturbance */
			SimulationEngine.disturbanceLevel = 0.05 * expNumber;

			/* Initialize and run the experiment */
			se.initializeExperiment(numberOfRuns);
			int[] teamScores = se.runExperiment();

			// How much re-planning did the re-planning teams do?
			int averageReplan2 = (int) Math
					.round((double) ((NoHelpRepTeam) teams[2])
							.getReplanCounts() / numberOfRuns);
			int averageReplan3 = (int) Math
					.round((double) ((NoHelpRepTeam) teams[3])
							.getReplanCounts() / numberOfRuns);
			int averageReplan6 = (int) Math
					.round((double) ((AdvActionMAPRepTeam) teams[6])
							.getReplanCounts() / numberOfRuns);
			int averageReplan7 = (int) Math
					.round((double) ((AdvActionMAPRepTeam) teams[7])
							.getReplanCounts() / numberOfRuns);
			
			// Add run data to csv file as a new row of data
			String[] run_data = {
					String.valueOf(expNumber),
					String.valueOf(SimulationEngine.disturbanceLevel),
					String.valueOf(teamScores[0]),
					String.valueOf(teamScores[1]),
					String.valueOf(teamScores[1] - teamScores[0]),
					String.valueOf(teamScores[2]),
					String.valueOf(averageReplan2),
					String.valueOf(teamScores[3]),
					String.valueOf(averageReplan3),
					String.valueOf(teamScores[3] - teamScores[2]),
					String.valueOf(teamScores[4]),
					String.valueOf(teamScores[5]),
					String.valueOf(teamScores[5] - teamScores[4]),
					String.valueOf(teamScores[6]),
					String.valueOf(averageReplan6),
					String.valueOf(teamScores[7]),
					String.valueOf(averageReplan7),
					String.valueOf(teamScores[7] - teamScores[6]),
					String.valueOf(teamScores[8]),
					String.valueOf(teamScores[9])
					
			};
			csv_file.appendRow(run_data);
			System.out.println("Experiment " + expNumber +" done.");
		}
		// End of all experiment runs
		
		csv_file.closeFileIO();
		System.out.println("Experiment Complete.  Data saved to csv output file.");
	}

	
	
	
	/*
	
	public static void runSimulation2(int numberOfRuns) throws Exception {

		if (numberOfRuns < 1)
			throw new Exception("numberOfRuns is invalid!");

		SimulationEngine.colorRange = new int[] { 0, 1, 2, 3, 4, 5 };
		SimulationEngine.numOfColors = SimulationEngine.colorRange.length;
		SimulationEngine.actionCostsRange = new int[] { 10, 40, 100, 150, 250,
				300, 350, 500 };

		Team.teamSize = 8;

		System.out
				.println("InitResource NoHelp NoHelpOpt Difference NoHelpRep RepCount NoHelpRepOpt RepCount "
						+ "Difference Help HelpOpt Difference HelpRep RepCount HelpRepOpt RepCount Difference");

		for (int exp2 = 0; exp2 < 11; exp2++) {

			Team[] teams = new Team[8];

			teams[0] = new NoHelpTeam();
			teams[1] = new NoHelpTeam();
			teams[1].setOptimumAssign(true);

			teams[2] = new NoHelpRepTeam();
			teams[3] = new NoHelpRepTeam();
			teams[3].setOptimumAssign(true);

			teams[4] = new AdvActionMAPTeam();
			teams[5] = new AdvActionMAPTeam();
			teams[5].setOptimumAssign(true);

			teams[6] = new AdvActionMAPRepTeam();
			teams[7] = new AdvActionMAPRepTeam();
			teams[7].setOptimumAssign(true);

			SimulationEngine se = new SimulationEngine(teams);

			Team.unicastCost = 1;
			Team.broadcastCost = Team.unicastCost * (Team.teamSize - 1);
			Agent.calculationCost = 1;
			Agent.planCostCoeff = 0.025;

			TeamTask.helpOverhead = 20;
			TeamTask.cellReward = 100;
			TeamTask.achievementReward = 2000;

			TeamTask.initResCoef = 100 + 10 * exp2;

			NoHelpRepAgent.WREP = -0.25;

			AdvActionMAPAgent.WLL = -0.1;
			AdvActionMAPAgent.requestThreshold = 351;
			AdvActionMAPAgent.lowCostThreshold = 50;
			AdvActionMAPAgent.importanceVersion = 2;

			AdvActionMAPRepAgent.WLL = -0.1;
			AdvActionMAPRepAgent.WREP = -0.25;
			AdvActionMAPRepAgent.requestThreshold = 351;
			AdvActionMAPRepAgent.lowCostThreshold = 50;
			AdvActionMAPRepAgent.importanceVersion = 2;

			SimulationEngine.disturbanceLevel = 0.1;

			se.initializeExperiment(numberOfRuns);
			int[] teamScores = se.runExperiment();

			int averageReplan2 = (int) Math
					.round((double) ((NoHelpRepTeam) teams[2])
							.getReplanCounts() / numberOfRuns);
			int averageReplan3 = (int) Math
					.round((double) ((NoHelpRepTeam) teams[3])
							.getReplanCounts() / numberOfRuns);
			int averageReplan6 = (int) Math
					.round((double) ((AdvActionMAPRepTeam) teams[6])
							.getReplanCounts() / numberOfRuns);
			int averageReplan7 = (int) Math
					.round((double) ((AdvActionMAPRepTeam) teams[7])
							.getReplanCounts() / numberOfRuns);

			if (teamScores.length > 1) {
				System.out.println(String.format("%d" + "\t%d\t%d\t%d\t%d"
						+ "\t%d\t%d\t%d\t%d" + "\t%d\t%d\t%d\t%d"
						+ "\t%d\t%d\t%d\t%d", TeamTask.initResCoef,
						teamScores[0], teamScores[1], teamScores[1]
								- teamScores[0], teamScores[2], averageReplan2,
						teamScores[3], averageReplan3, teamScores[3]
								- teamScores[2], teamScores[4], teamScores[5],
						teamScores[5] - teamScores[4], teamScores[6],
						averageReplan6, teamScores[7], averageReplan7,
						teamScores[7] - teamScores[6]));
			} else
				System.out.println("Score : 0");
		}
	}

	public static void runSimulation3(int numberOfRuns) throws Exception {

		if (numberOfRuns < 1)
			throw new Exception("numberOfRuns is invalid!");

		SimulationEngine.colorRange = new int[] { 0, 1, 2, 3, 4, 5 };
		SimulationEngine.numOfColors = SimulationEngine.colorRange.length;
		SimulationEngine.actionCostsRange = new int[] { 10, 40, 100, 150, 250,
				300, 350, 500 };

		Team.teamSize = 8;

		System.out
				.println("UnicastCost NoHelp NoHelpOpt Difference NoHelpRep RepCount NoHelpRepOpt RepCount "
						+ "Difference Help HelpOpt Difference HelpRep RepCount HelpRepOpt RepCount Difference");

		for (int exp3 = 0; exp3 < 11; exp3++) {

			Team[] teams = new Team[8];

			teams[0] = new NoHelpTeam();
			teams[1] = new NoHelpTeam();
			teams[1].setOptimumAssign(true);

			teams[2] = new NoHelpRepTeam();
			teams[3] = new NoHelpRepTeam();
			teams[3].setOptimumAssign(true);

			teams[4] = new AdvActionMAPTeam();
			teams[5] = new AdvActionMAPTeam();
			teams[5].setOptimumAssign(true);

			teams[6] = new AdvActionMAPRepTeam();
			teams[7] = new AdvActionMAPRepTeam();
			teams[7].setOptimumAssign(true);

			SimulationEngine se = new SimulationEngine(teams);

			Team.unicastCost = 1 + 2 * exp3;

			Team.broadcastCost = Team.unicastCost * (Team.teamSize - 1);
			Agent.calculationCost = 1;
			Agent.planCostCoeff = 0.025;

			TeamTask.helpOverhead = 20;
			TeamTask.cellReward = 100;
			TeamTask.achievementReward = 2000;
			TeamTask.initResCoef = 160;

			NoHelpRepAgent.WREP = -0.25;

			AdvActionMAPAgent.WLL = -0.1;
			AdvActionMAPAgent.requestThreshold = 351;
			AdvActionMAPAgent.lowCostThreshold = 50;
			AdvActionMAPAgent.importanceVersion = 2;

			AdvActionMAPRepAgent.WLL = -0.1;
			AdvActionMAPRepAgent.WREP = -0.25;
			AdvActionMAPRepAgent.requestThreshold = 351;
			AdvActionMAPRepAgent.lowCostThreshold = 50;
			AdvActionMAPRepAgent.importanceVersion = 2;

			SimulationEngine.disturbanceLevel = 0.1;

			se.initializeExperiment(numberOfRuns);
			int[] teamScores = se.runExperiment();

			int averageReplan2 = (int) Math
					.round((double) ((NoHelpRepTeam) teams[2])
							.getReplanCounts() / numberOfRuns);
			int averageReplan3 = (int) Math
					.round((double) ((NoHelpRepTeam) teams[3])
							.getReplanCounts() / numberOfRuns);
			int averageReplan6 = (int) Math
					.round((double) ((AdvActionMAPRepTeam) teams[6])
							.getReplanCounts() / numberOfRuns);
			int averageReplan7 = (int) Math
					.round((double) ((AdvActionMAPRepTeam) teams[7])
							.getReplanCounts() / numberOfRuns);

			if (teamScores.length > 1) {
				System.out.println(String.format("%d" + "\t%d\t%d\t%d\t%d"
						+ "\t%d\t%d\t%d\t%d" + "\t%d\t%d\t%d\t%d"
						+ "\t%d\t%d\t%d\t%d", Team.unicastCost, teamScores[0],
						teamScores[1], teamScores[1] - teamScores[0],
						teamScores[2], averageReplan2, teamScores[3],
						averageReplan3, teamScores[3] - teamScores[2],
						teamScores[4], teamScores[5], teamScores[5]
								- teamScores[4], teamScores[6], averageReplan6,
						teamScores[7], averageReplan7, teamScores[7]
								- teamScores[6]));
			} else
				System.out.println("Score : 0");
		}
	}
	
	*/

}
