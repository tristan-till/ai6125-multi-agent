package tileworld.agent;

import java.util.ArrayList;
import java.util.List;

import tileworld.environment.TWEnvironment;
import tileworld.utils.Constants;

public class ZoneManager {
    // Step 1: Private static instance
    private static ZoneManager instance;
    private TWEnvironment env;
    private int gridSizeX;
    private int gridSizeY;

    private AgentZone freeZone = null;

    private AgentZone zone1;
    private AgentZone zone2;
    private AgentZone zone3;
    private List<AgentZone> freeZones = new ArrayList<>();

    // Step 2: Private constructor
    private ZoneManager(TWEnvironment env) {
	this.env = env;
	this.gridSizeX = this.env.getxDimension();
	this.gridSizeY = this.env.getyDimension();
	this.initializeZones();
	return;
    }

    public static ZoneManager getInstance(TWEnvironment env) {
	if (instance == null) {
	    instance = new ZoneManager(env);
	}
	return instance;
    }
    
    public AgentZone getClosestZoneToTile(AgentTile tile) {
	int zone1Distance = this.getZoneDistanceToTile(zone1, tile);
	int zone2Distance = this.getZoneDistanceToTile(zone2, tile);
	int zone3Distance = this.getZoneDistanceToTile(zone3, tile);
	
	if (zone1Distance < zone2Distance && zone1Distance < zone3Distance) {
	    return zone1;
	}
	
	if (zone2Distance < zone1Distance && zone2Distance < zone3Distance) {
	    return zone2;
	}
	
	if (zone3Distance < zone1Distance && zone3Distance < zone2Distance) {
	    return zone3;
	}
	return null;
    }
    
    private int getZoneDistanceToTile(AgentZone zone, AgentTile tile) {
	int bestDistance = 999;
	
	for (int i = 0; i < zone.getZoneTilePath().size(); i++) {
            AgentTile pathTile = zone.getZoneTilePath().get(i);
            int pathTileDistance = this.distanceBetweenTiles(tile.x, tile.y, pathTile.x, pathTile.y);
            if (pathTileDistance < bestDistance) {
                bestDistance = pathTileDistance;
            }
        }
	return bestDistance;
    }
    
    private int distanceBetweenTiles(int x1, int y1, int x2, int y2) {
	return Math.abs(x1 - x2) + Math.abs(y1 - y2);
    }

    private void initializeZones() {
	this.initializeZone1();
	this.initializeZone2();
	this.initializeZone3();
    }

    private void initializeZone1() {
	int zoneMinX = 0;
	int zoneMaxX = Math.round(this.gridSizeX / 3);
	int zoneMinY = 0;
	int zoneMaxY = this.gridSizeY;
	zone1 = new AgentZone(Constants.ZONE_1_NAME, zoneMinX, zoneMaxX, zoneMinY, zoneMaxY);
	this.freeZones.add(zone1);
    }

    private void initializeZone2() {
	int zoneMinX = Math.round(this.gridSizeX / 3);
	int zoneMaxX = this.gridSizeX;
	int zoneMinY = 0;
	int zoneMaxY = Math.round(this.gridSizeY / 2);
	zone2 = new AgentZone(Constants.ZONE_2_NAME, zoneMinX, zoneMaxX, zoneMinY, zoneMaxY);
	this.freeZones.add(zone2);
    }

    private void initializeZone3() {
	int zoneMinX = Math.round(this.gridSizeX / 3);
	int zoneMaxX = this.gridSizeX;
	int zoneMinY = Math.round(this.gridSizeY / 2);
	int zoneMaxY = this.gridSizeY;
	zone3 = new AgentZone(Constants.ZONE_3_NAME, zoneMinX, zoneMaxX, zoneMinY, zoneMaxY);
	this.freeZones.add(zone3);
    }

    private void setFreeZone(String freeZoneName) {
	switch (freeZoneName) {
	case Constants.ZONE_1_NAME:
	    this.freeZones.add(zone1);
	    break;
	case Constants.ZONE_2_NAME:
	    this.freeZones.add(zone2);
	    break;
	case Constants.ZONE_3_NAME:
	    this.freeZones.add(zone3);
	    break;
	}
    }

    private AgentZone fetchZoneByName(String zoneName) {
	switch (zoneName) {
	case Constants.ZONE_1_NAME:
	    return zone1;
	case Constants.ZONE_2_NAME:
	    return zone2;
	case Constants.ZONE_3_NAME:
	    return zone3;
	}
	return null;
    }

    public AgentZone getZoneByZoneName(String zoneName) {
	return this.fetchZoneByName(zoneName);
    }

    public AgentZone getZone() {
	AgentZone zone = this.freeZones.getFirst();
	this.freeZones.removeFirst();
	return zone;
    }
}
