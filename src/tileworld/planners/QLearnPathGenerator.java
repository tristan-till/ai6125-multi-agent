package tileworld.planners;

import java.util.HashMap;
import java.util.Random;
import tileworld.agent.TWAgent;
import tileworld.environment.TWEnvironment;
import tileworld.environment.TWTile;
import tileworld.environment.TWEntity;
import tileworld.environment.TWDirection;
import java.util.List;
import java.util.ArrayList;

public class QLearnPathGenerator implements TWPathGenerator {
    private TWEnvironment map;
    private TWAgent agent;
    private double alpha = 0.1; // Learning rate
    private double gamma = 0.9; // Discount factor
    private double epsilon = 0.2; // Exploration rate
    private HashMap<String, Double> qTable;
    private Random random;
    
    public QLearnPathGenerator(TWEnvironment map, TWAgent agent) {
        this.map = map;
        this.agent = agent;
        this.qTable = new HashMap<>();
        this.random = new Random();
    }
    
    private String getStateActionKey(int x, int y, TWDirection action) {
        return x + "," + y + "," + action;
    }
    
    private double getQValue(int x, int y, TWDirection action) {
        return qTable.getOrDefault(getStateActionKey(x, y, action), 0.0);
    }
    
    public void updateQValue(int x, int y, TWDirection action, double reward, int newX, int newY) {
        double maxQ = Double.NEGATIVE_INFINITY;
        for (TWDirection dir : TWDirection.values()) {
            maxQ = Math.max(maxQ, getQValue(newX, newY, dir));
        }
        String key = getStateActionKey(x, y, action);
        double updatedQ = getQValue(x, y, action) + alpha * (reward + gamma * maxQ - getQValue(x, y, action));
        qTable.put(key, updatedQ);
    }
    
    public TWPath findPath(int sx, int sy, int tx, int ty) {
      if (agent.getMemory().isCellBlocked(tx, ty)) {
        return null;
      }
      if (random.nextDouble() < epsilon) {
        return randomPath(sx, sy, tx, ty);
      } else {
        return qLearnedPath(sx, sy, tx, ty);
      }
    }
    
    private TWPath randomPath(int sx, int sy, int tx, int ty) {
        List<TWDirection> directions = new ArrayList<>();
        directions.add(TWDirection.N);
        directions.add(TWDirection.S);
        directions.add(TWDirection.E);
        directions.add(TWDirection.W);
        
        TWPath path = new TWPath(tx,ty);
        while (sx != tx || sy != ty) {
            TWDirection randomDir = directions.get(random.nextInt(directions.size()));
            sx += randomDir.dx;
            sy += randomDir.dy;
            path.prependStep(sx, sy);
        }
        return path;
    }
    
    private TWPath qLearnedPath(int sx, int sy, int tx, int ty) {
        TWPath path = new TWPath(tx, ty);
        int cx, cy;
        while (sx != tx || sy != ty) {
            TWDirection bestAction = null;
            double bestQ = Double.NEGATIVE_INFINITY;
            
            for (TWDirection dir : TWDirection.values()) {
                double qValue = getQValue(sx, sy, dir);
                if (qValue > bestQ) {
                    bestQ = qValue;
                    bestAction = dir;
                }
            }
            
            if (bestAction == null) {
                bestAction = TWDirection.values()[random.nextInt(TWDirection.values().length)];
            }
            
            cx = sx + bestAction.dx;
            cy = sy + bestAction.dy;
            path.prependStep(cx, cy);
            // TODO: Determine Reward function for tiles and add it in.
            updateQValue(sx, sy, bestAction, reward, cx, cy);

        }
        return path;
    }
}
