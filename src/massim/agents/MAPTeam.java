package massim.agents;

import massim.RowCol;
import massim.Team;

public class MAPTeam extends Team {

	public int initResCoef = 100;
	public static int colorPenalty; // as in the old simulations
	
	public MAPTeam() {
		super();
		
		MAPAgent[] agents = new MAPAgent[teamSize];
		
		for (int i=0;i<teamSize;i++)
			agents[i] = new MAPAgent(i,env());
		
		setAgents(agents);
	}
	
	public void reset(RowCol[] agentsPos, int[][] actionCostsMatrix) {
		super.reset(agentsPos, actionCostsMatrix);
		
		for(int i=0;i<teamSize;i++)
			agent(i).incResourcePoints(initResCoef*12);
		
	}
}

