package massim.agents.resourcemap;

/**
 * ResourceMAP Team
 * 
 * @author Devin Calado 
 */

import massim.Team;
import massim.agents.advancedactionmap.AdvActionMAPRepAgent;

public class ResourceMAPTeam extends Team {
	/**
	 * New team - ResourceMAPTeam
	 */
	public ResourceMAPTeam() {

		super();
		ResourceMAPAgent[] resourceMAPAgents = new ResourceMAPAgent[Team.teamSize];

		for (int i = 0; i < Team.teamSize; i++)
			resourceMAPAgents[i] = new ResourceMAPAgent(i, commMedium());

		setAgents(resourceMAPAgents);
	}
	

	
}
