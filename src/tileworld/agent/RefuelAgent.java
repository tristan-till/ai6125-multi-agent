package tileworld.agent;

import tileworld.enums.RefuelRole;

public class RefuelAgent {
    
    private String name;
    private int distance;
    private RefuelRole role;
    
    public RefuelAgent(String name, int distance, RefuelRole role) {
	this.name = name;
	this.distance = distance;
	this.role = role;
    }
    
    public String getName() {
	return this.name;
    }
    
    public int getDistance() {
	return this.distance;
    }
}
