package massim.agents.resourcemap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import massim.*;


/**
 * Resource MAP Agent w/ Path Re-planning
 *
 * @author Devin Calado, Michael Conner
 * @version 1.0 - November 2014
 *
 */
public class ResourceMAPRepAgent extends ResourceMAPAgent {

    /**
     * The Constructor
     *
     * @param id					The agent's id; to be passed
     * 								by the team.
     * @param comMed				The instance of the team's
     * 								communication medium
     */
    public ResourceMAPRepAgent(int id, CommMedium comMed) {
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
            if (canCalcAndBCast()) {
                logInf2("Broadcasting my wellbeing to the team");
                broadcastMsg(prepareWellbeingUpMsg(wellbeing()));
            }
            setState(ResourceMAP_BaseAgent.ResMAPState.R_GET_HELP_REQ);
        }
        else
        {
            RowCol nextCell = path().getNextPoint(pos());
            int cost = getCellCost(nextCell);

            boolean needHelp = (cost > resourcePoints());// || (cost > requestThreshold);

            if (needHelp)
            {
                logInf2("Need help!");

                if (canCalcAndBCast()) {

                    // Create the help request message
                    double eCost = estimatedCost(remainingPath(pos()));
                    int remPath = remainingPath(pos()).getNumPoints();
                    double avgCellCostToGoal = eCost/remPath;
                    int nextCellCost = getCellCost(path().getNextPoint(pos()));

                    String helpReqMsg = prepareHelpReqMsg(remainingPath(pos()).getNumPoints(),
                            eCost,
                            avgCellCostToGoal,
                            nextCellCost);

                    broadcastMsg(helpReqMsg);
                    this.numOfHelpReq++;
                    setState(ResourceMAP_BaseAgent.ResMAPState.R_IGNORE_HELP_REQ);

                } else
                    setState(ResourceMAP_BaseAgent.ResMAPState.R_BLOCKED);
            }
            else
            {
                setState(ResourceMAP_BaseAgent.ResMAPState.R_GET_HELP_REQ);
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
