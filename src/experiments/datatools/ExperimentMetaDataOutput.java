package experiments.datatools;

import massim.Agent;
import massim.SimulationEngine;
import massim.Team;
import massim.TeamTask;

/**
 * Created by Devin Calado on 11/18/2014.
 */
public class ExperimentMetaDataOutput {

    /**
     * Exports experimental metadata to a text file.
     * Extracts information from the current state of the simulation engine.
     *
     */
    public static void exportSimulationMetadata(String filename, int numberOfExperiments, int numberOfRuns){
        FileIO file = new FileIO(filename + "-Metadata.txt");

        // These are the experiment wide parameters:
        file.appendToFile("Experiment Name: " + filename);
        file.appendToFile("# Experiments: " + numberOfExperiments);
        file.appendToFile("# Runs per Experiment: " + numberOfRuns);

        file.appendToFile("Number of colors: " + SimulationEngine.colorRange.length);
        //file.appendToFile("Action Cost Range: " + SimulationEngine.actionCostsRange.toString());
        file.appendToFile("Number of Teams: " + Team.teamSize);
        file.appendToFile("Unicast Cost: " + Team.unicastCost);
        file.appendToFile("Broadcast Cost: " + Team.broadcastCost);
        file.appendToFile("Calculation Cost: " + Agent.calculationCost);
        file.appendToFile("Plan Cost Coefficient: " + Agent.planCostCoeff);
        file.appendToFile("Help Overhead Cost: " + TeamTask.helpOverhead);
        file.appendToFile("Cell Reward: " + TeamTask.cellReward);
        file.appendToFile("Goal Achievement Reward: " + TeamTask.achievementReward);
        file.appendToFile("Initial Resource Coefficient: " + TeamTask.initResCoef);

        file.closeWriter();
    }
}
