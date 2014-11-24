package massim.agents.resourcemap;

import java.util.ArrayList;
import java.util.Collections;

import massim.*;

/**
 * Resource MAP Agent
 *
 * @author Devin Calado, Michael Conner
 * @version 1.0 - November 2014
 *
 */
public class ResourceMAPAgent extends ResourceMAP_BaseAgent {

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
	}


	public void method_S_INIT(){
		if (dbgInf2)
		{
			for(int i=0;i<Team.teamSize;i++)
				System.out.println("Agent "+i+":" + agentsWellbeing[i]);
		}


		if (reachedGoal())
		{
			if (canCalcAndBCast(1)) {
				logInf2("Broadcasting my wellbeing to the team");
				broadcastMsg(prepareWellbeingUpMsg(wellbeing()));
			}
			setState(ResMAPState.R_GET_HELP_REQ);
		}
		else
		{
			RowCol nextCell = path().getNextPoint(pos());
			int cost = getCellCost(nextCell);

			boolean needHelp = (cost > resourcePoints());// || (cost > requestThreshold);

			if (needHelp)
			{
				logInf2("Need help!");

				if (canCalcAndBCast(2)) {

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
					setState(ResMAPState.R_IGNORE_HELP_REQ);

				} else
					setState(ResMAPState.R_BLOCKED);
			}
			else
			{
				setState(ResMAPState.R_GET_HELP_REQ);
			}
		}
	}


	public void method_R_GET_HELP_REQ(){
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

			double estimatedCostToGoal = Double.MAX_VALUE;

			if(canCalc()){
				estimatedCostToGoal = estimatedCost(remainingPath(pos()));
			}
			else{
				setState(ResMAPState.S_RESPOND_TO_REQ);
			}

			// Check to see if any of the requester can get to the goal for less than self.
			for (int i = 0; (i < helpReqMsgs.size()) && canSacrifice && !bidding; i++) {
				Double reqECostToGoal = helpReqMsgs.get(i).getDoubleValue("eCostToGoal");
				int requesterAgent = helpReqMsgs.get(i).sender();

				//check that the myCostToGoal:requesterCostToGoal ratio is greater than threshold
				// and I have enough resources to sacrifice.
				if (((estimatedCostToGoal / reqECostToGoal) > costToGoalHelpThreshold) &&
						(resourcePoints - Team.unicastCost - TeamTask.helpOverhead) >= reqECostToGoal) {
					bidMsgs.add(prepareBidMsg(requesterAgent, reqECostToGoal.intValue(), wellbeing()));
					helpReqMsgs.remove(helpReqMsgs.get(i));
					bidding = true;
					break;
				}
			}

			int myMaxAssistance = resourcePoints - (int) estimatedCostToGoal;

			// Sort by average step cost to goal, in ascending order.
			Collections.sort(helpReqMsgs, averageStepCostOrder);

			// Bidding for helping achieve next cell
			for (int i = 0; (i < helpReqMsgs.size()) && !bidding; i++) {
				if(canCalc()) {
					int reqStepsToGoal = helpReqMsgs.get(i).getIntValue("stepsToGoal");
					double reqAvgStepCostToGoal = helpReqMsgs.get(i).getDoubleValue("averageStepCost");
					int reqNextStepCost = helpReqMsgs.get(i).getIntValue("nextStepCost") + TeamTask.helpOverhead;
					int requesterAgent = helpReqMsgs.get(i).sender();

					// Helper has enough resources to reach their own goal
					if ((estimatedCostToGoal <= resourcePoints - TeamTask.helpOverhead) &&
							myMaxAssistance > reqNextStepCost) {
						bidMsgs.add(prepareBidMsg(requesterAgent, reqNextStepCost, wellbeing()));
						bidding = true;
					}
					// Helper does not have enough resource points to get to their goal
					// My average step costs from current position to the goal is greater than the help requester's.
					else if ((estimatedCostToGoal / remainingPath(pos()).getNumPoints()) > reqAvgStepCostToGoal) {
						bidMsgs.add(prepareBidMsg(requesterAgent, reqNextStepCost, wellbeing()));
						bidding = true;
					}
				}

			}

		}
		setState(ResMAPState.S_RESPOND_TO_REQ);
	}
	public void	method_R_IGNORE_HELP_REQ(){
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
	}
	public void method_R_GET_BIDS(){
		ArrayList<Message> receivedBidMsgs = new ArrayList<Message>();

		msgStr = commMedium().receive(id());
		while (!msgStr.equals(""))
		{
			logInf("Received a message: " + msgStr);
			Message msg = new Message(msgStr);
			if (msg.isOfType(MAP_BID_MSG))
				receivedBidMsgs.add(msg);
			msgStr = commMedium().receive(id());
		}

		confMsgs = new ArrayList<Message>();

		if (receivedBidMsgs.size() == 0) {
			logInf("Nobody has offered me assistance!");
			this.numOfUnSucHelpReq++;

			int cost = getCellCost(path().getNextPoint(pos()));
			if (cost <= resourcePoints())
				setState(ResMAPState.S_DECIDE_OWN_ACT);
			else {
				setState(ResMAPState.S_BLOCKED);
				logInf("Now I'm blocked!");
			}
		}
		else
		{

			logInf("Received " + receivedBidMsgs.size()+" bids.");

			//Buffer of Cost for next step, for use when using multiple bids
			int buffer = getCellCost(path().getNextPoint(pos())) + Team.unicastCost;

			Collections.sort(receivedBidMsgs, wellbeingOrder);

			double estimatedCostToGoal = Double.MAX_VALUE;

			if(canCalc()){
				estimatedCostToGoal = estimatedCost(remainingPath(pos()));
			}
			else{
				setState(ResMAPState.S_BLOCKED);
			}

			for (Message bid : receivedBidMsgs)
			{
				//Check If agent has sacrificed own resources to self to reach goal
				if (bid.getIntValue("resourceAmount") == estimatedCostToGoal){
					buffer = 0;
					resourcePoints += bid.getIntValue("resourceAmount");
					//Use all the sacrificed resources.
					confMsgs.add(prepareConfirmMsg(0, bid.sender()));
				}
			}

			for (int i=0; (i < receivedBidMsgs.size()) && (buffer > 0); i++){

				int bidAmount = receivedBidMsgs.get(i).getIntValue("resourceAmount");
				int helperID = receivedBidMsgs.get(i).sender();

				if (bidAmount == buffer){
					buffer = 0;
					resourcePoints += bidAmount;
					//Use the whole bid
					confMsgs.add(prepareConfirmMsg(0, helperID));
				}
				else if (bidAmount < buffer){
					buffer -= bidAmount;
					resourcePoints += bidAmount;
					//Use the whole bid
					confMsgs.add(prepareConfirmMsg(0, helperID));
				}
				else if (bidAmount > buffer) {
					resourcePoints += buffer;
					//Use part of the bid, return un-used amount
					confMsgs.add(prepareConfirmMsg((bidAmount-buffer), helperID));
					buffer = 0; //Or break;
				}
			}

			setState(ResMAPState.S_RESPOND_BIDS);
		}
	}
	public void	method_R_GET_BID_CONF(){
		msgStr = commMedium().receive(id());

		if (!msgStr.equals("") && (new Message(msgStr)).isOfType(MAP_HELP_CONF))
		{
			int returnedResources =(new Message(msgStr)).getIntValue("returnedResources");  // resources to be re-added to an agents available resources
			resourcePoints += returnedResources; // This may be a negative number, since the helper is 'charged' the help overhead upon confirmation.
			logInf("Received confirmation, my resource offering was used.");  // Output information about full or partial resource use
			this.numOfSucOffers++;

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
	}
	public void method_R_DO_OWN_ACT(){
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
	}


	/**
	 * Calculated the estimated cost for the agent to move through path p.
	 *
	 * @param p						The agent's path
	 * @return						The estimated cost
	 */
	protected double estimatedCost(Path p) {

		decResourcePoints(Agent.calculationCost);

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
	 * Prepares a help request message and returns its String encoding.
	 *
	 *
	 * @return						The message encoded in String
	 */
	protected String prepareHelpReqMsg(int stepsToGoal, double estimatedCostToGoal, double averageCellCost, int nextStepCost) {

		Message helpReq = new Message(id(),-1,MAP_HELP_REQ_MSG);
		helpReq.putTuple("eCostToGoal", estimatedCostToGoal);
		helpReq.putTuple("stepsToGoal", stepsToGoal);
		helpReq.putTuple("averageStepCost", averageCellCost);
		helpReq.putTuple("nextStepCost", nextStepCost);
		helpReq.putTuple("wellbeing", wellbeing());

		return helpReq.toString();
	}
}
