package massim.agents.resourcemap;

/**
 * ResourceMAP Team
 *
 * @author Devin Calado, Michael Conner
 * @version 1.0 - November 2014
 */

import massim.Team;

public class ResourceMAP_TBTeam extends Team {
	/**
	 * New team - ResourceMAPTeam
	 */
	public ResourceMAP_TBTeam() {

		super();
		ResourceMAP_TBAgent[] resourceMAP_TBAgents = new ResourceMAP_TBAgent[Team.teamSize];

		for (int i = 0; i < Team.teamSize; i++)
			resourceMAP_TBAgents[i] = new ResourceMAP_TBAgent(i, commMedium());

		setAgents(resourceMAP_TBAgents);
	}
	

	
}
