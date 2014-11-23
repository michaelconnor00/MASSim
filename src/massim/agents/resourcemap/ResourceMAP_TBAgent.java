package massim.agents.resourcemap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import massim.*;


/**
 * Resource MAP Team Well Being Agent (Inherits directly from superclass)
 *
 * @author Devin Calado, Michael Conner
 * @version 1.0 - November 2014
 *
 */
public class ResourceMAP_TBAgent extends ResourceMAP_BaseAgent {


	/**
	 * The Constructor
	 *
	 * @param id					The agent's id; to be passed
	 * 								by the team.
	 * @param comMed				The instance of the team's
	 * 								communication medium
	 */
	public ResourceMAP_TBAgent(int id, CommMedium comMed) {
		super(id, comMed);
	}

}
