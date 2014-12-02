package massim.agents.resourcemap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import massim.*;

/**
 * Resource MAP Agent - Base class (based on the ResourceMAP_TBAgent)
 *
 * @author Devin Calado, Michael Conner
 * @version 1.0 - November 2014
 *
 */
public abstract class ResourceMAP_BaseAgent extends Agent {

    // Debug output flags
    boolean dbgInf = false;
    boolean dbgErr = true;
    boolean dbgInf2 = false;


    // Resource MAP toggles
    public static boolean canSacrifice = true;
    public static double costToGoalHelpThreshold = 1.1;


    // Agent states
    protected enum ResMAPState {
        S_INIT,
        S_SEEK_HELP, S_RESPOND_TO_REQ,
        S_DECIDE_OWN_ACT, S_BLOCKED, S_RESPOND_BIDS, S_BIDDING,
        R_IGNORE_HELP_REQ, R_GET_HELP_REQ,
        R_GET_BIDS, R_BIDDING, R_DO_OWN_ACT,
        R_BLOCKED,
        R_GET_BID_CONF
    }

    // Message types for MAP
    protected final static int MAP_HELP_REQ_MSG = 1;
    protected final static int MAP_BID_MSG = 2;
    protected final static int MAP_HELP_CONF = 3;
    protected final static int MAP_WB_UPDATE = 4;

    // The agents active state
    protected ResMAPState state;

    // Simulation
    protected int[][] oldBoard;
    protected double disturbanceLevel;

    // Estimated cost to goal
    protected double estimatedCostToGoal;
    
    //Next Step
    protected RowCol nextCell;

    // Well being
    public static double WLL;
    protected double[] agentsWellbeing;
    protected double lastSentWellbeing;
    public double wellbeing;

    // Bidding.  Used for communicating bid details.
    protected boolean bidding;
    protected RowCol helpeeFinalCell;
    protected ArrayList<Message> bidMsgs;
    protected ArrayList<Message> confMsgs;

    // Replanning
    protected boolean replanned = false;


    // Msg
    String msgStr;

    // Custom Comparators
    public static Comparator<Message> resourceAmountOrder;
    public static Comparator<Message> teamBenefitOrder;
    public static Comparator<Message> estimatedCostToGoalOrder;
    public static Comparator<Message> averageStepCostOrder;
    public static Comparator<Message> wellbeingOrder;
    public static Comparator<Message> tbOrder;
    public static Comparator<Message> tlossOrder;


    /**
     * The Constructor
     *
     * @param id					The agent's id; to be passed
     * 								by the team.
     * @param comMed				The instance of the team's
     * 								communication medium
     */
    public ResourceMAP_BaseAgent(int id, CommMedium comMed) {
        super(id, comMed);

        resourceAmountOrder = new Comparator<Message>() {
            @Override
            public int compare(Message m1, Message m2) { //DESC
                return Integer.compare(m2.getIntValue("resourceAmount"), m1.getIntValue("resourceAmount"));
            }
        };

        teamBenefitOrder = new Comparator<Message>() {
            @Override
            public int compare(Message m1, Message m2) { //DESC
                return Integer.compare(m2.getIntValue("teamBenefit"), m1.getIntValue("teamBenefit"));
            }
        };

        estimatedCostToGoalOrder = new Comparator<Message>() {
            @Override
            public int compare(Message m1, Message m2) { //ASC
                return Double.compare(m1.getDoubleValue("eCostToGoal"), m2.getDoubleValue("eCostToGoal"));
            }
        };

        averageStepCostOrder = new Comparator<Message>() {
            @Override
            public int compare(Message m1, Message m2) { //ASC
                return Double.compare(m1.getDoubleValue("averageStepCost"), m2.getDoubleValue("averageStepCost"));
            }
        };

        wellbeingOrder = new Comparator<Message>() {
            @Override
            public int compare(Message m1, Message m2) { //DESC
                return Double.compare(m2.getDoubleValue("wellbeing"), m1.getDoubleValue("wellbeing"));
            }
        };

        tbOrder = new Comparator<Message>() {
            @Override
            public int compare(Message m1, Message m2) { //DESC
                return Integer.compare(m2.getIntValue("teamBenefit"), m1.getIntValue("teamBenefit"));
            }
        };
            
       tlossOrder = new Comparator<Message>() {
            @Override
            public int compare(Message m1, Message m2) { //ASC
                return Integer.compare(m1.getIntValue("teamLoss"), m2.getIntValue("teamLoss"));
            }
        };
    }

    /* Initializes the agent for a new run.
     *
     * Called by Team.initializeRun()

     *
     * @param tt						The team task setting
     * @param subtaskAssignments		The subtask assignments for the team.
     * @param initResourcePoints		The initial resource points given
     * 									to the agent by its team.
     */
    public void initializeRun(TeamTask tt, int[] subtaskAssignments , RowCol[] currentPos,
                              int[] actionCosts,int initResourcePoints, int[] actionCostsRange){

        super.initializeRun(tt, subtaskAssignments,
                currentPos, actionCosts, initResourcePoints, actionCostsRange);

        logInf("Initialized for a new run.");
        logInf("My initial resource points = " + resourcePoints());
        logInf("My initial position: "+ pos());
        logInf("My goal position: " + goalPos().toString());

        oldBoard = null;
        agentsWellbeing = new double[Team.teamSize];
        lastSentWellbeing = -1;
    }

    /**
     * Initializes the agent for a new round of the game.
     *
     *
     * @param board						The game board
     * @param actionCostsMatrix			The matrix containing the action costs
     * 									for all the agents in the team (depends
     * 									on the level of mutual awareness in the
     * 									team)
     */
    @Override
    protected void initializeRound(Board board, int[][] actionCostsMatrix) {
        super.initializeRound(board, actionCostsMatrix);

        logInf("Starting a new round ...");

        if (path() == null)
        {
            findPath();
            logInf("Initial Planning: Chose this path: "+ path().toString());
        }

        logInf("My current position: " + pos().toString());

        state = ResMAPState.S_INIT;
        logInf("Set the inital state to +"+state.toString());

        setRoundAction(actionType.SKIP);

        disturbanceLevel = calcDistrubanceLevel();
        logInf("The estimated disturbance level on the board is " + disturbanceLevel);

    }

    /**
     * The agent's send states implementations.
     *
     * @return					The current communication state.
     */
    @Override
    protected AgCommStatCode sendCycle() {

        AgCommStatCode returnCode = AgCommStatCode.DONE;
        logInf("Send Cycle");


        switch(state) {
            case S_INIT:
                method_S_INIT();
                break;
            case S_RESPOND_TO_REQ:
                method_S_RESPOND_TO_REQ();
                break;
            case S_SEEK_HELP:
                setState(ResMAPState.R_GET_BIDS);
                break;
            case S_BIDDING:
                setState(ResMAPState.R_GET_BID_CONF);
                break;
            case S_DECIDE_OWN_ACT:
                setState(ResMAPState.R_DO_OWN_ACT);
                break;
            case S_RESPOND_BIDS:
                method_S_RESPOND_BIDS();
                break;
            case S_BLOCKED:
                setState(ResMAPState.R_BLOCKED);
                break;
            default:
                logErr("Unimplemented send state: " + state.toString());
        }

        return returnCode;
    }

    // Must be implemented by child classes
    public abstract void method_S_INIT();
    public abstract void method_S_RESPOND_TO_REQ();
    public abstract void method_S_RESPOND_BIDS();


    /**
     * The agent's receive states implementations.
     *
     * @return					The current communication state;
     * 							'done' when the state is final.
     */
    @Override
    protected AgCommStatCode receiveCycle() {
        AgCommStatCode returnCode = AgCommStatCode.NEEDING_TO_SEND;
        logInf("Receive Cycle");

        switch (state) {
            case R_GET_HELP_REQ:
                method_R_GET_HELP_REQ();
                break;
            case R_IGNORE_HELP_REQ:
                method_R_IGNORE_HELP_REQ();
                break;
            case R_BIDDING:
                setState(ResMAPState.S_BIDDING);
                break;
            case R_GET_BIDS:
                method_R_GET_BIDS();
                break;
            case R_BLOCKED:
                setRoundAction(actionType.FORFEIT);
                break;
            case R_GET_BID_CONF:
                method_R_GET_BID_CONF();
                break;
            case R_DO_OWN_ACT:
                method_R_DO_OWN_ACT();
                break;
            default:
                logErr("Unimplemented receive state: " + state.toString());
        }

        if (isInFinalState())
            returnCode = AgCommStatCode.DONE;

        return returnCode;
    }

    // Must be implemented by child classes
    public abstract void method_R_GET_HELP_REQ();
    public abstract void method_R_IGNORE_HELP_REQ();
    public abstract void method_R_GET_BIDS();
    public abstract void method_R_GET_BID_CONF();
    public abstract void method_R_DO_OWN_ACT();


    /**
     * Finalizes the round by moving the agent.
     *
     * Also determines the current state of the agent which can be
     * reached the goal, blocked, or ready for next round.
     *
     * @return 						Returns the current state
     */
    @Override
    protected AgGameStatCode finalizeRound() {
        logInf("Finalizing the round ...");

        keepBoard();

        boolean succeed = act();

        if (reachedGoal())
        {
            logInf("Reached the goal");
            return AgGameStatCode.REACHED_GOAL;
        }
        else
        {
            if (succeed)
                return AgGameStatCode.READY;
            else
            {
                logInf("Blocked!");
                return AgGameStatCode.BLOCKED;
            }
        }
    }

    protected boolean act() {

        boolean result = false;

        switch (super.getRoundAction()) {
            case OWN:
                setLastAction("Self");
                result = doOwnAction();
                break;
            case SKIP:
                setLastAction("Skipped");
                result = true;
                break;
            case FORFEIT:
                setLastAction("Forfeit");
                result = false;
                break;
            default:
                break;
        }

        return result;
    }

    /**
     * Checks whether agent needs help using well being and thresholds.
     *
     * @param cost					Cost of next action
     * @param wellbeing				Well being of the agent
     * @return
     */
    protected boolean checkNeedHelp(int cost, double wellbeing) {
        if (wellbeing < WLL) logInf2("Wellbeing = " + wellbeing);
        return (cost > resourcePoints());
    }

    /**
     * Calculates the team well being
     *
     * @return Team well being
     */
    protected double teamWellbeing()
    {
        double sum = 0;
        agentsWellbeing[id()] = wellbeing();
        for (double w : agentsWellbeing)
            sum+=w;

        return sum/agentsWellbeing.length;
    }

    /**
     * Calculates the standard deviation of the team's well being
     *
     * @return Standard deviation of the team's well being
     */
    protected double teamWellbeingStdDev() {

        double tw = teamWellbeing();

        double sum = 0;
        for (double w : agentsWellbeing)
        {
            sum+= (w-tw)*(w-tw);
        }

        return Math.sqrt(sum/agentsWellbeing.length);
    }

    /**
     * Calculates the disturbance level of the board.
     *
     * This compares the current state of the board with the stored state
     * from the previous round.
     *
     * @return				The level of disturbance.
     */
    protected double calcDistrubanceLevel() {
        if (oldBoard == null)
            return 0.0;

        int changeCount = 0;
        for (int i=0;i<theBoard().rows();i++)
            for (int j=0;j<theBoard().cols();j++)
                if (theBoard().getBoard()[i][j] != oldBoard[i][j])
                    changeCount++;

        return (double)changeCount / (theBoard().rows() * theBoard().cols());
    }

    /**
     * Keeps the current state of the board for calculating the disturbance
     * in the next round of the game.
     *
     * This copied theBoard into oldBoard.
     */
    protected void keepBoard() {

        int rows = theBoard().rows();
        int cols = theBoard().cols();

        if (oldBoard == null) /* first round */
            oldBoard = new int[rows][cols];

        for (int i=0;i<rows;i++)
            for (int j=0;j<cols;j++)
                oldBoard[i][j] = theBoard().getBoard()[i][j];
    }

    /**
     * Calculated the estimated cost for the agent to move through path p.
     *
     * @param p						The agent's path
     * @return						The estimated cost
     */
    protected double estimatedCost(Path p) {

        int l = p.getNumPoints();
        double sigma = 1 - disturbanceLevel;
        double eCost = 0.0;
        if (Math.abs(sigma-1) < 0.000001)
        {
            for (int k=0;k<l;k++)
                eCost += getCellCost(p.getNthPoint(k));
        }
        else
        {
            double m = getAverage(actionCosts());
            eCost = (l - ((1-Math.pow(sigma, l))/(1-sigma))) * m;
            for (int k=0;k<l;k++)
                eCost += Math.pow(sigma, k) * getCellCost(p.getNthPoint(k));
        }
        return eCost;
    }

    
    /**
     * Calculate the agent's wellbeing based on number of steps
     * 
     * @return the agents wellbeing as ratio of resources and steps
     */
    protected double wellbeingProximity() {
    	int currentLocation = path().getIndexOf(pos())+1; //returns index!, so add 1
    	int sizeOfPath = path().getNumPoints();
        int stepsToGoal = sizeOfPath - currentLocation;
        
        double proximityFactor = 2.0 - ((double)stepsToGoal / (double)sizeOfPath);
        // steps =3, path =10, 3/10=.3, 2.0-.3 = 1.7 proximity factor
        // steps =9, path =10, 9/10=.9, 2.0-.9 = 1.1 proximity factor
        
        double eCost = estimatedCostToGoal;
        if (eCost == 0)
            return resourcePoints();
        else
            return ((double)resourcePoints()/eCost)*proximityFactor;
    }
    
    /**
     * Calculates the agent's wellbeing.
     *
     * @return	The agent's wellbeing
     */
    protected double wellbeing() {
        double eCost = estimatedCostToGoal;
        if (eCost == 0)
            return resourcePoints();
        else
            return (double)resourcePoints()/eCost;
    }

    /**
     * Finds the remaining path from the given cell.
     *
     * The path DOES NOT include the given cell and the starting cell
     * of the remaining path would be the next cell.
     *
     * @param from					The cell the remaining path would be
     * 								generated from.
     * @return						The remaining path.
     */
    protected Path remainingPath(RowCol from) {
        Path rp = new Path(path());

        while (!rp.getStartPoint().equals(from))
            rp = rp.tail();

        return rp.tail();
    }


    /**
     * Finds the final position of the agent assuming using the
     * given resource points.
     *
     * @param remainingResourcePoints			The amount of resource points
     * 											the agent can use.
     * @return									The index of the agent's position
     * 											on the path, consumed all the resources;
     * 											staring from 0.
     */
    protected int findFinalPos(int remainingResourcePoints, RowCol startPos) {

        if (path().getEndPoint().equals(startPos)){
            return path().getIndexOf(startPos);
        }

        RowCol iCell = path().getNextPoint(startPos);
        int iIndex = path().getIndexOf(iCell);
        double m = getAverage(actionCosts());

        while (iIndex < path().getNumPoints())
        {
            int jIndex = iIndex - path().getIndexOf(startPos);
            double sigma = 1 - disturbanceLevel;
            double eCost =  ((1-Math.pow(sigma, jIndex))) * m;
            eCost += Math.pow(sigma, jIndex) * getCellCost(path().getNthPoint(iIndex));

            //int cost = getCellCost(iCell);
            if (eCost <= remainingResourcePoints)
            {
                remainingResourcePoints-=eCost;
                iCell=path().getNextPoint(iCell);
                iIndex++;
            }
            else
            {
                iCell = path().getNthPoint(iIndex-1);
                break;
            }
        }

        return path().getIndexOf(iCell);
    }

    /**
     * Estimates the agent's reward points at the end of the game.
     *
     * Estimates the agent's reward points at the end of the game assuming having
     * the given resources points left and being the the specified position.
     *
     * @param remainingResourcePoints			The assumed remaining resource
     * 											points
     * @param startPos							The position which the agent
     * 											starts to move along the path.
     * @return									The estimated reward points
     */
    protected int projectRewardPoints(int remainingResourcePoints, RowCol startPos) {

        if (path().getEndPoint().equals(startPos))
            return calcRewardPoints(remainingResourcePoints, startPos);

        RowCol iCell = path().getNextPoint(startPos);
        int iIndex = path().getIndexOf(iCell);

        double m = getAverage(actionCosts());

        while (iIndex < path().getNumPoints())
        {
            int jIndex = iIndex - path().getIndexOf(startPos);
            double sigma = 1 - disturbanceLevel;
            double eCost =  ((1-Math.pow(sigma, jIndex))) * m;
            eCost += Math.pow(sigma, jIndex) * getCellCost(path().getNthPoint(iIndex));

            if (eCost <= remainingResourcePoints)
            {
                remainingResourcePoints-=eCost;
                iCell=path().getNextPoint(iCell);
                iIndex++;
            }
            else
            {
                iCell = path().getNthPoint(iIndex-1);
                break;
            }
        }

        return calcRewardPoints(remainingResourcePoints, iCell);
    }

    /**
     * The importance function.
     *
     * Maps the remaining distance to the goal into
     *
     * Currently: imp(x) = 100/x
     *
     * @param remainingLength
     * @return
     */
    protected int importance(int remainingLength) {
        remainingLength ++; /* TODO: double check */
        if (remainingLength != 0)
            return 100/remainingLength;
        else
            return 0;
    }

    /**
     * Prepares a help request message and returns its String encoding.
     *
     *
     * @return						The message encoded in String
     */
    protected String prepareHelpReqMsg(double estimatedCostToGoal, int stepTB, int nextStepCost) {

        Message helpReq = new Message(id(),-1,MAP_HELP_REQ_MSG);
        //helpReq.putTuple("teamBenefit", Integer.toString(teamBenefit));
        //helpReq.putTuple("requiredResources", requiredResources);

        helpReq.putTuple("eCostToGoal", estimatedCostToGoal);
        helpReq.putTuple("teamBenefit", stepTB);
        helpReq.putTuple("nextStepCost", nextStepCost);

        helpReq.putTuple("wellbeing", wellbeing);

        return helpReq.toString();
    }

    /**
     * Prepares a help request message and returns its String encoding.
     *
     * @param w			The team benefit to be included in
     * 								the message.
     * @return						The message encoded in String
     */
    protected String prepareWellbeingUpMsg(double w) {

        Message wu = new Message(id(),-1,MAP_WB_UPDATE);

        wu.putTuple("wellbeing", Double.toString(w));
        lastSentWellbeing = w;

        return wu.toString();
    }

    /**
     * Prepares a bid message and returns its String encoding.
     *
     * @param requester				The help requester agent
     * @return						The message encoded in String
     */
    protected Message prepareBidMsg(int requester, int resourceAmount, int teamLoss, double helperWellBeing) {
        Message bidMsg = new Message(id(),requester,MAP_BID_MSG);
        bidMsg.putTuple("resourceAmount", resourceAmount);
        bidMsg.putTuple("requester", requester);
        bidMsg.putTuple("teamLoss", teamLoss); //MC nov 27 2014
        bidMsg.putTuple("wellbeing", helperWellBeing );

        return bidMsg;
    }

    /**
     * Prepares  a list of help confirmed messages for a list of helpers
     * encoding.
     *
     * @param helperID				The helper agent
     * @return						The message encoded in String
     */
    protected Message prepareConfirmMsg(int returnedResources, int helperID) {
        Message confirmMessage = new Message(id(), helperID, MAP_HELP_CONF);
        confirmMessage.putTuple("returnedResources", returnedResources);
        return confirmMessage;
    }


    /**
     * Calculates the team loss considering spending the given amount
     * of resource points to help.
     *
     * @param resourcesToSend				The amount of resources to send
     * @return							The team loss
     */
    protected int calcTeamLoss(int resourcesToSend)
    {
        decResourcePoints(Agent.calculationCost);

        int withHelpRewards = projectRewardPoints(resourcePoints()- resourcesToSend, pos());

        int noHelpRewards = projectRewardPoints(resourcePoints(),pos());

        return (noHelpRewards - withHelpRewards);
    }

    /**
     * Calculates the team benefit for using the available resources to move towards the goal.
     *
     * @return						The team benefit.
     */
    protected int calcTeamBenefit(int helpResourcePoints, RowCol skipCell) {

        decResourcePoints(Agent.calculationCost);

        //Calc PATH rewards with help
        int withHelpRewards = projectRewardPoints(resourcePoints() + helpResourcePoints, skipCell);
        //Calc PATH rewards with no help
        int noHelpRewards = projectRewardPoints(resourcePoints(), pos());


        return (withHelpRewards - noHelpRewards);
    }

    /**
     * Enables the agent to perform its own action.
     *
     * To be overriden by the agent if necessary.
     *
     * @return						true if successful/false o.w.
     */
    @Override
    protected boolean doOwnAction() {
        RowCol nextCell = path().getNextPoint(pos());
        int cost = getCellCost(nextCell);
        logInf("Should do my own move!");
        if (resourcePoints() >= cost )
        {
            decResourcePoints(cost);
            setPos(nextCell);
            logInf("Moved to " + pos().toString());
            return true;
        }
        else
        {
            logErr("Could not do my own move :(");
            return false;
        }
    }

    /**
     * Enables the agent do any bookkeeping while receiving help.
     *
     * To be overridden by the agent if necessary.
     *
     * @return						true if successful/false o.w.
     */
    @Override
    protected boolean doGetHelpAction() {
        // An agent will be performing its own action, with the resources given by another agent
        RowCol nextCell = path().getNextPoint(pos());
        int cost = getCellCost(nextCell);

        if (resourcePoints() >= cost )
        {
            decResourcePoints(cost);
            setPos(nextCell);
            logInf("Moved to " + pos().toString());
            return true;
        }
        else
        {
            logErr("Could not do my own move :(");
            return false;
        }

    }

    /**
     * Checks whether the agent is in a final state or not.
     *
     * @return						true if is in a final state /
     * 								false otherwise
     */
    protected boolean isInFinalState() {
        switch (state) {
            case R_DO_OWN_ACT:
            case R_BLOCKED:
                return true;
            default:
                return false;
        }
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
     * Returns a two dimensional array representing the estimated cost
     * of cells with i, j coordinates
     *
     * @author Mojtaba
     */
    protected int[][] estimBoardCosts(int[][] board) {

        int[][] eCosts = new int[board.length][board[0].length];

        for (int i = 0; i < eCosts.length; i++)
            for (int j = 0; j < eCosts[0].length; j++) {

                eCosts[i][j] = estimCellCost(i ,j);
            }

        return eCosts;
    }

    /**
     * Returns estimated cost of a cell with k steps from current position
     *
     * @param i				cell coordinate
     * @param j				cell coordinate
     * @author Mojtaba
     */
    protected int estimCellCost(int i, int j) {
        double sigma = 1 - disturbanceLevel;
        double m = getAverage(actionCosts());
        int k = Math.abs((currentPositions[mySubtask()].row - i)) + Math.abs((currentPositions[mySubtask()].col - j));

        int eCost = (int) (Math.pow(sigma, k) * actionCosts[theBoard.getBoard()[i][j]]  + (1 - Math.pow(sigma, k)) * m);
        return eCost;
    }

    /*******************************************************************/

    /**
     * Tells whether the agent has enough resources to send a unicast
     * message or not
     *
     * @return 					true if there are enough resources /
     * 							false if there aren't enough resources
     */
    protected boolean canSend() {
        return (resourcePoints() >= Team.unicastCost);
    }

    /**
     * Tells whether the agent has enough resources to send a broadcast
     * message or not
     *
     * @return 					true if there are enough resources /
     * 							false if there aren't enough resources
     */
    protected boolean canBCast() {
        return (resourcePoints() >= Team.broadcastCost);
    }

    /**
     * Indicates whether the agent has enough resources to do calculations.
     *
     * @return					true if there are enough resources /
     * 							false if there aren't enough resources
     */
    protected boolean canCalc() {
        return (resourcePoints() >= Agent.calculationCost);
    }

    /**
     * Indicates whether the agent has enough resources to do calculations and broadcast a message.
     *
     * @return					true if there are enough resources /
     * 							false if there aren't enough resources
     */
    protected boolean canCalcAndBCast() {
        return (resourcePoints() >= (Agent.calculationCost) + Team.broadcastCost);
    }



    /**
     * Broadcast the given String encoded message.
     *
     * @param msg				The String encoded message
     */
    protected void broadcastMsg(String msg) {
        decResourcePoints(Team.broadcastCost);
        commMedium().broadcast(id(), msg);
    }

    /**
     * Sends the given String encoded message to the specified
     * receiver through the communication medium.
     *
     * @param receiver			The receiver's id
     * @param msg				The String encoded message
     */
    protected void sendMsg(int receiver, String msg) {
        decResourcePoints(Team.unicastCost);
        commMedium().send(id(), receiver, msg);
    }

    /**
     * Calculates the average of the given integer array.
     *
     * @return						The average.
     */
    protected double getAverage(int[] array) {
        int sum = 0;
        for (int i=0;i<array.length;i++)
            sum+=array[i];
        return (double)sum/array.length;
    }

    /**
     * Changes the current state of the agents state machine.
     *
     * @param newState				The new state
     */
    protected void setState(ResMAPState newState) {
        logInf("In "+ state.toString() +" state");
        state = newState;
        logInf("Set the state to +"+state.toString());
    }


    /**
     * Prints the log message into the output if the information debugging
     * level is turned on (debuggingInf).
     *
     * @param msg					The desired message to be printed
     */
    protected void logInf(String msg) {
        if (dbgInf)
            System.out.println("[ResourceMAP Agent " + id() + "]: " + msg);
        //Denish, 2014/03/30
        super.logInf(msg);
    }

    /**
     * Prints the log message into the output if the information debugging
     * level is turned on (debuggingInf).
     *
     * @param msg					The desired message to be printed
     */
    protected void logInf2(String msg) {
        if (dbgInf2)
            System.err.println("[ResourceMAP Agent " + id() + "]: " + msg);
    }

    /**
     * Prints the log message into the output if the  debugging level
     * is turned on (debuggingInf).
     *
     * @param msg					The desired message to be printed
     */
    protected void logErr(String msg) {
        if (dbgErr)
            System.out.println("[xxxxxxxxxxx][ResourceMAP Agent " + id() +
                    "]: " + msg);
    }
}