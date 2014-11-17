package massim.agents.resourcemap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import massim.*;


/**
 * Resource MAP Agent
 * 
 * @author Devin Calado, Michael Conner
 * @version 1.0 - November 2014
 * 
 */
public class ResourceMAPAgent extends Agent {

	// Debug output flags
	boolean dbgInf = false;
	boolean dbgErr = true;
	boolean dbgInf2 = false;


	// Adjustable Parameters:

	// Re-Planning
	public static boolean replanning = false;

	// Request and cost thresholds
	public static double requestThreshold;  // Devin says: Check these... I don't think we need them.
	public static double lowCostThreshold;
	public static double EPSILON;

	// Resource MAP toggles
	public static boolean canSacrifice;
	//public static boolean multipleBids;  // Maybe implement this in the future
	public static double costToGoalHelpThreshold = 1.1;


	// Agent states
	private enum ResMAPState {
		S_INIT,
		S_SEEK_HELP, S_RESPOND_TO_REQ,
		S_DECIDE_OWN_ACT, S_BLOCKED, S_RESPOND_BIDS, S_BIDDING,
		R_IGNORE_HELP_REQ, R_GET_HELP_REQ,
		R_GET_BIDS, R_BIDDING, R_DO_OWN_ACT,
		R_BLOCKED,
		R_GET_BID_CONF
	}
	

	
	// Message types for MAP
	private final static int MAP_HELP_REQ_MSG = 1;
	private final static int MAP_BID_MSG = 2;
	private final static int MAP_HELP_CONF = 3;
	private final static int MAP_WB_UPDATE = 4;
	
	// The agents active state
	private ResMAPState state;

	// Simulation 
	private int[][] oldBoard;
	private double disturbanceLevel;
	
	// Well being
	public static double WLL;
	private double[] agentsWellbeing;
	private double lastSentWellbeing;
	
	// Bidding.  Used for communicating bid details.
	private boolean bidding;
	private RowCol helpeeFinalCell;
	private ArrayList<Message> bidMsgs;
	private ArrayList<Integer> helperAgents;  // List of agents who assisted with resources in this round
	
	
	// Team well-being counters
	public static int cond1count = 0;
	public static int cond2count = 0;
	public static int cond3count = 0;
	public static int cond21count = 0;

	// Replanning
	private boolean replanned = false;

	// Custom Comparators
	private static Comparator<Message> resourceAmountOrder;
	private static Comparator<Message> teamBenefitOrder;
	private static Comparator<Message> estimatedCostToGoalOrder;
	private static Comparator<Message> averageStepCostOrder;

	/**
	 * The Constructor
	 *
	 * @param id					The agent's id; to be passed
	 * 								by the team.
	 * @param comMed				The instance of the team's 
	 * 								communication medium
	 */
	public ResourceMAPAgent(int id, CommMedium comMed) {
		super(id, comMed);
		
		resourceAmountOrder = new Comparator<Message>() {
            @Override
            public int compare(Message m1, Message m2) {
                return Integer.compare(m2.getIntValue("resourceAmount"), m1.getIntValue("resourceAmount"));
            }
        };
        
        teamBenefitOrder = new Comparator<Message>() {
            @Override
            public int compare(Message m1, Message m2) {
                return Integer.compare(m2.getIntValue("teamBenefit"), m1.getIntValue("teamBenefit"));
            }
        };

		estimatedCostToGoalOrder = new Comparator<Message>() {
			@Override
			public int compare(Message m1, Message m2) {
				return Integer.compare(m1.getIntValue("teamBenefit"), m2.getIntValue("teamBenefit"));
			}
		};

		averageStepCostOrder = new Comparator<Message>() {
			@Override
			public int compare(Message m1, Message m2) {
				return Integer.compare(m1.getIntValue("teamBenefit"), m2.getIntValue("teamBenefit"));
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
			
			double wellbeing = wellbeing();
			double twb = teamWellbeing();
			double twbSD = teamWellbeingStdDev();
			
			logInf2("My wellbeing = " + wellbeing);
			logInf2("Team wellbeing = "+twb);
			logInf2("Team wellbeing std dev = " + twbSD);
			
			if (twb < 0.8 && twbSD < 0.5)
				WLL -= 0.3;
			if (twb > 0.8 && twbSD > 0.5)
				WLL += 0.1;
			if (twb > 0.8 && twbSD < 0.5)
				WLL += 0.3;
				
			if (dbgInf2)
			{
				for(int i=0;i<Team.teamSize;i++)
					System.out.println("Agent "+i+":" + agentsWellbeing[i]);
			}


			if(replanning && canReplan())
				replan();
			else {
				logErr("Could not replan " + resourcePoints());
			}


			if (reachedGoal())
			{
				if (Math.abs((wellbeing - lastSentWellbeing)/lastSentWellbeing) < EPSILON)
					if (canBCast()) {
					logInf2("Broadcasting my wellbeing to the team");
					broadcastMsg(prepareWellbeingUpMsg(wellbeing));
					}
				setState(ResMAPState.R_GET_HELP_REQ);
			}
			else 
			{
				RowCol nextCell = path().getNextPoint(pos());			
				int cost = getCellCost(nextCell);
				
				boolean needHelp = (cost > resourcePoints()) || (cost > requestThreshold);  // Devin : request threshold required?
				
				// Condition counters
				if (cost > resourcePoints()) cond1count++;
				if ((wellbeing < WLL && cost > lowCostThreshold)) cond2count++;
				if (cost > requestThreshold) cond3count++;
				if (wellbeing < WLL) cond21count++;
				
				if (needHelp)
				{							
					logInf2("Need help!");

					if (canCalc() && canBCast()) {

						// Create the help request message
						double eCost = estimatedCost(remainingPath(pos()));
						double avgCellCostToGoal = estimatedCost(path)/remainingPath(pos()).getNumPoints();
						int nextCellCost = getCellCost(path().getNextPoint(pos()));

						String helpReqMsg = prepareHelpReqMsg(remainingPath(pos()).getNumPoints(),
											eCost,
											avgCellCostToGoal,
											nextCellCost);

						broadcastMsg(helpReqMsg);
						this.numOfHelpReq++;
						setState(ResMAPState.R_IGNORE_HELP_REQ);

					} else
						setState(ResMAPState.R_BLOCKED);
				}
				else
				{
					setState(ResMAPState.R_GET_HELP_REQ);
				}
			}			
			break;
			
		case S_RESPOND_TO_REQ:
			if(bidding && canSend())
			{
				for (Message msg : bidMsgs){
					logInf("Sending a bid to agent" + msg.getIntValue("requester"));
					sendMsg(msg.getIntValue("requester"), msg.toString());
					this.numOfBids++;
				}
				
				setState(ResMAPState.R_BIDDING);
			}
			
			else
			{
				int cost = getCellCost(path().getNextPoint(pos()));
				if (cost <= resourcePoints())
					setState(ResMAPState.R_DO_OWN_ACT);
				else
					setState(ResMAPState.R_BLOCKED);
			}							
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
			// Will have to respond to multiple bids here...  How much can we give back to an agent (to R_GET_BIDS)
			if (canSend())
			{
				logInf("Confirming the help offer(s) of " + helperAgents.size() + " agents.");
			
				// Prepare and send messages to all the bids
				Message[] messages = prepareConfirmMsgs(helperAgents);
			
				for (int i = 0 ; i < messages.length ; i++){
					sendMsg(helperAgents.get(i), messages[i].toString());
				}
				
				// This send message will need to respond with unused resources (resources we can give back)? maybe not
				
				setState(ResMAPState.R_DO_OWN_ACT);
			}
			else
				setState(ResMAPState.R_BLOCKED); 
			/* should be checked if can not send ... */
			break;
			
		case S_BLOCKED:
			setState(ResMAPState.R_BLOCKED);
			break;
			
		default:
			logErr("Unimplemented send state: " + state.toString());
		}
		
		return returnCode;
	}
	
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
			ArrayList<Message> helpReqMsgs = new ArrayList<Message>();
			
			String msgStr = commMedium().receive(id());
			while (!msgStr.equals(""))
			{
				logInf("Received a message: " + msgStr);
				Message msg = new Message(msgStr);				
				
				if (msg.isOfType(MAP_HELP_REQ_MSG))
						helpReqMsgs.add(msg);
				
				else if (msg.isOfType(MAP_WB_UPDATE))
				{
					agentsWellbeing[msg.sender()] = msg.getDoubleValue("wellbeing");
					logInf("Received agent "+msg.sender()+ "'s wellbeing = " + agentsWellbeing[msg.sender()]);
				}
				 msgStr = commMedium().receive(id());
			}

			if (helpReqMsgs.size() > 0) {
				bidding = false;
				bidMsgs = new ArrayList();

				logInf("Received " + helpReqMsgs.size() + " help requests");

				// Sort help request messages from cheapest cost to goal to most expensive
				Collections.sort(helpReqMsgs, estimatedCostToGoalOrder);

				// Bidding for helping achieve goal
				for (int i = 0; (i < helpReqMsgs.size()) && canSacrifice && !bidding; i++) {
					Double reqECostToGoal = helpReqMsgs.get(i).getDoubleValue("eCostToGoal");
					int requesterAgent = helpReqMsgs.get(i).sender();

					if (((estimatedCost(remainingPath(pos())) / reqECostToGoal) > costToGoalHelpThreshold) &&
							(resourcePoints + calculationCost + Team.unicastCost) >= reqECostToGoal) {
						bidMsgs.add(prepareBidMsg(requesterAgent, reqECostToGoal, wellbeing()));
						helpReqMsgs.remove(helpReqMsgs.get(i));
						bidding = true;
						break;
					}
				}

				int myMaxAssistance = resourcePoints - (int) estimatedCost(remainingPath(pos()));

				// Sort by average step cost to goal, in ascending order.
				Collections.sort(helpReqMsgs, averageStepCostOrder);

				// Bidding for helping achieve next cell
				for (int i = 0; (i < helpReqMsgs.size()) && !bidding; i++) {
					int reqStepsToGoal = helpReqMsgs.get(i).getIntValue("stepsToGoal");
					double reqAvgStepCostToGoal = helpReqMsgs.get(i).getDoubleValue("averageStepCost");
					int reqNextStepCost = helpReqMsgs.get(i).getIntValue("nextStepCost");
					int requesterAgent = helpReqMsgs.get(i).sender();

					// Helper has enough resources to reach their own goal
					if ((estimatedCost(remainingPath(pos())) <= resourcePoints) &&
							myMaxAssistance > reqNextStepCost) {
						bidMsgs.add(prepareBidMsg(requesterAgent, reqNextStepCost, wellbeing()));
					}
					// Helper does not have enough resource points to get to their goal
					// My average step costs from current position to the goal is greater than the help requester's.
					else if ((remainingPath(pos()).getNumPoints() / estimatedCost(remainingPath(pos()))) > reqAvgStepCostToGoal) {
						bidMsgs.add(prepareBidMsg(requesterAgent, reqNextStepCost, wellbeing()));
					}

				}

			}
			setState(ResMAPState.S_RESPOND_TO_REQ);
			break;
	
		case R_IGNORE_HELP_REQ:
			msgStr = commMedium().receive(id());
			while (!msgStr.equals(""))
			{
				logInf("Received a message: " + msgStr);
				Message msg = new Message(msgStr);				
				if (msg.isOfType(MAP_WB_UPDATE) || msg.isOfType(MAP_HELP_REQ_MSG) )
				{
					agentsWellbeing[msg.sender()] = msg.getDoubleValue("wellbeing");
					logInf("Received agent "+msg.sender()+ "'s wellbeing = " +
					agentsWellbeing[msg.sender()]);
				}
				 msgStr = commMedium().receive(id());
			}
			setState(ResMAPState.S_SEEK_HELP);
			break;
			
		case R_BIDDING:
			setState(ResMAPState.S_BIDDING);
			break;
			
		case R_GET_BIDS:
			ArrayList<Message> bidMsgs = new ArrayList<Message>();
			
			msgStr = commMedium().receive(id());
			while (!msgStr.equals(""))
			{
				logInf("Received a message: " + msgStr);
				Message msg = new Message(msgStr);				
				if (msg.isOfType(MAP_BID_MSG))
					bidMsgs.add(msg);
				 msgStr = commMedium().receive(id());
			}
			
			helperAgents = new ArrayList<Integer>();
			
			if (bidMsgs.size() == 0)
			{							
				logInf("Nobody has offered me assistance!");
				this.numOfUnSucHelpReq++;
				
				int cost = getCellCost(path().getNextPoint(pos()));
				if (cost <= resourcePoints())
					setState(ResMAPState.S_DECIDE_OWN_ACT);
				else{
					setState(ResMAPState.S_BLOCKED);
					logInf("Now I'm blocked!");
				}
			}
			
			else
			{
				
				logInf("Received " + bidMsgs.size()+" bids.");
				
				int maxBid = Integer.MIN_VALUE;	
				
				// Sort the list of bids by amounts...
				
				for (Message bid : bidMsgs)
				{
					int bidNTB = bid.getIntValue("NTB");
					//int offererAgent = bid.sender();
					
					 // Compare the net team benefit of the bid to the current max bid
					if (bidNTB > maxBid) 
					{
						maxBid = bidNTB;
					}
				}
				//helperAgents.add(offererAgent); 

				// Prepare response messages for all bidding agents (mainly, how much resources to return)
				// Create a global array to be used in S_RESPOND_BIDS...
				
				logInf(helperAgents.size() +" agents have won the bidding. (Will be providing assistance)");
				setState(ResMAPState.S_RESPOND_BIDS);
			}		
			break;
			
		case R_BLOCKED:
			//TODO: ? skip the action
			//TODO: What if distrubance? replan?
			// or forfeit
			setRoundAction(actionType.FORFEIT);
			break;
			
		case R_GET_BID_CONF:
			
			// Recieve bid confirmation and add returned resources to agent's resources.
			
			msgStr = commMedium().receive(id());
			
			if (!msgStr.equals("") && (new Message(msgStr)).isOfType(MAP_HELP_CONF))				
			{
				//int returnedResources;  // resources to be re-added to an agents available resources
				logInf("Received confirmation, my resource offering was used.");  // Output information about full or partial resource use
				this.numOfSucOffers++;
				//setState(ResMAPState.S_DECIDE_HELP_ACT);
			}
			
			else
			{
				logInf("Didn't received confirmation, my resources were not used.");	// The resources were not used											
			}
			
			// perform our own action 
			RowCol nextCell = path().getNextPoint(pos());			
			int nextCost = getCellCost(nextCell);
			if (nextCost <= resourcePoints())
				setState(ResMAPState.S_DECIDE_OWN_ACT);
			else{
				setState(ResMAPState.S_BLOCKED);
				logInf("I just gave my resources, and now I can't make a move");
			}
			break;
			
		case R_DO_OWN_ACT:
			//TODO: Check this
			int cost = getCellCost(path().getNextPoint(pos()));			
			if (!reachedGoal() && cost <= resourcePoints())
			{
				logInf("Will do my own move.");
				setRoundAction(actionType.OWN);
			}
			else
			{
				logInf("Nothing to do at this round.");
				setRoundAction(actionType.SKIP);
			}
			break;
			
		default:			
			logErr("Unimplemented receive state: " + state.toString());
		}
		
		if (isInFinalState())
			returnCode = AgCommStatCode.DONE;
		
		return returnCode;
	}
	

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
			else  /* TODO: The logic here should be changed!*/
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
			break;/*
		case HAS_HELP:
			//result = doGetHelpAction();
			break;
		case HELP_ANOTHER:
			//result = doHelpAnother();
			break;*/
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
	 * Calculates the team well being
	 *
	 * @return Team well being
	 */
	private double teamWellbeing()
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
	private double teamWellbeingStdDev() {

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
	private double calcDistrubanceLevel() {
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
	private void keepBoard() {
		
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
	private double estimatedCost(Path p) {		
		
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
	 * Calculates the agent's wellbeing.
	 * 
	 * @return	The agent's wellbeing
	 */
	protected double wellbeing() {		
		double eCost = estimatedCost(remainingPath(pos()));
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
	private Path remainingPath(RowCol from) {
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
	private int findFinalPos(int remainingResourcePoints, RowCol startPos) {
		
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
	private int projectRewardPoints(int remainingResourcePoints, RowCol startPos) {
		
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
	private int importance(int remainingLength) {
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
	private String prepareHelpReqMsg(int stepsToGoal, double estimatedCostToGoal, double averageCellCost, int nextStepCost) {
		
		Message helpReq = new Message(id(),-1,MAP_HELP_REQ_MSG);
		//helpReq.putTuple("teamBenefit", Integer.toString(teamBenefit));
		//helpReq.putTuple("requiredResources", requiredResources);

		helpReq.putTuple("eCostToGoal", estimatedCostToGoal);
		helpReq.putTuple("stepsToGoal", stepsToGoal);
		helpReq.putTuple("averageStepCost", averageCellCost);
		helpReq.putTuple("nextStepCost", nextStepCost);

		Double w = wellbeing();
		helpReq.putTuple("wellbeing", Double.toString(w));
		lastSentWellbeing = w;
		
		return helpReq.toString();
	}
	
	/**
	 * Prepares a help request message and returns its String encoding.
	 * 
	 * @param w			The team benefit to be included in
	 * 								the message.
	 * @return						The message encoded in String
	 */
	private String prepareWellbeingUpMsg(double w) {
		
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
	private Message prepareBidMsg(int requester, double resourceAmount, double helperWellBeing) {
		Message bidMsg = new Message(id(),requester,MAP_BID_MSG);
		bidMsg.putTuple("resourceAmount", resourceAmount);
		bidMsg.putTuple("requester", requester);
		bidMsg.putTuple("wellBeing", helperWellBeing );
		
		return bidMsg;
	}
	
	/**
	 * Prepares  a list of help confirmed messages for a list of helpers 
	 * encoding.
	 * 
	 * @param helpers				The helper agent
	 * @return						The message encoded in String
	 */
	private Message[] prepareConfirmMsgs(ArrayList<Integer> helpers) {
		Message[] confirmMessages = new Message[helpers.size()];
		for (int i = 0 ; i < helpers.size() ; i++){
			confirmMessages[i] = new Message(id(), helpers.get(i), MAP_HELP_CONF);
		}
		return confirmMessages;
	}
	
	/**
	 * Calculates the team loss considering spending the given amount 
	 * of resource points to help. 
	 * 
	 * @param resourcesToSend				The amount of resources to send
	 * @return							The team loss
	 */
	private int calcTeamLoss(int resourcesToSend)
	{
		decResourcePoints(Agent.calculationCost);
		
		int withHelpRewards = projectRewardPoints(resourcePoints()- resourcesToSend, pos());
						
		int noHelpRewards = projectRewardPoints(resourcePoints(),pos());
						
		int withHelpRemPathLength = path().getNumPoints() - findFinalPos(resourcePoints()- resourcesToSend, pos()) - 1;
					
		int noHelpRemPathLength = path().getNumPoints() - findFinalPos(resourcePoints(), pos()) - 1;
				
		return  
			(noHelpRewards - withHelpRewards) *
			(1 + (importance(noHelpRemPathLength)-importance(withHelpRemPathLength)) *
			(withHelpRemPathLength-noHelpRemPathLength)) + TeamTask.helpOverhead;
							
	}
	
	/**
	 * Calculates the team benefit for using the available resources to move towards the goal.
	 * 
	 * @return						The team benefit.
	 */
	private int calcTeamBenefit(int helpResourcePoints) {
		
		decResourcePoints(Agent.calculationCost);

		//Calc PATH rewards with help
		int withHelpRewards = projectRewardPoints(resourcePoints() + helpResourcePoints, path.getEndPoint());
		//Calc PATH rewards with no help
		int noHelpRewards = projectRewardPoints(resourcePoints(), path.getEndPoint());
		
		//int withHelpRemPathLength = path().getNumPoints() - findFinalPos(resourcePoints(),skipCell) - 1 ;	
		//int noHelpRemPathLength = path().getNumPoints() -  findFinalPos(resourcePoints(),pos()) - 1;		
		//return (withHelpRewards-noHelpRewards) * (1+ (importance(withHelpRemPathLength)-importance(noHelpRemPathLength)) *
			//(noHelpRemPathLength-withHelpRemPathLength));
		
		return (withHelpRewards - noHelpRewards);
	}

	private int calcTeamBenefit(){
		return calcTeamBenefit(0);
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
		logInf("Yaay! Agent"+ helperAgents+" has provided resources so I can perform my own action!");
		//setPos(nextCell);
		
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
		
//		setLastAction("Helped by:" + (helperAgent + 1));
//		helperAgent = -1;
//		return true;
	}
	
	/**
	 * Checks whether the agent is in a final state or not.
	 * 
	 * @return						true if is in a final state /
	 * 								false otherwise	
	 */
	private boolean isInFinalState() {
		switch (state) {
			case R_DO_OWN_ACT:
			case R_BLOCKED:
				return true;				
			default:
				return false;
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
	 * Returns a two dimensional array representing the estimated cost
	 * of cells with i, j coordinates
	 *
	 * @author Mojtaba
	 */
	private int[][] estimBoardCosts(int[][] board) {

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
	private int estimCellCost(int i, int j) {
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
	private boolean canSend() {
		return (resourcePoints() >= Team.unicastCost);	
	}
	
	/**
	 * Tells whether the agent has enough resources to send a broadcast
	 * message or not
	 * 
	 * @return 					true if there are enough resources /
	 * 							false if there aren't enough resources	
	 */
	private boolean canBCast() {
		return (resourcePoints() >= Team.broadcastCost);	
	}
	
	/**
	 * Indicates whether the agent has enough resources to do calculations.
	 * 
	 * @return					true if there are enough resources /
	 * 							false if there aren't enough resources
	 */
	private boolean canCalc() {
		return (resourcePoints() >= Agent.calculationCost);
	}

	/**
	 * Checks whether the agent has enough resources in order to replan
	 *
	 * @author Mojtaba
	 */
	private boolean canReplan() {
		return (resourcePoints() >= planCost());
	}

	/**
	 * Broadcast the given String encoded message.
	 * 
	 * @param msg				The String encoded message 
	 */
	private void broadcastMsg(String msg) {
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
	private void sendMsg(int receiver, String msg) {
		decResourcePoints(Team.unicastCost);
		commMedium().send(id(), receiver, msg);
	}
	
	/**
	 * Calculates the average of the given integer array.
	 * 
	 * @return						The average.
	 */
	private double getAverage(int[] array) {
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
	private void setState(ResMAPState newState) {
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
	private void logInf2(String msg) {
		if (dbgInf2)
			System.err.println("[AdvActionMAP2 Agent " + id() + "]: " + msg);
	}
	
	/**
	 * Prints the log message into the output if the  debugging level
	 * is turned on (debuggingInf).
	 * 
	 * @param msg					The desired message to be printed
	 */
	private void logErr(String msg) {
		if (dbgErr)
			System.out.println("[xxxxxxxxxxx][AdvActionMAP2 Agent " + id() + 
							   "]: " + msg);
	}
}
