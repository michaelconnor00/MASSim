package experiments.resourceMAP;

import experiments.datatools.ExperimentMetaDataOutput;
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
import massim.agents.resourcemap.ResourceMAPRepAgent;
import massim.agents.resourcemap.ResourceMAPRepTeam;
import massim.agents.resourcemap.ResourceMAPTeam;
import massim.agents.resourcemap.ResourceMAPRep_TBAgent;
import massim.agents.resourcemap.ResourceMAPRep_TBTeam;

import java.util.*;

import experiments.datatools.CsvTool;

/**
 * Experiment for comparing resource MAP to other MAP's
 * 
 * @author Devin Calado & Michael Conner
 * 
 */
public class ResourceExp {

	public static void main(String[] args) {

		Scanner inputScanner = new Scanner(System.in);

		try {

//			int experimentNumber = -1;
			String experimentNumber;
			
			System.out.println("Enter the experiment number:");
//			experimentNumber = inputScanner.nextInt();
			experimentNumber = inputScanner.nextLine();
			String[] numList = experimentNumber.split(",");
//			System.out.println(""+numList[0]+", "+numList[1]+", "+numList[2]);

			System.out.println("Enter the number of experiments to run:");
			final int numberOfExperiments = inputScanner.nextInt();
			
			System.out.println("Enter the numbers of runs per experiment:");
			final int numberOfRuns = inputScanner.nextInt();

			if (numberOfRuns < 1)
				throw new Exception("numberOfRuns is invalid!");

			if (numberOfExperiments < 1)
				throw new Exception("numberOfExperiments is invalid!");
			for(int i=0; i<numList.length; i++){
				String e = numList[i];
				System.out.println("Exp num: "+e);
				switch (e){
					
					// 3 Teams , variable: disturbance amount
					case "1":
						new Thread() {
							@Override
							public void run(){
								runSimulation1(numberOfExperiments, numberOfRuns);
							}
						}.start();
						break;
					
//					// 6 Teams, variable: disturbance amount
//					case "2":
//						runSimulation2(numberOfExperiments, numberOfRuns);
//						break;
//		
//					// 6 Teams, variable: initial resources (Constrained Resources)
//					case "3":
//						runSimulation3(numberOfExperiments, numberOfRuns);
//						break;
//		
//					// 6 Teams, variable: initial resources (Constrained Resources (2x exp #3))
//					case "4":
//						runSimulation4(numberOfExperiments, numberOfRuns);
//						break;
//		
//					// 6 Teams, variable: increasing unicast costs
//					case "5":
//						runSimulation5(numberOfExperiments, numberOfRuns);
//						break;
					
					// 3 Teams , variable: init resources
					case "6":
						new Thread() {
							@Override
							public void run(){
								runSimulation6(numberOfExperiments, numberOfRuns);
							}
						}.start();
						break;
				    
				    // 3 Teams , variable: unicast cost
					case "7":
						new Thread() {
							@Override
							public void run(){
								runSimulation7(numberOfExperiments, numberOfRuns);
							}
						}.start();
						break;
				    
				    // 3 Teams , variable: calculation cost
					case "8":
						new Thread() {
							@Override
							public void run(){
								runSimulation8(numberOfExperiments, numberOfRuns);
							}
						}.start();
						break;
						
					default:
						System.out.println("A valid experiment was not selected, exiting program.");
						System.exit(0);
				}
			}	
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	
	public static void runSimulation1(int numberOfExperiments, int numberOfRuns) {
		
		SimulationEngine.colorRange = new int[] { 0, 1, 2, 3, 4, 5 };
		SimulationEngine.numOfColors = SimulationEngine.colorRange.length;
		SimulationEngine.actionCostsRange = new int[] { 10, 40, 100, 150, 250, 300, 350, 500 };

		Team.teamSize = 8;

		// Set up the CSV file for experiment output:
		String[] csv_columns = {"Run Number",
				"Disturbance Amount",
				"No Help Re-plan Score",
				"AdvActionMAP Re-plan Score",
				"ResourceMAP TB Re-plan Score"
		};


		CsvTool csv_file = new CsvTool("Simulation1Output", csv_columns);

		/* The experiments loop */

		for (int i = 0; i < numberOfExperiments && SimulationEngine.disturbanceLevel <= 1; i++) {

			/* Create the teams involved in the simulation */
			Team[] teams = new Team[3];

			// No help, re-plan team
			teams[0] = new NoHelpRepTeam();

			// Advanced action MAP, re-plan
			teams[1] = new AdvActionMAPRepTeam();
			
			// Resource MAP, re-plan
			teams[2] = new ResourceMAPRep_TBTeam();
			

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

			ResourceMAPRepAgent.costToGoalHelpThreshold = 1.25;
			ResourceMAPRepAgent.canSacrifice = true;

			/* vary the disturbance */
			SimulationEngine.disturbanceLevel = 0.05 * i;

			/* Initialize and run the experiment */
			se.initializeExperiment(numberOfRuns);
			int[] teamScores = se.runExperiment();

			// Add run data to csv file as a new row of data
			String[] run_data = {
					String.valueOf(i+1),
					String.valueOf(SimulationEngine.disturbanceLevel),
					String.valueOf(teamScores[0]),
					String.valueOf(teamScores[1]),
					String.valueOf(teamScores[2])
			};

			csv_file.appendRow(run_data);
			System.out.println("Experiment " + i +" done.");
		}
		// End of all experiment runs

		csv_file.closeFileIO();
		System.out.println("Simulation Complete.  Data saved to csv output file.");

		ExperimentMetaDataOutput.exportSimulationMetadata("Simulation1", numberOfExperiments, numberOfRuns);
		System.out.println("Simulation Metadata saved to output text file.");

	}


	public static void runSimulation2(int numberOfExperiments, int numberOfRuns) {
		

		SimulationEngine.colorRange = new int[] { 0, 1, 2, 3, 4, 5 };
		SimulationEngine.numOfColors = SimulationEngine.colorRange.length;
		SimulationEngine.actionCostsRange = new int[] { 10, 40, 100, 150, 250, 300, 350, 500 };

		Team.teamSize = 6;

		// Set up the CSV file for experiment output:
		String[] csv_columns = {"Run Number",
				"Disturbance Amount",
				"No Help Score",
				"No Help Re-plan Score",
				"AdvActionMAP Score",
				"AdvActionMAP Re-plan Score",
				"ResourceMAP Score",
				"ResourceMAP Re-plan Score"
		};


		CsvTool csv_file = new CsvTool("Simulation2Output", csv_columns);

		/* The experiments loop */

		for (int i = 0; i < numberOfExperiments; i++) {

			/* Create the teams involved in the simulation */
			Team[] teams = new Team[Team.teamSize];

			// No help team
			teams[0] = new NoHelpTeam();

			// No help, re-plan team
			teams[1] = new NoHelpRepTeam();

			// Advanced action MAP team
			teams[2] = new AdvActionMAPTeam();

			// Advanced action MAP, re-plan
			teams[3] = new AdvActionMAPRepTeam();

			// Resource MAP
			teams[4] = new ResourceMAPTeam();

			// Resource MAP, re=plan
			teams[5] = new ResourceMAPRepTeam();


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

			ResourceMAPRepAgent.costToGoalHelpThreshold = 1.0;
			ResourceMAPRepAgent.canSacrifice = true;

			/* vary the disturbance */
			SimulationEngine.disturbanceLevel = 0.05 * i;

			/* Initialize and run the experiment */
			se.initializeExperiment(numberOfRuns);
			int[] teamScores = se.runExperiment();

			// Add run data to csv file as a new row of data
			String[] run_data = {
					String.valueOf(i+1),
					String.valueOf(SimulationEngine.disturbanceLevel),
					String.valueOf(teamScores[0]),
					String.valueOf(teamScores[1]),
					String.valueOf(teamScores[2]),
					String.valueOf(teamScores[3]),
					String.valueOf(teamScores[4]),
					String.valueOf(teamScores[5])
			};

			csv_file.appendRow(run_data);
			System.out.println("Experiment " + i +" done.");
		}
		// End of all experiment runs

		csv_file.closeFileIO();
		System.out.println("Simulation Complete.  Data saved to csv output file.");

		ExperimentMetaDataOutput.exportSimulationMetadata("Simulation2", numberOfExperiments, numberOfRuns);
		System.out.println("Simulation Metadata saved to output text file.");
	}

	public static void runSimulation3(int numberOfExperiments, int numberOfRuns) {


		SimulationEngine.colorRange = new int[] { 0, 1, 2, 3, 4, 5 };
		SimulationEngine.numOfColors = SimulationEngine.colorRange.length;
		SimulationEngine.actionCostsRange = new int[] { 10, 40, 100, 150, 250, 300, 350, 500 };

		Team.teamSize = 6;

		// Set up the CSV file for experiment output:
		String[] csv_columns = {"Run Number",
				"Resource Co-efficient",
				"No Help Score",
				"No Help Re-plan Score",
				"AdvActionMAP Score",
				"AdvActionMAP Re-plan Score",
				"ResourceMAP Score",
				"ResourceMAP Re-plan Score"
		};


		CsvTool csv_file = new CsvTool("Simulation3Output", csv_columns);

		/* The experiments loop */

		for (int i = 1; i <= numberOfExperiments; i++) {

			/* Create the teams involved in the simulation */
			Team[] teams = new Team[Team.teamSize];

			// No help team
			teams[0] = new NoHelpTeam();

			// No help, re-plan team
			teams[1] = new NoHelpRepTeam();

			// Advanced action MAP team
			teams[2] = new AdvActionMAPTeam();

			// Advanced action MAP, re-plan
			teams[3] = new AdvActionMAPRepTeam();

			// Resource MAP
			teams[4] = new ResourceMAPTeam();

			// Resource MAP, re=plan
			teams[5] = new ResourceMAPRepTeam();


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
			TeamTask.initResCoef = 2 * i;//was 160


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
			SimulationEngine.disturbanceLevel = 0.05;

			/* Initialize and run the experiment */
			se.initializeExperiment(numberOfRuns);
			int[] teamScores = se.runExperiment();

			// Add run data to csv file as a new row of data
			String[] run_data = {
					String.valueOf(i),
					String.valueOf(TeamTask.initResCoef),
					String.valueOf(teamScores[0]),
					String.valueOf(teamScores[1]),
					String.valueOf(teamScores[2]),
					String.valueOf(teamScores[3]),
					String.valueOf(teamScores[4]),
					String.valueOf(teamScores[5])
			};

			csv_file.appendRow(run_data);

			System.out.println("Experiment " + i +" done.");
		}
		// End of all experiment runs

		csv_file.closeFileIO();
		System.out.println("Simulation Complete.  Data saved to csv output file.");

		ExperimentMetaDataOutput.exportSimulationMetadata("Simulation3", numberOfExperiments, numberOfRuns);
		System.out.println("Simulation Metadata saved to output text file.");
	}

	public static void runSimulation4(int numberOfExperiments, int numberOfRuns) {

		SimulationEngine.colorRange = new int[] { 0, 1, 2, 3, 4, 5 };
		SimulationEngine.numOfColors = SimulationEngine.colorRange.length;
		SimulationEngine.actionCostsRange = new int[] { 10, 40, 100, 150, 250, 300, 350, 500 };

		Team.teamSize = 6;

		// Set up the CSV file for experiment output:
		String[] csv_columns = {"Run Number",
				"Resource Co-efficient",
				"No Help Score",
				"No Help Re-plan Score",
				"AdvActionMAP Score",
				"AdvActionMAP Re-plan Score",
				"ResourceMAP Score",
				"ResourceMAP Re-plan Score"
		};


		CsvTool csv_file = new CsvTool("Simulation4Output", csv_columns);

		/* The experiments loop */

		for (int i = 1; i <= numberOfExperiments; i++) {

			/* Create the teams involved in the simulation */
			Team[] teams = new Team[Team.teamSize];

			// No help team
			teams[0] = new NoHelpTeam();

			// No help, re-plan team
			teams[1] = new NoHelpRepTeam();

			// Advanced action MAP team
			teams[2] = new AdvActionMAPTeam();

			// Advanced action MAP, re-plan
			teams[3] = new AdvActionMAPRepTeam();

			// Resource MAP
			teams[4] = new ResourceMAPTeam();

			// Resource MAP, re=plan
			teams[5] = new ResourceMAPRepTeam();


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
			TeamTask.initResCoef = 4 * i;


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
			SimulationEngine.disturbanceLevel = 0.05;

			/* Initialize and run the experiment */
			se.initializeExperiment(numberOfRuns);
			int[] teamScores = se.runExperiment();

			// Add run data to csv file as a new row of data
			String[] run_data = {
					String.valueOf(i),
					String.valueOf(TeamTask.initResCoef),
					String.valueOf(teamScores[0]),
					String.valueOf(teamScores[1]),
					String.valueOf(teamScores[2]),
					String.valueOf(teamScores[3]),
					String.valueOf(teamScores[4]),
					String.valueOf(teamScores[5])
			};

			csv_file.appendRow(run_data);

			System.out.println("Experiment " + i +" done.");
		}
		// End of all experiment runs

		csv_file.closeFileIO();
		System.out.println("Simulation Complete.  Data saved to csv output file.");

		ExperimentMetaDataOutput.exportSimulationMetadata("Simulation4", numberOfExperiments, numberOfRuns);
		System.out.println("Simulation Metadata saved to output text file.");
	}

	public static void runSimulation5(int numberOfExperiments, int numberOfRuns) {

		SimulationEngine.colorRange = new int[] { 0, 1, 2, 3, 4, 5 };
		SimulationEngine.numOfColors = SimulationEngine.colorRange.length;
		SimulationEngine.actionCostsRange = new int[] { 10, 40, 100, 150, 250, 300, 350, 500 };

		Team.teamSize = 6;

		// Set up the CSV file for experiment output:
		String[] csv_columns = {"Run Number",
				"Unicast cost",
				"No Help Score",
				"No Help Re-plan Score",
				"AdvActionMAP Score",
				"AdvActionMAP Re-plan Score",
				"ResourceMAP Score",
				"ResourceMAP Re-plan Score"
		};


		CsvTool csv_file = new CsvTool("Simulation5Output", csv_columns);

		/* The experiments loop */

		for (int i = 1; i <= numberOfExperiments; i++) {

			/* Create the teams involved in the simulation */
			Team[] teams = new Team[Team.teamSize];

			// No help team
			teams[0] = new NoHelpTeam();

			// No help, re-plan team
			teams[1] = new NoHelpRepTeam();

			// Advanced action MAP team
			teams[2] = new AdvActionMAPTeam();

			// Advanced action MAP, re-plan
			teams[3] = new AdvActionMAPRepTeam();

			// Resource MAP
			teams[4] = new ResourceMAPTeam();

			// Resource MAP, re=plan
			teams[5] = new ResourceMAPRepTeam();


			/* Create the SimulationEngine */
			SimulationEngine se = new SimulationEngine(teams);

			/* Set the experiment-wide parameters: */

			Team.unicastCost = 2 * i;
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
			SimulationEngine.disturbanceLevel = 0.05;

			/* Initialize and run the experiment */
			se.initializeExperiment(numberOfRuns);
			int[] teamScores = se.runExperiment();

			// Add run data to csv file as a new row of data
			String[] run_data = {
					String.valueOf(i),
					String.valueOf(Team.unicastCost),
					String.valueOf(teamScores[0]),
					String.valueOf(teamScores[1]),
					String.valueOf(teamScores[2]),
					String.valueOf(teamScores[3]),
					String.valueOf(teamScores[4]),
					String.valueOf(teamScores[5])
			};

			csv_file.appendRow(run_data);

			System.out.println("Experiment " + i +" done.");
		}
		// End of all experiment runs

		csv_file.closeFileIO();
		System.out.println("Simulation Complete.  Data saved to csv output file.");

		ExperimentMetaDataOutput.exportSimulationMetadata("Simulation5", numberOfExperiments, numberOfRuns);
		System.out.println("Simulation Metadata saved to output text file.");
	}
	
public static void runSimulation6(int numberOfExperiments, int numberOfRuns) {
		
		SimulationEngine.colorRange = new int[] { 0, 1, 2, 3, 4, 5 };
		SimulationEngine.numOfColors = SimulationEngine.colorRange.length;
		SimulationEngine.actionCostsRange = new int[] { 10, 40, 100, 150, 250, 300, 350, 500 };

		Team.teamSize = 8;

		// Set up the CSV file for experiment output:
		String[] csv_columns = {"Run Number",
				"init resource coeff",
				"No Help Re-plan Score",
				"AdvActionMAP Re-plan Score",
				"ResourceMAP TB Re-plan Score"
		};


		CsvTool csv_file = new CsvTool("Simulation6Output", csv_columns);

		/* The experiments loop */

		for (int i = 1; i <= numberOfExperiments && TeamTask.initResCoef <= 200; i++) {

			/* Create the teams involved in the simulation */
			Team[] teams = new Team[3];

			// No help, re-plan team
			teams[0] = new NoHelpRepTeam();

			// Advanced action MAP, re-plan
			teams[1] = new AdvActionMAPRepTeam();
			
			// Resource MAP, re-plan
			teams[2] = new ResourceMAPRep_TBTeam();
			

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
			TeamTask.initResCoef = 10 * i;

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

			ResourceMAPRepAgent.costToGoalHelpThreshold = 1.0;
			ResourceMAPRepAgent.canSacrifice = true;

			/* vary the disturbance */
			SimulationEngine.disturbanceLevel = 0.05;

			/* Initialize and run the experiment */
			se.initializeExperiment(numberOfRuns);
			int[] teamScores = se.runExperiment();

			// Add run data to csv file as a new row of data
			String[] run_data = {
					String.valueOf(i),
					String.valueOf(TeamTask.initResCoef),
					String.valueOf(teamScores[0]),
					String.valueOf(teamScores[1]),
					String.valueOf(teamScores[2])
			};

			csv_file.appendRow(run_data);
			System.out.println("Experiment " + i +" done.");
		}
		// End of all experiment runs

		csv_file.closeFileIO();
		System.out.println("Simulation Complete.  Data saved to csv output file.");

		ExperimentMetaDataOutput.exportSimulationMetadata("Simulation6", numberOfExperiments, numberOfRuns);
		System.out.println("Simulation Metadata saved to output text file.");

	}


public static void runSimulation7(int numberOfExperiments, int numberOfRuns) {
	
	SimulationEngine.colorRange = new int[] { 0, 1, 2, 3, 4, 5 };
	SimulationEngine.numOfColors = SimulationEngine.colorRange.length;
	SimulationEngine.actionCostsRange = new int[] { 10, 40, 100, 150, 250, 300, 350, 500 };

	Team.teamSize = 8;

	// Set up the CSV file for experiment output:
	String[] csv_columns = {"Run Number",
			"unicast cost",
			"No Help Re-plan Score",
			"AdvActionMAP Re-plan Score",
			"ResourceMAP TB Re-plan Score"
	};


	CsvTool csv_file = new CsvTool("Simulation7Output", csv_columns);

	/* The experiments loop */

	for (int i = 1; i <= numberOfExperiments && Team.unicastCost <= 21; i++) {

		/* Create the teams involved in the simulation */
		Team[] teams = new Team[3];

		// No help, re-plan team
		teams[0] = new NoHelpRepTeam();

		// Advanced action MAP, re-plan
		teams[1] = new AdvActionMAPRepTeam();
		
		// Resource MAP, re-plan
		teams[2] = new ResourceMAPRep_TBTeam();
		

		/* Create the SimulationEngine */
		SimulationEngine se = new SimulationEngine(teams);

		/* Set the experiment-wide parameters: */

		Team.unicastCost = 1 * i;
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

		ResourceMAPRepAgent.costToGoalHelpThreshold = 1.0;
		ResourceMAPRepAgent.canSacrifice = true;

		/* vary the disturbance */
		SimulationEngine.disturbanceLevel = 0.05;

		/* Initialize and run the experiment */
		se.initializeExperiment(numberOfRuns);
		int[] teamScores = se.runExperiment();

		// Add run data to csv file as a new row of data
		String[] run_data = {
				String.valueOf(i),
				String.valueOf(Team.unicastCost),
				String.valueOf(teamScores[0]),
				String.valueOf(teamScores[1]),
				String.valueOf(teamScores[2])
		};

		csv_file.appendRow(run_data);
		System.out.println("Experiment " + i +" done.");
	}
	// End of all experiment runs

	csv_file.closeFileIO();
	System.out.println("Simulation Complete.  Data saved to csv output file.");

	ExperimentMetaDataOutput.exportSimulationMetadata("Simulation7", numberOfExperiments, numberOfRuns);
	System.out.println("Simulation Metadata saved to output text file.");

}

public static void runSimulation8(int numberOfExperiments, int numberOfRuns) {
	
	SimulationEngine.colorRange = new int[] { 0, 1, 2, 3, 4, 5 };
	SimulationEngine.numOfColors = SimulationEngine.colorRange.length;
	SimulationEngine.actionCostsRange = new int[] { 10, 40, 100, 150, 250, 300, 350, 500 };

	Team.teamSize = 8;

	// Set up the CSV file for experiment output:
	String[] csv_columns = {"Run Number",
			"Calculation Cost",
			"No Help Re-plan Score",
			"AdvActionMAP Re-plan Score",
			"ResourceMAP TB Re-plan Score"
	};


	CsvTool csv_file = new CsvTool("Simulation8Output", csv_columns);

	/* The experiments loop */

	for (int i = 1; i <= numberOfExperiments && Agent.calculationCost <= 15; i++) {

		/* Create the teams involved in the simulation */
		Team[] teams = new Team[3];

		// No help, re-plan team
		teams[0] = new NoHelpRepTeam();

		// Advanced action MAP, re-plan
		teams[1] = new AdvActionMAPRepTeam();
		
		// Resource MAP, re-plan
		teams[2] = new ResourceMAPRep_TBTeam();
		

		/* Create the SimulationEngine */
		SimulationEngine se = new SimulationEngine(teams);

		/* Set the experiment-wide parameters: */

		Team.unicastCost = 1;
		Team.broadcastCost = Team.unicastCost * (Team.teamSize - 1);
		Agent.calculationCost = 1 * i;
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

		ResourceMAPRepAgent.costToGoalHelpThreshold = 1.0;
		ResourceMAPRepAgent.canSacrifice = true;

		/* vary the disturbance */
		SimulationEngine.disturbanceLevel = 0.05;

		/* Initialize and run the experiment */
		se.initializeExperiment(numberOfRuns);
		int[] teamScores = se.runExperiment();

		// Add run data to csv file as a new row of data
		String[] run_data = {
				String.valueOf(i),
				String.valueOf(Agent.calculationCost),
				String.valueOf(teamScores[0]),
				String.valueOf(teamScores[1]),
				String.valueOf(teamScores[2])
		};

		csv_file.appendRow(run_data);
		System.out.println("Experiment " + i +" done.");
	}
	// End of all experiment runs

	csv_file.closeFileIO();
	System.out.println("Simulation Complete.  Data saved to csv output file.");

	ExperimentMetaDataOutput.exportSimulationMetadata("Simulation8", numberOfExperiments, numberOfRuns);
	System.out.println("Simulation Metadata saved to output text file.");

}

}


