package massim.agents.resourcemap;

/**
 * ResourceMAP Team
 *
 * @author Devin Calado, Michael Conner
 * @version 1.0 - November 2014
 */

import massim.Team;

public class ResourceMAPRep_TBTeam extends Team {
	/**
	 * New team - ResourceMAPTeam
	 */
	public ResourceMAPRep_TBTeam() {

		super();
		ResourceMAPRep_TBAgent[] resourceMAP_TBAgents = new ResourceMAPRep_TBAgent[Team.teamSize];

		for (int i = 0; i < Team.teamSize; i++)
			resourceMAP_TBAgents[i] = new ResourceMAPRep_TBAgent(i, commMedium());

		setAgents(resourceMAP_TBAgents);
	}
	

	
}
