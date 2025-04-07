package tileworld.agent;

import java.util.ArrayList;
import java.util.List;

import tileworld.environment.TWEnvironment;

public class AgentZone {

    private String zoneName;
    private int minX;
    private int maxX;
    private int minY;
    private int maxY;

    private int FOV = 3;
    private int stepSize = 2*FOV+1;

    private List<AgentTile> nodes = new ArrayList<>();
    // private String agentName = null;

    public AgentZone(String zoneName, int minX, int maxX, int minY, int maxY) {
	this.zoneName = zoneName;
	this.minX = minX;
	this.maxX = maxX;
	this.minY = minY;
	this.maxY = maxY;

	this.generatePath();
	return;
    }
    
    public String getZoneName() {
	return this.zoneName;
    }
    
    public List<AgentTile> getZoneTilePath() {
	return this.nodes;
    }

    private void generatePath() {
	AgentTile nextNode = this.getFirstNode();
	nextNode = this.goDownFromNode(nextNode);
	nextNode = this.goAcrossAndUp(nextNode);
	this.finishLoop(nextNode);
	return;
    }

    private AgentTile getFirstNode() {
	AgentTile nextNode = new AgentTile(this.minX + this.FOV, this.minY + this.FOV);
	this.nodes.add(nextNode);
	return nextNode;

    }

    private AgentTile goDownFromNode(AgentTile node) {
	int currentY = node.y;
	AgentTile nextNode = null;
	while (currentY + this.stepSize < this.maxY) {
	    currentY += this.stepSize;
	    nextNode = new AgentTile(node.x, currentY);
	    nodes.add(nextNode);
	}
	nextNode = new AgentTile(node.x, this.maxY - this.FOV);
	nodes.add(nextNode);
	return nextNode;
    }

    private AgentTile goRight(AgentTile node) {
	int currentX = node.x;
	AgentTile nextNode = null;
	while (currentX < this.maxX - this.FOV) {
	    currentX += this.stepSize;
	    nextNode = new AgentTile(currentX, node.y);
	    nodes.add(nextNode);
	}
	nextNode = new AgentTile(this.maxX - this.FOV, node.y);
	nodes.add(nextNode);
	return nextNode;
    }

    private AgentTile goLeft(AgentTile node) {
	int currentX = node.x;
	AgentTile nextNode = null;
	while (currentX > this.minX + this.FOV + this.stepSize) {
	    currentX -= this.stepSize;
	    nextNode = new AgentTile(currentX, node.y);
	    nodes.add(nextNode);
	}
	nextNode = new AgentTile(this.minX + this.FOV, node.y);
	nodes.add(nextNode);
	return nextNode;
    }

    private AgentTile goAcrossAndUp(AgentTile node) {
	AgentTile nextNode = node;
	int currentY = nextNode.y;
	int currentX = nextNode.x;
	while (currentY - this.stepSize > this.minY) {
	    if (currentX < this.maxX / 2) {
		nextNode = this.goRight(nextNode);
		
	    } else {
		nextNode = this.goLeft(nextNode);
		
	    }
	    nextNode = this.stepUp(nextNode);
	    currentY = nextNode.y;
	    currentX = nextNode.x;
	}

	return nextNode;
    }
    
    private AgentTile stepUp(AgentTile node) {
	AgentTile nextNode = new AgentTile(node.x, Math.max(node.y - this.stepSize, this.minY + this.FOV));
	nodes.add(nextNode);
	return nextNode;
    }
    
    private AgentTile stepLeft(AgentTile node) {
	AgentTile nextNode = new AgentTile(node.x - this.stepSize, node.y);
	nodes.add(nextNode);
	return nextNode;
    }
    
    private AgentTile stepDown(AgentTile node) {
	AgentTile nextNode = new AgentTile(node.x, node.y + this.stepSize);
	nodes.add(nextNode);
	return nextNode;
    }
    
    private void zigZagBack(AgentTile node) {
	int currentX = node.x;
	int currentY = node.y;
	while (currentX - 2 * this.stepSize > this.minX && currentY - this.FOV > this.minY) {
	    if (currentY > this.minY + this.FOV) {
		node = this.stepUp(node);
	    } else {
		node = this.stepDown(node);
	    }
	    node = this.stepLeft(node);
	    currentX = node.x;
	    currentY = node.y;
	}
    }
    
    private void finishLoop(AgentTile node) {
	AgentTile nextNode = node;
	int currentY = nextNode.y;
	int currentX = nextNode.x;
	if (currentX < this.maxX / 2) {
	    nextNode = this.goRight(nextNode);
	    nextNode = this.stepUp(nextNode);
	    nextNode = this.goLeft(nextNode);
	    this.nodes.removeLast();
	} else {
	    this.zigZagBack(nextNode);
	}
    }

}
