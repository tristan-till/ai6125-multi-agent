package tileworld.agent;

import sim.display.Console;
import tileworld.TWGUI;
import tileworld.environment.TWDirection;
import tileworld.environment.TWEntity;
import tileworld.environment.TWEnvironment;
import tileworld.environment.TWHole;
import tileworld.environment.TWTile;
import tileworld.environment.TWFuelStation;
import tileworld.exceptions.CellBlockedException;
import tileworld.planners.AstarPathGenerator;
import tileworld.planners.TWPath;
import tileworld.planners.TWPathStep;
import tileworld.utils.Constants;
import tileworld.environment.TWObstacle;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

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
    private String name = "agent1";
    private String tempMessage = "";
    private String tempAllMessage = "";
    private int fuelX = -1;
    private int fuelY = -1;
    private final int mapsizeX = this.getEnvironment().getxDimension();
    private final int mapsizeY = this.getEnvironment().getyDimension();
    private int[][] seenMap = new int[mapsizeX][mapsizeY];
    private AgentState myState = new AgentState("initial", 0, 0);
    
    private int otherCarriedTiles = 0;
    private int[] otherASF = new int[] { -5, -5, -5, -5 };
    
    private TWPath curPath = null;
    private int curPathStep = 0;
    private AstarPathGenerator pathGenerator = new AstarPathGenerator(this.getEnvironment(), this, mapsizeX + mapsizeY);
    private ArrayList<int[]> searchTileChain = new ArrayList<int[]>();
    private int rethinking = 0;
    private int pickVanished = 0;
    private int[] otherAgentPosition = { -1, -1 };
    public ArrayList<int[]> pickTileChain = new ArrayList<int[]>();
    private ArrayList<int[]> bPlanPickArea = new ArrayList<int[]>();
    private int[] bPlanPickTarget = new int[] { -1, -1 };
    private ArrayList<int[]> patrolSearchPoints = new ArrayList<int[]>();
    private int[] patrolFuelPoint = new int[] { -1, -1 };

    public TeamAgent3(String name, int xpos, int ypos, TWEnvironment env, double fuelLevel) {
	super(xpos, ypos, env, fuelLevel);
	this.name = name;
    }


    public void clearTempMessage() {
	this.tempMessage = "";
	this.tempAllMessage = "";
    }

    @Override
    public void communicate() {
	Message message = new Message(this.name, Constants.PRIVATE_CHANNEL, tempMessage);
	this.getEnvironment().receiveMessage(message);
	Message message2 = new Message(this.name, Constants.PUBLIC_CHANNEL, tempAllMessage);
	this.getEnvironment().receiveMessage(message2);
	this.clearTempMessage();
    }

    private double getExpectedLifeTime() {
	return this.getMemory().estimateLifeTime;
    }

    private boolean agentOnTile() {
	return this.getMemory().getMemoryGrid().get(this.getX(), this.getY()) instanceof TWTile;
    }

    private boolean agentOnHole() {
	return this.getMemory().getMemoryGrid().get(this.getX(), this.getY()) instanceof TWHole;
    }

    private boolean agentOnFuelStation() {
	return this.getX() == fuelX && this.getY() == fuelY;
    }


    protected TWThought think() {
	if (this.carriedTiles.size() < 3 && this.agentOnTile()) {
	    return new TWThought(TWAction.PICKUP);
	}
	if (this.carriedTiles.size() > 0 && this.agentOnHole()) {
	    return new TWThought(TWAction.PUTDOWN);
	}
	if (this.getFuelLevel() <= 490 && this.agentOnFuelStation()) {
	    return new TWThought(TWAction.REFUEL);
	}

	return new TWThought(TWAction.MOVE, getRandomDirection());
    }

    @Override
    protected void act(TWThought thought) {
	// System.out.println(this.getName() + " act");
	Object tempObject = this.getMemory().getMemoryGrid().get(this.getX(), this.getY());
	switch (thought.getAction()) {

	case PICKUP:
	    pickUpTile((TWTile) tempObject);
	    this.getMemory().removeObject(this.getX(), this.getY()); // remove memory
	    this.addTempMessage("UpdateMemoryMap " + this.getX() + " " + this.getY() + " " + "null");

	    // this.rethinking=1;
	    if (!AgentParameter.isPickNeedOneStep) {
		act(this.think());
	    }
	    return;

	case PUTDOWN:
	    putTileInHole((TWHole) tempObject);
	    this.getMemory().removeObject(this.getX(), this.getY());
	    this.addTempMessage("UpdateMemoryMap " + this.getX() + " " + this.getY() + " " + "null");
	    
	    if (!AgentParameter.isPickNeedOneStep) {
		act(this.think());
	    }
	    return;

	case REFUEL:
	    this.refuel();
	    this.myState.state1 = Constants.IDLE_STATE;
	    this.curPath = null;
	    this.curPathStep = 0;

	    // this.rethinking=1;
	    if (!AgentParameter.isPickNeedOneStep) {
		act(this.think());
	    }
	    return;

	}

	try {
	    this.move(thought.getDirection());
	    rethinking = 0;
	    curPathStep++;
	    this.getMemory().estimateLifeTime += AgentParameter.lifetimeLearningRate
		    * AgentParameter.lifetimeIncreaseLearningRatio;
	    if (AgentParameter.lifetimeLearningRate > AgentParameter.lifetimeFinalLearningRate)
		AgentParameter.lifetimeLearningRate *= 0.999;
	    this.addTempMessage("MyPosition " + this.getX() + " " + this.getY());
	    this.addTempMessage("MyCarriedTiles " + this.carriedTiles.size());
	} catch (CellBlockedException ex) {
	    this.rethinking = 1;
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

    @Override
    public String getName() {
	return name;
    }
}