package massim.agents.resourcemap;

/**
 * ResourceMAP Team
 *
 * @author Devin Calado, Michael Conner
 * @version 1.0 - November 2014
 */

import massim.Team;

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
