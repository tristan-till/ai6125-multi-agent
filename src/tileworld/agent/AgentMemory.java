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
    private String agentName;
    private TWEnvironment environment;

    private TWPath currentPath = null;
    private List<AgentTile> candidateTiles = new ArrayList<AgentTile>();
    private int FOV = 3;

    private String myState = Constants.FUEL_STATION_FINDING_STATE;

    private ZoneManager zoneManager;
    private AgentZone myZone;
    private AgentZone fuelStationZone = null;

    public boolean isDistracted = false;

    public AgentMemory(TWEnvironment environment, String myName, AgentTile currentAgentTile) {
	this.environment = environment;
	this.zoneManager = ZoneManager.getInstance(environment);
	this.agentName = myName;
	this.myZone = zoneManager.getZone();
	this.initializeAgentTiles(currentAgentTile);
	return;
    }

    private void initializeAgentTiles(AgentTile currentAgentTile) {
	this.candidateTiles = this.myZone.getZoneTilePath();
	int bestDistance = 999;
	int closestTileIndex = -1;
	// AgentTile closestTile = new AgentTile(-1, -1);
	for (int i = 0; i < this.candidateTiles.size(); i++) {
            AgentTile pathTile = this.candidateTiles.get(i);
            int pathTileDistance = this.distanceBetweenTiles(currentAgentTile.x, currentAgentTile.y, pathTile.x, pathTile.y);
            if (pathTileDistance < bestDistance) {
                bestDistance = pathTileDistance;
                closestTileIndex = i;
            }
        }
	
	this.reshuffleUntilTileReached(closestTileIndex);
	
	
    }
    
    private void reshuffleUntilTileReached(int targetIndex) {
        if (targetIndex < 0 || targetIndex >= candidateTiles.size() || candidateTiles.size() <= 1) {
            return; // Nothing to reorder if index is invalid or list is too small
        }

        for (int i = 0; i < targetIndex; i++) {
            AgentTile firstTile = candidateTiles.remove(0);
            candidateTiles.add(firstTile);
        }
    }
    
    private int distanceBetweenTiles(int x1, int y1, int x2, int y2) {
	return Math.abs(x1 - x2) + Math.abs(y1 - y2);
    }

    public void setZoneByName(String requestedZoneName, AgentTile currentAgentTile) {
	// String lastZoneName = this.getZone().getZoneName();
	this.myZone = zoneManager.getZoneByZoneName(requestedZoneName);
	this.initializeAgentTiles(currentAgentTile);
    }

    public AgentTile getExplorationTile() {
	AgentTile randomTile = this.candidateTiles.getFirst();

	this.candidateTiles.removeFirst();
	this.candidateTiles.addLast(randomTile);
	return randomTile;
    }

    public void swapLastExplorationTile() {
	AgentTile lastTile = this.candidateTiles.getLast();
	this.candidateTiles.removeLast();
	this.candidateTiles.addFirst(lastTile);
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
	this.setFuelStationZone();
    }
    
    private void setFuelStationZone() {
	if (this.fuelStationZone != null) {
	    return;
	}
	this.fuelStationZone = this.zoneManager.getClosestZoneToTile(this.fuelTile);
    }
    
    public AgentZone getFuelStationZone() {
	return this.fuelStationZone;
    }

    public TWPath getCurrentPath() {
	return currentPath;
    }

    public void setCurrentPath(TWPath newCurrentPath) {
	this.currentPath = newCurrentPath;
    }

    public boolean currentPathValid() {
	if (this.currentPath == null)
	    return false;
	return this.currentPath.hasNext();
    }

    public TWThought getNextThought() {
	if (!this.currentPathValid())
	    return null;
	TWPathStep nextStep = this.currentPath.popNext();
	TWThought nextThought = new TWThought(TWAction.MOVE, nextStep.getDirection());
	return nextThought;
    }

    public AgentZone getZone() {
	return this.myZone;
    }
}
