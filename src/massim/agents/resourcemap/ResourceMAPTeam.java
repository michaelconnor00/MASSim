package massim.agents.resourcemap;

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
