package massim.agents.resourcemap;

/**
 * ResourceMAP Team
 *
 * @author Devin Calado, Michael Conner
 * @version 1.0 - November 2014
 */

import massim.Team;

public class ResourceMAPRepTeam extends Team {

    /**
     * New team - ResourceMAPTeam
     */
    public ResourceMAPRepTeam() {

        super();
        ResourceMAPRepAgent[] resourceMAPRepAgents = new ResourceMAPRepAgent[Team.teamSize];

        for (int i = 0; i < Team.teamSize; i++)
            resourceMAPRepAgents[i] = new ResourceMAPRepAgent(i, commMedium());

        setAgents(resourceMAPRepAgents);
    }



}
