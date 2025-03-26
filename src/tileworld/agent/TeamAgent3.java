package tileworld.agent;

import tileworld.environment.TWDirection;
import tileworld.environment.TWEnvironment;
import tileworld.environment.TWHole;
import tileworld.environment.TWTile;
import tileworld.exceptions.CellBlockedException;
import tileworld.planners.AstarPathGenerator;
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
public class TeamAgent3 extends TWAgent {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private String name = "agent1";

    private final int mapSizeX = this.getEnvironment().getxDimension();
    private final int mapSizeY = this.getEnvironment().getyDimension();
    private AstarPathGenerator pathGenerator = new AstarPathGenerator(this.getEnvironment(), this,
	    this.mapSizeX + this.mapSizeY);

    private AgentMemory memory = new AgentMemory(this.getEnvironment());

    private String tempMessage = "";
    private String tempAllMessage = "";

    // private List<AgentTile> candidateTiles = new ArrayList<AgentTile>();
    private AgentTile closestTile = new AgentTile(-1, -1);
    private AgentTile closestHole = new AgentTile(-1, -1);

    public TeamAgent3(String name, int xpos, int ypos, TWEnvironment env, double fuelLevel) {
	super(xpos, ypos, env, fuelLevel);
	this.name = name;
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

    private void checkMessages() {
	if (this.tileInFOV(closestTile)) {
	    System.out.println("Reset, since tile should be in FOV");
	    this.closestTile = new AgentTile(-1, -1);
	}
	if (this.tileInFOV(closestHole)) {
	    System.out.println("Reset, since tile should be in FOV");
	    this.closestHole = new AgentTile(-1, -1);
	}
	for (Message message : this.getEnvironment().getMessages()) {
	    System.out.println(message.getMessage());
	    String[] splitMessage = message.getMessage().split(";");
	    String[] words;
	    String messageType;
	    for (String msg : splitMessage) {
		words = msg.split(" ");
		messageType = words[0];
		switch (messageType) {
		case Constants.FOUND_FUEL_STATION_MESSAGE:
		    int fuelX = Integer.parseInt(words[1]);
		    int fuelY = Integer.parseInt(words[2]);
		    this.memory.setFuelTile(fuelX, fuelY);
		    break;
		// found object(s)
		case Constants.UPDATE_MEMORY_MAP_MESSAGE:
		    int objectX = Integer.parseInt(words[1]);
		    int objectY = Integer.parseInt(words[2]);
		    String objectStatus = words[3];
		    if (objectStatus == "null")
			break;
		    Object object = this.getMemory().getMemoryGrid().get(objectX, objectY);
		    if (object instanceof TWTile) {
			// System.out.println("Adding tile to frontier");
			// this.candidateTiles.add(new AgentTile(objectX, objectY));
			if (this.distanceToTile(objectX, objectY) < this.distanceToTile(this.closestTile.x,
				closestTile.y) || !this.isValidEntity(closestTile)) {
			    this.closestTile.x = objectX;
			    this.closestTile.y = objectY;
			}
			break;
		    } else if (object instanceof TWHole) {
			// System.out.println("Adding hole to frontier");
			if (this.distanceToTile(objectX, objectY) < this.distanceToTile(this.closestHole.x,
				closestHole.y) || !this.isValidEntity(closestTile)) {
			    this.closestHole.x = objectX;
			    this.closestHole.y = objectY;
			}
			break;
		    }
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

    private boolean fuelLevelCritical() {
	if (!this.foundFuelStation())
	    return false;
	return (this.fuelLevel < this.distanceToFuelStation() + 5);
    }

    private void goToFuelStation() {
	this.goToTile(this.memory.getFuelTile());
    }

    private void explore() {
	this.goToTile(this.memory.getExplorationTile());
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

    private void updateNextThought() {
	if (this.fuelLevelCritical()) {
	    System.out.println("Setting in fuel level");
	    this.goToFuelStation();
	    return;
	}

	if (this.carriedTiles.size() > 1 && this.isValidHole(closestHole)) {
	    System.out.println("Going to hole...");
	    boolean valid = this.isValidHole(closestHole);
	    System.out.println(valid);
	    this.goToTile(closestHole);
	    // this.exploring = false;
	    return;
	}

	if (this.carriedTiles.size() < 2 && this.isValidTile(closestTile)) {
	    System.out.println("Going to tile...");
	    System.out.println(closestTile.x);
	    System.out.println(closestTile.y);
	    this.goToTile(closestTile);
	    // this.exploring = false;
	    return;
	}
	
	if (!this.memory.currentPathValid()) {
	    this.explore();
	    return;
	}
	
    }

    protected TWThought think() {
	
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