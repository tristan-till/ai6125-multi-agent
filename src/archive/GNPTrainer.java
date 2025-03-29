package archive;

import tileworld.environment.TWEnvironment;
import java.io.*;
import java.util.*;

/**
 * GNPTrainer
 * Trains and evolves GNP-based agents for Tileworld.
 */
public class GNPTrainer {
	
    private static final int GENERATIONS = 500;
    private static final int POPULATION_SIZE = 100;
    private static final double MUTATION_RATE = 0.2;
    private static final double CROSSOVER_RATE = 0.5;

    private TWEnvironment env;
    private List<GNPAgent> agents;
    private Random random = new Random();

    public GNPTrainer(TWEnvironment env) {
        this.env = env;
        this.agents = new ArrayList<>();
    }

    /** Initialize random agents */
    private void initializePopulation() {
        for (int i = 0; i < POPULATION_SIZE; i++) {
            agents.add(new GNPAgent("Agent" + i, random.nextInt(env.getxDimension()), random.nextInt(env.getyDimension()), env, 500));
        }
    }

    /** Evaluate fitness of each agent */
    private double evaluateFitness(GNPAgent agent) {
        env.start(); // Reset environment for a clean run
        double initialScore = agent.getScore();
        for (int i = 0; i < 1000; i++) { // Run simulation for 1000 steps
            agent.step(env);
        }
        double finalScore = agent.getScore();
        // System.out.println(finalScore);
        return finalScore - initialScore; // Fitness = Reward gained
    }

    /** Selection: Pick best agents */
    private List<GNPAgent> selectTopAgents() {
        agents.sort(Comparator.comparingDouble(this::evaluateFitness).reversed());
        double topScore = agents.getFirst().getScore();
        System.out.println(topScore);
        return agents.subList(0, POPULATION_SIZE / 2); // Keep top half
    }

    /** Crossover: Merge two agents' GNP graphs */
    private GNPAgent crossover(GNPAgent parent1, GNPAgent parent2) {
        GNPAgent child = new GNPAgent("Child", random.nextInt(env.getxDimension()), random.nextInt(env.getyDimension()), env, 500);
        int split = parent1.nodes.size() / 2;
        child.nodes.clear();
        child.nodes.addAll(parent1.nodes.subList(0, split));
        child.nodes.addAll(parent2.nodes.subList(split, parent2.nodes.size()));
        return child;
    }

    /** Mutation: Randomly modify an agent's graph */
    private void mutate(GNPAgent agent) {
        if (random.nextDouble() < MUTATION_RATE) {
            int index = random.nextInt(agent.nodes.size());
            GNPNode mutatedNode = agent.nodes.get(index);

            if (mutatedNode.type == NodeType.JUDGMENT) {
                while (mutatedNode.edges.size() < 2) {
                    mutatedNode.addEdge(agent.nodes.get(random.nextInt(agent.nodes.size())));
                }
            } else {
                while (mutatedNode.edges.isEmpty()) { // Ensure at least one edge
                    mutatedNode.addEdge(agent.nodes.get(random.nextInt(agent.nodes.size())));
                }
            }
        }

        // Ensure at least one action node exists
        if (agent.nodes.stream().noneMatch(n -> n.type == NodeType.ACTION)) {
            agent.newRandomActionNode();
        }
    }


    /** Train agents over multiple generations */
    public void train() {
        initializePopulation();

        for (int gen = 0; gen < GENERATIONS; gen++) {
            System.out.println("Generation " + gen);

            List<GNPAgent> topAgents = selectTopAgents();
            List<GNPAgent> newGeneration = new ArrayList<>(topAgents);

            while (newGeneration.size() < POPULATION_SIZE) {
                if (random.nextDouble() < CROSSOVER_RATE) {
                    GNPAgent parent1 = topAgents.get(random.nextInt(topAgents.size()));
                    GNPAgent parent2 = topAgents.get(random.nextInt(topAgents.size()));
                    newGeneration.add(crossover(parent1, parent2));
                } else {
                    newGeneration.add(new GNPAgent("Mutant", random.nextInt(env.getxDimension()), random.nextInt(env.getyDimension()), env, 500));
                }
            }

            newGeneration.forEach(this::mutate);
            agents = newGeneration;
        }

        // Save best agent
        saveBestAgent(agents.get(0));
    }

    /** Save best agent to file */
    private void saveBestAgent(GNPAgent agent) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("best_agent.dat"))) {
            oos.writeObject(agent);
            System.out.println("Best agent saved!");
        } catch (IOException e) {
            System.err.println("Error saving agent: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Load trained agent from file and reinitialize transient fields */
    public static GNPAgent loadBestAgent(TWEnvironment env) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("best_agent.dat"))) {
            GNPAgent agent = (GNPAgent) ois.readObject();
            agent.initializeTransientFields(env); // Restore transient fields
            System.out.println("Loaded trained agent!");
            return agent;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    /** Entry point */
    public static void main(String[] args) {
        TWEnvironment env = new TWEnvironment();
        env.start();
        GNPTrainer trainer = new GNPTrainer(env);
        trainer.train();
    }
}
