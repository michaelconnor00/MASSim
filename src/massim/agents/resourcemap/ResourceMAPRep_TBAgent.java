package massim.agents.resourcemap;

import massim.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;


/**
 * Resource MAP Replanning TB Agent
 *
 * @author Devin Calado, Michael Conner
 * @version 1.0 - November 2014
 *
 */
public class ResourceMAPRep_TBAgent extends ResourceMAP_BaseAgent {

	/**
	 * The Constructor
	 *
	 * @param id					The agent's id; to be passed
	 * 								by the team.
	 * @param comMed				The instance of the team's
	 * 								communication medium
	 */
	public ResourceMAPRep_TBAgent(int id, CommMedium comMed) {
		super(id, comMed);
	}



	public void method_S_INIT(){
		if (dbgInf2)
		{
			for(int i=0;i<Team.teamSize;i++)
				System.out.println("Agent "+i+":" + agentsWellbeing[i]);
		}

		if(canReplan()) {
			replan();
			replanned = true;
		}
		else {
			//logErr("Could not replan " + resourcePoints());
		}


		if (reachedGoal())
		{
			if (canBCast()) {
				logInf2("Broadcasting my wellbeing to the team");
				broadcastMsg(prepareWellbeingUpMsg(wellbeing()));
			}
			setState(ResMAPState.R_GET_HELP_REQ);
		}
		else
		{
			RowCol nextCell = path().getNextPoint(pos());
			int cost = getCellCost(nextCell);

			boolean needHelp = checkNeedHelp(cost, wellbeing);

			if (needHelp)
			{
				logInf2("Need help!");

				if (canCalc()) {

					int helpAmount = cost - (resourcePoints() - Team.broadcastCost - Agent.calculationCost);

					int teamBenefit = calcTeamBenefit(helpAmount, nextCell);

					if (canBCast() && canCalc()){

						// Create the help request message
						double eCost = estimatedCost(remainingPath(pos()));
						int remPath = remainingPath(pos()).getNumPoints();

						String helpReqMsg = prepareHelpReqMsg(
								eCost, //estimated cost to goal
								teamBenefit, //teambenefit
								cost //next step cost
						);

						logInf("Broadcasting help");
						logInf("Team benefit of help would be "+teamBenefit);

						broadcastMsg(helpReqMsg);
						this.numOfHelpReq++;
						setState(ResMAPState.R_IGNORE_HELP_REQ);

					} else {
						setState(ResMAPState.R_BLOCKED);
					}
				}
			}
			else
			{
				setState(ResMAPState.R_GET_HELP_REQ);
			}
		}
	}


	/**
	 * Does Replanning
	 */
	private void replan()
	{
		findPath();
		//repRound = roundCount;
		logInf("Re-planning: Chose this path: " + path().toString());
		numOfReplans++;
		replanned = true;
	}

	/**
	 * Finds the lowest cost path among shortest paths of a rectangular board
	 * based on the Polajnar's algorithm V2.
	 *
	 * The method uses the agents position as the starting point and the goal
	 * position as the ending point of the path.
	 *
	 * @author Mojtaba
	 */
	@Override
	protected void findPath() {
		if (mySubtask() != -1)
		{
			PolajnarPath2 pp = new PolajnarPath2();
			Path shortestPath = new Path(pp.findShortestPath(
					estimBoardCosts(theBoard.getBoard()),
					currentPositions[mySubtask()], goalPos()));

			path = new Path(shortestPath);

			int pCost = planCost();
			replanCosts += pCost;
			decResourcePoints(pCost);
		}
		else
			path = null;
	}

	/**
	 * Checks whether the agent has enough resources in order to replan
	 *
	 * @author Mojtaba
	 */
	private boolean canReplan() {
		return (resourcePoints() >= planCost());
	}

}
