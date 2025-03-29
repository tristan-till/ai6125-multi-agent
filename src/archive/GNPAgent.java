package archive;

import tileworld.agent.TWAction;
import tileworld.agent.TWAgent;
import tileworld.agent.TWAgentWorkingMemory;
import tileworld.agent.TWThought;
import tileworld.environment.TWDirection;
import tileworld.environment.TWEnvironment;
import tileworld.exceptions.CellBlockedException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;

/**
 * GNPAgent
 * Implements a Genetic Network Programming (GNP)-based agent for Tileworld.
 */
public class GNPAgent extends TWAgent {
	private static final long serialVersionUID = 1L;
	
	private String name;
    public List<GNPNode> nodes; // GNP graph
    private GNPNode currentNode; // Active node
    protected int addedScore = 0;
    
    private transient TWAgentWorkingMemory memory;
    private transient TWEnvironment environment;
    
    // private transient TWAgentWorkingMemory memory;

    public GNPAgent(String name, int xpos, int ypos, TWEnvironment env, double fuelLevel) {
        super(xpos, ypos, env, fuelLevel);
        this.name = name;
        initializeGNP();
        this.environment = env;
        
        
        this.memory = new TWAgentWorkingMemory(this, env.schedule, env.getxDimension(), env.getyDimension());
    }

    /** Initialize the GNP graph randomly */
    private void initializeGNP() {
        nodes = new ArrayList<>();
        Random rand = new Random();
        boolean hasActionNode = false;

        // Create nodes, ensuring at least one action node exists
        for (int i = 0; i < 10; i++) {
            if (rand.nextBoolean() || !hasActionNode) { // Ensure at least one action node
                nodes.add(new GNPNode(NodeType.ACTION, getRandomAction()));
                hasActionNode = true;
            } else {
                nodes.add(new GNPNode(NodeType.JUDGMENT, getRandomCondition()));
            }
        }

        // Ensure every judgment node has 2 outgoing edges
        for (GNPNode node : nodes) {
            while (node.edges.size() < (node.type == NodeType.JUDGMENT ? 2 : 1)) {
                GNPNode target = nodes.get(rand.nextInt(nodes.size()));
                if (!node.edges.contains(target)) {
                    node.addEdge(target);
                }
            }
        }

        // Start at a valid node
        currentNode = nodes.get(rand.nextInt(nodes.size()));
    }
    
    public void initializeTransientFields(TWEnvironment env) {
        this.environment = env;
        this.memory = new TWAgentWorkingMemory(this, env.schedule, env.getxDimension(), env.getyDimension());
    }

    /** Custom serialization: Exclude transient fields */
    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject(); // Serialize non-transient fields
    }

    /** Custom deserialization: Restore transient fields */
    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject(); // Deserialize non-transient fields
        this.memory = null; // Will be reinitialized after loading
        this.environment = null;
    }




    /** Think step: follow GNP graph to select an action */
    @Override
    protected TWThought think() {
        // System.out.println("Current Node: " + currentNode);

        int safetyCounter = 0;
        while (currentNode.type == NodeType.JUDGMENT) {
            if (currentNode.edges.size() < 2) {
                // System.out.println("Warning: Judgment node has insufficient edges! Fixing...");
                currentNode = fixInvalidJudgmentNode(currentNode);
                break;
            }

            boolean conditionMet = evaluateCondition(currentNode.condition);
            // System.out.println("Evaluating Condition: " + currentNode.condition + " -> " + conditionMet);
            currentNode = conditionMet ? currentNode.edges.get(0) : currentNode.edges.get(1);

            if (++safetyCounter > 10) {
                // System.out.println("Error: Stuck in judgment loop! Defaulting to random action.");
                currentNode = getRandomActionNode();
                break;
            }
        }

        // System.out.println("Selected Action Node: " + currentNode.action);
        return new TWThought(currentNode.action, getRandomDirection());
    }
    
    @Override
    public int getScore() {
        return super.getScore() + addedScore;
    }

    /** Fixes a judgment node with missing edges */
    private GNPNode fixInvalidJudgmentNode(GNPNode node) {
        Random rand = new Random();
        while (node.edges.size() < 2) {
            node.addEdge(nodes.get(rand.nextInt(nodes.size())));
        }
        return node;
    }

    /** Returns a random action node if needed */
    private GNPNode getRandomActionNode() {
        return nodes.stream().filter(n -> n.type == NodeType.ACTION).findAny()
            .orElseGet(() -> {
                System.out.println("Warning: No action nodes found! Creating one...");
                GNPNode newActionNode = new GNPNode(NodeType.ACTION, getRandomAction());
                nodes.add(newActionNode);
                return newActionNode;
            });
    }

    /** Act step: execute the selected action */
    @Override
    protected void act(TWThought thought) {
        try {
            switch (thought.getAction()) {
                case MOVE:
                	if (super.getFuelLevel() > 0) {
                		this.addedScore++;
                	}
                    this.move(thought.getDirection());
                    break;
                case PICKUP:
                    // Check if tile exists and pick up
                    break;
                case PUTDOWN:
                    // Check if at hole and drop tile
                    break;
                case REFUEL:
                	if (this.getEnvironment().inFuelStation(this)) {
                		System.out.println("successfully refueled!");
                		this.addedScore += 5;
                	}
                    this.refuel();
                    break;
            }
        } catch (CellBlockedException e) {
            // System.out.println("Move blocked! Recalculating...");
        }

        // Move to the next node safely
        if (!currentNode.edges.isEmpty()) {
            currentNode = currentNode.edges.get(0);
        } else {
            // System.out.println("Warning: Action node has no outgoing edges! Selecting new node...");
            currentNode = getRandomActionNode();
        }
    }


    /** Evaluate conditions for judgment nodes */
    private boolean evaluateCondition(GNPCondition condition) {
        switch (condition) {
            case TILE_NEARBY: return getMemory().getClosestObjectInSensorRange(tileworld.environment.TWTile.class) != null;
            case HOLE_NEARBY: return getMemory().getClosestObjectInSensorRange(tileworld.environment.TWHole.class) != null;
            case LOW_FUEL: return getFuelLevel() < 50;
            case CARRYING_TILE: return hasTile();
            default: return false;
        }
    }

    /** Randomly selects an action for action nodes */
    private TWAction getRandomAction() {
        TWAction[] actions = TWAction.values();
        return actions[new Random().nextInt(actions.length)];
    }
    
    public void newRandomActionNode() {
    	this.nodes.add(new GNPNode(NodeType.ACTION, this.getRandomAction()));
    }

    /** Randomly selects a condition for judgment nodes */
    private GNPCondition getRandomCondition() {
        GNPCondition[] conditions = GNPCondition.values();
        return conditions[new Random().nextInt(conditions.length)];
    }

    /** Get agent name */
    @Override
    public String getName() {
        return name;
    }

    /** Get random valid direction */
    private TWDirection getRandomDirection() {
        return TWDirection.values()[new Random().nextInt(5)];
    }
}

/** Enum for different node types */
enum NodeType {
    ACTION, JUDGMENT
}

/** Enum for judgment conditions */
enum GNPCondition {
    TILE_NEARBY, HOLE_NEARBY, LOW_FUEL, CARRYING_TILE
}

/** Graph node class */
class GNPNode {
    NodeType type;
    TWAction action;
    GNPCondition condition;
    List<GNPNode> edges;

    public GNPNode(NodeType type, Object data) {
        this.type = type;
        this.edges = new ArrayList<>();

        if (type == NodeType.ACTION) {
            this.action = (TWAction) data;
        } else {
            this.condition = (GNPCondition) data;
        }
    }

    public void addEdge(GNPNode node) {
        if (edges.size() < 2) edges.add(node);
    }
}
