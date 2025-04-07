package tileworld.agent;

import java.util.Map;

import tileworld.environment.TWDirection;
import tileworld.environment.TWEnvironment;
import tileworld.environment.TWHole;
import tileworld.environment.TWTile;
import tileworld.exceptions.CellBlockedException;
import tileworld.planners.AstarWAgentCost;
import tileworld.planners.TWPath;
import tileworld.utils.Constants;

/**
 * TWContextBuilder
 *
 * @author michaellees Created: Feb 6, 2011
 *
 *         Copyright michaellees Expression year is undefined on line 16, column
 *         24 in Templates/Classes/Class.java.
 *
 *
 *         Description:
 *
 */
public class FOVAgent extends TWAgent {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private String name = "agent1";

    private final int mapSizeX = this.getEnvironment().getxDimension();
    private final int mapSizeY = this.getEnvironment().getyDimension();
    private AstarWAgentCost pathGenerator = new AstarWAgentCost(this.getEnvironment(), this,
	    this.mapSizeX + this.mapSizeY);

    private AgentMemory memory;

    private String tempMessage = "";
    private String tempAllMessage = "";

    private AgentTile closestTile = new AgentTile(-1, -1);
    private AgentTile closestHole = new AgentTile(-1, -1);

    private RefuelScheduler refuelScheduler;
    private int earlyRequestRefuelTimer = 0;
    private boolean refuelBeforeSwitch = false;
    private boolean refuelImmediately = false;

    public FOVAgent(String name, int xpos, int ypos, TWEnvironment env, double fuelLevel) {
	super(xpos, ypos, env, fuelLevel);
	this.name = name;
	this.memory = new AgentMemory(this.getEnvironment(), this.name, this.getCurrentAgentTile());
	this.refuelScheduler = RefuelScheduler.getInstance();
    }

    private boolean foundFuelStation() {
	return this.memory.getFuelTile().x != -1 && this.memory.getFuelTile().y != -1;
    }

    private boolean agentOnTile() {
	return this.getMemory().getMemoryGrid().get(this.getX(), this.getY()) instanceof TWTile;
    }

    private boolean agentOnHole() {
	return this.getMemory().getMemoryGrid().get(this.getX(), this.getY()) instanceof TWHole;
    }

    private boolean agentOnFuelStation() {
	AgentTile fuelTile = memory.getFuelTile();
	return this.getX() == fuelTile.x && this.getY() == fuelTile.y;
    }

    private void goToXY(int x, int y) {
	TWPath path = this.findPath(x, y);
	this.memory.setCurrentPath(path);
    }

    private void goToTile(AgentTile tile) {
	this.goToXY(tile.x, tile.y);
    }

    private void exploreTile(AgentTile tile) {
	int[] closestAgent = this.getMemory().getClosestAgent();
	this.getMemory().resetClosestAgent();
	if (closestAgent == null) {
	    this.goToTile(this.memory.getExplorationTile());
	    return;
	}
	TWPath path = pathGenerator.explorePath(this.getX(), this.getY(), tile.x, tile.y, closestAgent[0],
		closestAgent[1]);
	
	this.memory.setCurrentPath(path);
    }

    private TWPath findPath(int x, int y) {
	return pathGenerator.findPath(this.getX(), this.getY(), x, y);
    }

    private boolean tileInFOV(AgentTile tile) {
	if (!this.isValidEntity(tile)) {
	    return false;
	}
	if (tile.x > this.getX() + 2 || tile.x < this.getX() + 2) {
	    return false;
	}

	if (tile.y > this.getY() + 2 || tile.y < this.getY() + 2) {
	    return false;
	}

	return true;
    }

    private void notifyDistanceToFuelStation() {
	int distanceToFuelStation = this.distanceToFuelStation();
	this.refuelScheduler.addRefuelAgent(distanceToFuelStation, name);
	Map<String, Integer> refuelAgents = this.refuelScheduler.getOrderedRefuelAgents();
	if (refuelAgents == null) {
	    return;
	}

	for (Map.Entry<String, Integer> entry : refuelAgents.entrySet()) {
	    String agentName = entry.getKey();
	    Integer refuelInterval = entry.getValue();
	    this.addTempAllMessage(Constants.REFUEL_SCHEDULE + " " + agentName + " " + refuelInterval);
	}

    }

    private void reactToFoundFuelStationMessage(String[] words) {
	if (this.foundFuelStation()) {
	    return;
	}
	int fuelX = Integer.parseInt(words[1]);
	int fuelY = Integer.parseInt(words[2]);
	this.memory.setFuelTile(fuelX, fuelY);
	this.notifyDistanceToFuelStation();
    }

    private void reactToUpdateMemoryMapMessage(String[] words) {
	int objectX = Integer.parseInt(words[1]);
	int objectY = Integer.parseInt(words[2]);
	String objectStatus = words[3];
	if (objectStatus == "null")
	    return;
	Object object = this.getMemory().getMemoryGrid().get(objectX, objectY);
	if (object instanceof TWTile) {
	    if (this.distanceToTile(objectX, objectY) < this.distanceToTile(this.closestTile.x, closestTile.y)
		    || !this.isValidEntity(closestTile)) {
		this.closestTile.x = objectX;
		this.closestTile.y = objectY;
	    }
	    return;
	} else if (object instanceof TWHole) {
	    if (this.distanceToTile(objectX, objectY) < this.distanceToTile(this.closestHole.x, closestHole.y)
		    || !this.isValidEntity(closestTile)) {
		this.closestHole.x = objectX;
		this.closestHole.y = objectY;
	    }
	    return;
	}
    }

    private void reactToRequestZoneSwitchMessage(String[] words) {
	String agentNameSwitchingZones = words[1];
	String zoneAgentIsSwitchingTo = words[2];
	String freeZone = words[3];

	if (agentNameSwitchingZones.equals(this.name)) {
	    return;
	}

	if (!zoneAgentIsSwitchingTo.equals(this.memory.getZone().getZoneName())) {
	    return;
	}
	this.memory.setZoneByName(freeZone, this.getCurrentAgentTile());

	if (this.fuelLevel < 100) {
	    this.refuelBeforeSwitch = true;
	}
    }

    private AgentTile getCurrentAgentTile() {
	return new AgentTile(this.getX(), this.getY());
    }

    private void reactToRefuelScheduleMessage(String[] words) {
	String targetAgentName = words[1];
	int refuelSchedule = Integer.parseInt(words[2]);

	if (!targetAgentName.equals(this.name)) {
	    return;
	}

	this.earlyRequestRefuelTimer = refuelSchedule;
    }
    
    private void reactToUpdateAgentZoneMessage(String[] words) {
	String agentName = words[1];
	String zoneName = words[2];
	
	if (agentName.equals(this.name)) {
	    return;
	}
	
	if (!zoneName.equals(this.memory.getZone().getZoneName())) {
	    return;
	}
	this.memory.randomlySwitchZone(this.getCurrentAgentTile());
    }

    private void checkMessages() {
	if (this.tileInFOV(closestTile)) {
	    this.closestTile = new AgentTile(-1, -1);
	}
	if (this.tileInFOV(closestHole)) {
	    this.closestHole = new AgentTile(-1, -1);
	}
	for (Message message : this.getEnvironment().getMessages()) {
	    String[] splitMessage = message.getMessage().split(";");
	    String[] words;
	    String messageType;
	    for (String msg : splitMessage) {
		words = msg.split(" ");
		messageType = words[0];
		switch (messageType) {
		
		case Constants.FOUND_FUEL_STATION_MESSAGE:
		    this.reactToFoundFuelStationMessage(words);
		    break;
		case Constants.UPDATE_MEMORY_MAP_MESSAGE:
		    this.reactToUpdateMemoryMapMessage(words);
		    break;

		case Constants.REFUEL_SCHEDULE:
		    this.reactToRefuelScheduleMessage(words);
		    break;

		case Constants.REQUEST_ZONE_SWITCH:
		    this.reactToRequestZoneSwitchMessage(words);
		    break;
		    
		case Constants.UPDATE_AGENT_ZONE:
		    this.reactToUpdateAgentZoneMessage(words);
		    break;
		}
	    }
	}

    }

    private TWThought getNonMoveThought() {
	if (this.carriedTiles.size() < 3 && this.agentOnTile()) {
	    return new TWThought(TWAction.PICKUP);
	}
	if (this.carriedTiles.size() > 0 && this.agentOnHole()) {
	    return new TWThought(TWAction.PUTDOWN);
	}
	if (this.getFuelLevel() <= 490 && this.agentOnFuelStation()) {
	    return new TWThought(TWAction.REFUEL);
	}
	return null;
    }

    private int distanceToTile(int x, int y) {
	return Math.abs(this.getX() - x) + Math.abs(this.getY() - y);
    }

    private int distanceToFuelStation() {
	return distanceToTile(this.memory.getFuelTile().x, this.memory.getFuelTile().y);
    }

    private void requestZoneSwitch() {
	this.resetEarlyRequestRefuelTimer();
	if (this.memory.getFuelStationZone() == null) {
	    return;
	}
	String zoneNameToSwitchTo = this.memory.getFuelStationZone().getZoneName();
	String myZone = this.memory.getZone().getZoneName();
	if (zoneNameToSwitchTo.equals(myZone)) {
	    return;
	}
	// this.memory.setFreeZone(myZone);
	this.addTempAllMessage(
		Constants.REQUEST_ZONE_SWITCH + " " + this.name + " " + zoneNameToSwitchTo + " " + myZone);
	this.memory.setZoneByName(zoneNameToSwitchTo, this.getCurrentAgentTile());
	this.refuelImmediately = true;
    }

    private void resetEarlyRequestRefuelTimer() {
	this.earlyRequestRefuelTimer = 75;
    }

    private boolean fuelLevelCritical() {
	if (!this.foundFuelStation())
	    return false;

	return (this.fuelLevel < this.distanceToFuelStation() + 5);
    }

    private void goToFuelStation() {
	this.goToTile(this.memory.getFuelTile());
    }

    private void explore() {
	this.exploreTile(this.memory.getExplorationTile());
    }

    private boolean isValidEntity(AgentTile tile) {
	return (tile.x != -1 && tile.y != -1);
    }

    private boolean isValidTile(AgentTile tile) {
	if (!this.isValidEntity(tile)) {
	    return false;
	}
	Object object = this.getMemory().getMemoryGrid().get(tile.x, tile.y);
	if (!(object instanceof TWTile)) {
	    return false;
	}
	return true;
    }

    private boolean isValidHole(AgentTile tile) {
	if (!this.isValidEntity(tile)) {
	    return false;
	}
	Object object = this.getMemory().getMemoryGrid().get(tile.x, tile.y);
	if (!(object instanceof TWHole)) {
	    return false;
	}
	return true;
    }

    private void rememberExplorationTarget() {
	if (this.memory.isDistracted) {
	    this.memory.swapLastExplorationTile();
	}
    }

    private void updateNextThought() {
	if (this.fuelLevelCritical()) {
	    this.goToFuelStation();
	    return;
	}

	// System.out.println(this.name + " requesting switch in "
	//	+ (this.fuelLevel - this.earlyRequestRefuelTimer - this.distanceToFuelStation()));
	if (this.fuelLevel < this.earlyRequestRefuelTimer + this.distanceToFuelStation()) {
	    this.requestZoneSwitch();
	}

	if (this.carriedTiles.size() > 1 && this.isValidHole(closestHole)) {
	    this.rememberExplorationTarget();
	    this.goToTile(closestHole);
	    this.memory.isDistracted = true;
	    // this.exploring = false;
	    return;
	}

	if (this.carriedTiles.size() < 2 && this.isValidTile(closestTile)) {
	    this.rememberExplorationTarget();
	    this.goToTile(closestTile);
	    this.memory.isDistracted = true;
	    return;
	}

	if (this.refuelBeforeSwitch || this.refuelImmediately) {
	    this.goToFuelStation();
	    return;
	}

	if (!this.memory.currentPathValid()) {
	    this.memory.isDistracted = false;
	    this.explore();
	    return;
	}

    }
    
    private void randomlyCommunicateZone() {
	if (Math.random() > 0.1) {
	    return;
	}
	this.addTempAllMessage(Constants.UPDATE_AGENT_ZONE + " " + this.name + " " + this.memory.getZone().getZoneName());
    }

    protected TWThought think() {
	System.out.println(this.name + " currently occupies " + this.memory.getZone().getZoneName());
	this.checkMessages();

	if (!this.foundFuelStation()) {
	    if (!this.memory.currentPathValid()) {
		this.explore();
	    }
	    return this.memory.getNextThought();
	}

	TWThought nonMoveThought = this.getNonMoveThought();
	if (nonMoveThought != null) {
	    return nonMoveThought;
	}
	
	this.randomlyCommunicateZone();
	this.updateNextThought();

	TWThought nextThought = this.memory.getNextThought();
	if (nextThought != null) {
	    return nextThought;
	}

	return new TWThought(TWAction.MOVE, getRandomDirection());
    }

    @Override
    protected void act(TWThought thought) {
	Object tempObject = this.getMemory().getMemoryGrid().get(this.getX(), this.getY());
	switch (thought.getAction()) {

	case PICKUP:
	    pickUpTile((TWTile) tempObject);
	    this.getMemory().removeObject(this.getX(), this.getY()); // remove memory
	    this.addTempMessage("UpdateMemoryMap " + this.getX() + " " + this.getY() + " " + "null");
	    this.closestTile = new AgentTile(-1, -1);
	    if (!AgentParameter.isPickNeedOneStep) {
		act(this.think());
	    }
	    return;

	case PUTDOWN:
	    putTileInHole((TWHole) tempObject);
	    this.closestHole = new AgentTile(-1, -1);
	    this.getMemory().removeObject(this.getX(), this.getY());
	    this.addTempMessage("UpdateMemoryMap " + this.getX() + " " + this.getY() + " " + "null");

	    if (!AgentParameter.isPickNeedOneStep) {
		act(this.think());
	    }
	    return;

	case REFUEL:
	    this.refuel();
	    this.memory.setCurrentPath(null);
	    this.resetEarlyRequestRefuelTimer();
	    this.refuelBeforeSwitch = false;
	    this.refuelImmediately = false;

	    if (!AgentParameter.isPickNeedOneStep) {
		act(this.think());
	    }
	    return;
	default:
	    break;

	}

	try {
	    this.move(thought.getDirection());
	    this.getMemory().estimateLifeTime += AgentParameter.lifetimeLearningRate
		    * AgentParameter.lifetimeIncreaseLearningRatio;
	    if (AgentParameter.lifetimeLearningRate > AgentParameter.lifetimeFinalLearningRate)
		AgentParameter.lifetimeLearningRate *= 0.999;
	} catch (CellBlockedException ex) {
	    act(this.think());
	}
    }

    private TWDirection getRandomDirection() {

	TWDirection randomDir = TWDirection.values()[this.getEnvironment().random.nextInt(4)];

	if (this.getX() >= this.getEnvironment().getxDimension()) {
	    randomDir = TWDirection.W;
	} else if (this.getX() <= 1) {
	    randomDir = TWDirection.E;
	} else if (this.getY() <= 1) {
	    randomDir = TWDirection.S;
	} else if (this.getY() >= this.getEnvironment().getxDimension()) {
	    randomDir = TWDirection.N;
	}

	return randomDir;

    }

    public void clearTempMessage() {
	this.tempMessage = "";
	this.tempAllMessage = "";
    }

    @Override
    public void addTempMessage(String mes) {
	this.tempMessage = this.tempMessage + ";" + mes;
    }

    @Override
    public void addTempAllMessage(String mes) {
	this.tempAllMessage = this.tempAllMessage + ";" + mes;
    }

    @Override
    public void communicate() {
	Message message = new Message(this.name, "private", tempMessage);
	this.getEnvironment().receiveMessage(message); // this will send the message to the broadcast channel of the
	// environment
	Message message2 = new Message(this.name, "all", tempAllMessage);
	this.getEnvironment().receiveMessage(message2);
	this.clearTempMessage();
    }

    @Override
    public String getName() {
	return name;
    }
}