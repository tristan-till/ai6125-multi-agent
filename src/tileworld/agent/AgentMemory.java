package tileworld.agent;

import java.util.ArrayList;
import java.util.List;

import tileworld.environment.TWEnvironment;
import tileworld.planners.AstarPathGenerator;
import tileworld.planners.TWPath;
import tileworld.planners.TWPathStep;
import tileworld.utils.Constants;

public class AgentMemory {
    private AgentTile fuelTile = new AgentTile(-1, -1);

    private TWEnvironment environment;

    private TWPath currentPath = null;
    private List<AgentTile> candidateTiles = new ArrayList<AgentTile>();
    private int FOV = 3;

    private String myState = Constants.FUEL_STATION_FINDING_STATE;

    public AgentMemory(TWEnvironment environment) {
	this.environment = environment;
	this.initializeAgentTiles();
	return;
    }

    private void initializeAgentTiles() {
	int mapSizeX = this.environment.getxDimension();
	int mapSizeY = this.environment.getyDimension();
	this.candidateTiles.add(new AgentTile(FOV, mapSizeX / 2));
	this.candidateTiles.add(new AgentTile(FOV, mapSizeX - FOV));
	this.candidateTiles.add(new AgentTile(mapSizeY / 2, mapSizeX - FOV));
	this.candidateTiles.add(new AgentTile(mapSizeY / 2, mapSizeX / 2));
	this.candidateTiles.add(new AgentTile(mapSizeY / 2, FOV));
	this.candidateTiles.add(new AgentTile(mapSizeY - FOV, FOV));
	this.candidateTiles.add(new AgentTile(mapSizeY - FOV, mapSizeX / 2));
	this.candidateTiles.add(new AgentTile(mapSizeY - FOV, mapSizeX - FOV));
	this.candidateTiles.add(new AgentTile(FOV, FOV));
    }

    public AgentTile getExplorationTile() {
	AgentTile randomTile = this.candidateTiles.getFirst();
	
	this.candidateTiles.removeFirst();
	this.candidateTiles.addLast(randomTile);
	return randomTile;
    }

    public String getState() {
	return myState;
    }

    public AgentTile getFuelTile() {
	return fuelTile;
    }

    public void setFuelTile(int x, int y) {
	this.fuelTile.x = x;
	this.fuelTile.y = y;
    }

    public TWPath getCurrentPath() {
	return currentPath;
    }

    public void setCurrentPath(TWPath newCurrentPath) {
	System.out.println("Setting new path!");
	
	this.currentPath = newCurrentPath;
    }

    public boolean currentPathValid() {
	if (this.currentPath == null)
	    return false;
	return this.currentPath.hasNext();
    }

    public TWThought getNextThought() {
	if (!this.currentPathValid()) return null;
	TWPathStep nextStep = this.currentPath.popNext();
	TWThought nextThought = new TWThought(TWAction.MOVE, nextStep.getDirection());
	return nextThought;
    }
}
