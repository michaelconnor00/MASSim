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


	public void method_S_INIT(){
		if (dbgInf2)
		{
			for(int i=0;i<Team.teamSize;i++)
				System.out.println("Agent "+i+":" + agentsWellbeing[i]);
		}

		if(canCalc()){
			estimatedCostToGoal = estimatedCost(remainingPath(pos()));
		}
		else{
			setState(ResMAPState.R_BLOCKED);
			return;
		}

		wellbeing = wellbeing();

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

					if (canBCast()){
						String helpReqMsg = prepareHelpReqMsg(
								estimatedCostToGoal, //estimated cost to goal
								teamBenefit, //teambenefit
								cost //next step cost
						);


						logInf("Broadcasting help");
						logInf("Team benefit of help would be " + teamBenefit);

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

	public void method_S_RESPOND_TO_REQ(){
		if(bidding)
		{
			for (Message msg : bidMsgs){
				if(canSend()) {
					logInf("Sending a bid to agent" + msg.getIntValue("requester"));
					sendMsg(msg.getIntValue("requester"), msg.toString());
					this.numOfBids++;
				}
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
	}

	public void method_S_RESPOND_BIDS(){

		logInf("Confirming the help offer(s) of " + confMsgs.size() + " agents.");

		for (int i = 0 ; i < confMsgs.size() ; i++){
			if (canSend())
			{
				sendMsg(confMsgs.get(i).receiver(), confMsgs.get(i).toString());
			}
			else
				setState(ResMAPState.R_BLOCKED);
		}

		setState(ResMAPState.R_DO_OWN_ACT);

	}

	public void method_R_GET_HELP_REQ(){
		ArrayList<Message> helpReqMsgs = new ArrayList<Message>();

		msgStr = commMedium().receive(id());
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

			// Sort DESC by teamBenefit, most to least.
			Collections.sort(helpReqMsgs, tbOrder);
			int stepTeamBenefit = 0;
			int reqNextStepCost = 0;
			double reqWellbeing = 0.0;

			// Bidding for helping achieve next cell
			for (int i = 0; (i < helpReqMsgs.size()) && !bidding; i++) {
				stepTeamBenefit = helpReqMsgs.get(i).getIntValue("teamBenefit");
				reqNextStepCost = helpReqMsgs.get(i).getIntValue("nextStepCost") + TeamTask.helpOverhead ;
				reqWellbeing = helpReqMsgs.get(i).getDoubleValue("wellbeing");
				int requesterAgent = helpReqMsgs.get(i).sender();

				int teamLoss = -1;
				int netTeamBenefit = -1;

				if (canCalc())
				{
					teamLoss = calcTeamLoss(reqNextStepCost);
					netTeamBenefit = stepTeamBenefit - teamLoss;
				}

				logInf("For agent "+ requesterAgent+", team loss= "+teamLoss+
						", NTB= "+netTeamBenefit);

				double wellBeing = wellbeing();

				if (netTeamBenefit > 0 && reqNextStepCost <= myMaxAssistance) {
					bidMsgs.add(prepareBidMsg(requesterAgent, reqNextStepCost, wellBeing));
					bidding = true;
				} else if (netTeamBenefit > 0 && reqWellbeing > wellBeing) {
					//Bid all the assistance you have available
					bidMsgs.add(prepareBidMsg(requesterAgent, reqNextStepCost, wellBeing));
					bidding = true;
				}

			}

		}
		setState(ResMAPState.S_RESPOND_TO_REQ);
	}

	public void method_R_IGNORE_HELP_REQ(){
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
			int buffer = getCellCost(path().getNextPoint(pos()));

			//Check for a self sacrifice agent
			Collections.sort(receivedBidMsgs, wellbeingOrder); //sort DESC wellbeing

			for (Message bid : receivedBidMsgs)
			{
				//Check If agent has sacrificed own resources to self to reach goal
				if (bid.getIntValue("resourceAmount") >= estimatedCostToGoal && canSend()){
					resourcePoints -= Team.unicastCost;
					buffer = 0;
					resourcePoints += bid.getIntValue("resourceAmount");
					//Use all the sacrificed resources.
					confMsgs.add(prepareConfirmMsg(0, bid.sender()));
				}
			}

			//Collections.sort(receivedBidMsgs, tbOrder); //sort TB DESC.

			// Ascending wellbeing (previously sorted by well being in descending order)
			Collections.reverse(receivedBidMsgs);  //TODO : we used wellbeing here, could try to use Team Benefit instead?

			//Combine bids
			for (int i=0; (i < receivedBidMsgs.size()) && (buffer > 0); i++){

				int bidAmount = receivedBidMsgs.get(i).getIntValue("resourceAmount");
				int helperID = receivedBidMsgs.get(i).sender();

				if (bidAmount == buffer && canSend()){  //Only one bid required
					buffer = 0;
					resourcePoints -= Team.unicastCost;
					resourcePoints += bidAmount;
					//Use the whole bid
					confMsgs.add(prepareConfirmMsg(-TeamTask.helpOverhead, helperID));
				}
				else if (bidAmount < buffer && canSend()){ //require multiple bids
					buffer -= bidAmount;
					resourcePoints -= Team.unicastCost;
					resourcePoints += bidAmount;
					//Use the whole bid
					confMsgs.add(prepareConfirmMsg(-TeamTask.helpOverhead, helperID));
				}
				else if (bidAmount > buffer && canSend()){
					resourcePoints -= Team.unicastCost;
					resourcePoints += buffer;
					//Use part of the bid, return un-used amount
					confMsgs.add(prepareConfirmMsg((bidAmount-buffer-TeamTask.helpOverhead), helperID));
					buffer = 0; //Or break;
				}
				else{
					setState(ResMAPState.S_BLOCKED);
					logInf("Now I'm blocked!");
				}
			}

			setState(ResMAPState.S_RESPOND_BIDS);
		}
	}

	public void method_R_GET_BID_CONF(){
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

}
