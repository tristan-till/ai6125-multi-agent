package tileworld.agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import tileworld.enums.RefuelRole;
import tileworld.environment.TWEnvironment;

public class RefuelScheduler {

    private static RefuelScheduler instance;
    // private String myName;
    private List<RefuelAgent> refuelAgents = new ArrayList<>();

    private RefuelScheduler() {
	return;
    }

    public static RefuelScheduler getInstance() {
	if (instance == null) {
	    instance = new RefuelScheduler();
	}
	return instance;
    }

    public void addRefuelAgent(int distance, String name) {
	if (this.refuelAgents.stream().anyMatch(agent -> agent.getName().equals(name))) {
	    return;
	}
	RefuelAgent newRefuelAgent = new RefuelAgent(name, distance, RefuelRole.UNKNOWN);
	this.refuelAgents.add(newRefuelAgent);
    }
    
    public Map<String, Integer> getOrderedRefuelAgents() {
	if (this.refuelAgents.size() < 3) {
	    return null;
	}
	this.reorderAgents();
	Map<String, Integer> refuelRequestInterval = new HashMap<String, Integer>();
	for (int i = 0; i < this.refuelAgents.size(); i++) {
	    RefuelAgent agent = this.refuelAgents.get(i);
	    refuelRequestInterval.put(agent.getName(), i * 500 / 3 + 75);
	}
	return refuelRequestInterval;
    }

    public boolean agentIsHead(String name) {
	if (refuelAgents.size() < 3) {
	    return false;
	}
	System.out.println(refuelAgents.getFirst().getName() + " -> " + name);
	if (refuelAgents.getFirst().getName().equals(name)) {
	    return true;
	}

	return false;
    }

    public boolean agentIsNext(String name) {
	if (refuelAgents.size() < 3) {
	    return false;
	}

	if (refuelAgents.get(1).getName().equals(name)) {
	    return true;
	}

	return false;
    }

    public boolean agentIsLast(String name) {
	if (refuelAgents.size() < 3) {
	    return false;
	}

	if (refuelAgents.getLast().getName().equals(name)) {
	    return true;
	}

	return false;
    }

    public void reorderAgents() {
	Collections.sort(refuelAgents, (agent1, agent2) -> Integer.compare(agent1.getDistance(), agent2.getDistance()));
    }

    private int getAgentIndexByName(String name) {
	for (int i = 0; i < this.refuelAgents.size(); i++) {
	    RefuelAgent agent = this.refuelAgents.get(i);
	    // Check if the agent is not null and its name matches
	    if (agent != null && Objects.equals(agent.getName(), name)) {
		return i; // Found the agent, return its index
	    }
	}
	return -1;
    }

    public String getAgentNameForSwitch(String name) {
	int myIndex = this.getAgentIndexByName(name);
	int otherIndex = (myIndex + 1) > refuelAgents.size() - 1  ? 0 : (myIndex + 1);
	RefuelAgent otherAgent = refuelAgents.get(otherIndex);
	return otherAgent.getName();
    }

    public int getRefuelRequestTimeInterval(String name) {
	if (refuelAgents.size() < 3) {
	    return 75;
	}

	if (this.agentIsHead(name)) {
	    return 75;
	}

	if (this.agentIsNext(name)) {
	    return 75 + Math.round(2*500 / 3);
	}

	if (this.agentIsLast(name)) {
	    return 75 + Math.round(500 / 3);
	}
	return 0;
    }
}
