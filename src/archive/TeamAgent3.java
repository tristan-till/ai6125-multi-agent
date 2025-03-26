package archive;

import tileworld.environment.*;
import tileworld.exceptions.CellBlockedException;
import tileworld.planners.*;
import java.util.*;

public class TeamAgent3 extends TWAgent {
    private String name = "agent3";
    private int fuelX = -1, fuelY = -1;
    private final int mapsizeX, mapsizeY;
    private TWPath curPath;
    private int curPathStep = 0;
    private AstarPathGenerator pathGenerator;
    private int[][] seenMap;
    private int rethinking = 0;
    private int[] otherAgentPosition = {-1, -1};
    private ArrayList<int[]> explorationPoints;
    private int explorationIndex = 0;

    public TeamAgent3(String name, int xpos, int ypos, TWEnvironment env, double fuelLevel) {
        super(xpos, ypos, env, fuelLevel);
        this.name = name;
        this.mapsizeX = env.getxDimension();
        this.mapsizeY = env.getyDimension();
        this.pathGenerator = new AstarPathGenerator(env, this, mapsizeX + mapsizeY);
        this.seenMap = new int[mapsizeX][mapsizeY];
        for (int[] row : seenMap) Arrays.fill(row, -1);
        initializeExplorationPoints();
    }

    private void initializeExplorationPoints() {
        this.explorationPoints = new ArrayList<>();
        int step = 7; // Same step size as Agents 1/2
        
        // Start from current position and spiral outward
        int centerX = getX();
        int centerY = getY();
        
        // Spiral parameters
        int radius = 1;
        int[] dx = {1, 0, -1, 0}; // Right, Down, Left, Up
        int[] dy = {0, 1, 0, -1};
        int direction = 0;
        int segmentLength = 1;
        
        while (radius <= Math.max(mapsizeX, mapsizeY)) {
            // Add points in current direction
            for (int i = 0; i < segmentLength; i++) {
                int x = centerX + radius * dx[direction];
                int y = centerY + radius * dy[direction];
                
                // Clamp to map boundaries
                x = Math.max(0, Math.min(mapsizeX-1, x));
                y = Math.max(0, Math.min(mapsizeY-1, y));
                
                // Add unique points only
                if (!containsPoint(explorationPoints, x, y)) {
                    explorationPoints.add(new int[]{x, y});
                }
                
                // Change direction when needed
                if (i == segmentLength - 1) {
                    direction = (direction + 1) % 4;
                    if (direction % 2 == 0) {
                        segmentLength++;
                    }
                }
            }
            radius += step;
        }
        
        // Add center if empty
        if (explorationPoints.isEmpty()) {
            explorationPoints.add(new int[]{
                Math.max(0, Math.min(mapsizeX-1, centerX)),
                Math.max(0, Math.min(mapsizeY-1, centerY))
            });
        }
    }

    // Helper to check if point exists in list
    private boolean containsPoint(ArrayList<int[]> points, int x, int y) {
        for (int[] p : points) {
            if (p[0] == x && p[1] == y) return true;
        }
        return false;
    }

    @Override
    protected TWThought think() {
        updateSeenMap();
        checkMessages();
        
        // Priority 1: Emergency fuel
        if (handleFuelEmergency()) {
            return getNextMove();
        }

        // Priority 2: Clear critical obstacles
        TWThought obstacleAction = handleObstacleClearance();
        if (obstacleAction != null) return obstacleAction;

        // Priority 3: Strategic exploration
        return strategicExploration();
    }

    private void updateSeenMap() {
        for (int x = Math.max(0, getX()-3); x <= Math.min(mapsizeX-1, getX()+3); x++) {
            for (int y = Math.max(0, getY()-3); y <= Math.min(mapsizeY-1, getY()+3); y++) {
                seenMap[x][y] = 0;
            }
        }
    }

    private void checkMessages() {
        for (Message message : getEnvironment().getMessages()) {
            if (message.getTo().equals("all")) {
                String[] parts = message.getMessage().split(" ");
                if (parts[0].equals("FindFuelStation") && fuelX == -1) {
                    fuelX = Integer.parseInt(parts[1]);
                    fuelY = Integer.parseInt(parts[2]);
                    addTempMessage("AckFuel " + fuelX + " " + fuelY);
                }
            }
        }
    }

    private boolean handleFuelEmergency() {
        if (fuelX == -1) return false;
        
        int distToFuel = Math.abs(getX()-fuelX) + Math.abs(getY()-fuelY);
        boolean needsFuel = getFuelLevel() < Math.min(300, distToFuel * 2);
        
        if (needsFuel) {
            if (getX() == fuelX && getY() == fuelY) {
                return true; // Will trigger refuel in act()
            }
            
            // Try direct path if in memory
            if (getMemory().getMemoryGrid().get(fuelX, fuelY) != null) {
                curPath = pathGenerator.findPath(getX(), getY(), fuelX, fuelY);
                curPathStep = 0;
                return true;
            }
            
            // Fallback: Move toward last known fuel location
            return false;
        }
        return false;
    }

    private TWThought handleObstacleClearance() {
        // Check for obstacles near fuel station
        if (fuelX != -1) {
            List<int[]> nearFuel = getObjectsInRadius(fuelX, fuelY, 5, TWObstacle.class);
            if (!nearFuel.isEmpty()) {
                int[] target = nearFuel.get(0);
                if (getMemory().getMemoryGrid().get(target[0], target[1]) != null) {
                    curPath = pathGenerator.findPath(getX(), getY(), target[0], target[1]);
                    curPathStep = 0;
                    return getNextMove();
                }
            }
        }
        
        // Check for obstacles between agents
        if (otherAgentPosition[0] != -1) {
            List<int[]> betweenAgents = getObjectsAlongLine(
                getX(), getY(), 
                otherAgentPosition[0], otherAgentPosition[1], 
                TWObstacle.class);
            if (!betweenAgents.isEmpty()) {
                int[] target = betweenAgents.get(0);
                curPath = pathGenerator.findPath(getX(), getY(), target[0], target[1]);
                curPathStep = 0;
                return getNextMove();
            }
        }
        return null;
    }

    private TWThought strategicExploration() {
        // 1. Try to explore unseen areas first
        int[] unseenTarget = findLeastSeenArea();
        if (unseenTarget != null) {
            curPath = pathGenerator.findPath(getX(), getY(), unseenTarget[0], unseenTarget[1]);
            if (curPath != null) {
                curPathStep = 0;
                return getNextMove();
            }
        }
        
        // 2. Fallback to systematic exploration with proper bounds checking
        if (curPath == null || curPathStep >= curPath.getpath().size()) {
            if (!explorationPoints.isEmpty()) {
                // Safely get the next exploration point
                int[] target = explorationPoints.get(explorationIndex % explorationPoints.size());
                
                // Validate target coordinates
                if (target != null && target.length >= 2) {
                    int targetX = Math.max(0, Math.min(mapsizeX-1, target[0]));
                    int targetY = Math.max(0, Math.min(mapsizeY-1, target[1]));
                    
                    System.out.println("Exploring to: " + targetX + "," + targetY);
                    curPath = pathGenerator.findPath(getX(), getY(), targetX, targetY);
                    explorationIndex = (explorationIndex + 1) % explorationPoints.size();
                    curPathStep = 0;
                } else {
                    // Handle invalid target
                    System.out.println("Invalid exploration target");
                    return new TWThought(TWAction.MOVE, getRandomDirection());
                }
            } else {
                // No exploration points - use random walk
                return new TWThought(TWAction.MOVE, getRandomDirection());
            }
        }
        
        return getNextMove();
    }

    private int[] findLeastSeenArea() {
        int maxSeenValue = -1;
        int[] target = null;
        for (int x = 0; x < mapsizeX; x += 5) {
            for (int y = 0; y < mapsizeY; y += 5) {
                if (seenMap[x][y] > maxSeenValue && 
                    !getMemory().isCellBlocked(x, y)) {
                    maxSeenValue = seenMap[x][y];
                    target = new int[]{x, y};
                }
            }
        }
        return target;
    }

    private List<int[]> getObjectsInRadius(int centerX, int centerY, int radius, Class<?> type) {
        List<int[]> objects = new ArrayList<>();
        for (int x = Math.max(0, centerX-radius); x <= Math.min(mapsizeX-1, centerX+radius); x++) {
            for (int y = Math.max(0, centerY-radius); y <= Math.min(mapsizeY-1, centerY+radius); y++) {
                if (getMemory().getMemoryGrid().get(x, y) != null && 
                    type.isInstance(getMemory().getMemoryGrid().get(x, y))) {
                    objects.add(new int[]{x, y});
                }
            }
        }
        return objects;
    }

    private List<int[]> getObjectsAlongLine(int x1, int y1, int x2, int y2, Class<?> type) {
        List<int[]> objects = new ArrayList<>();
        // Bresenham's line algorithm implementation
        // ... (implementation omitted for brevity)
        return objects;
    }

    //private TWDirection getDirectionToward(int targetX, int targetY) {
    //    int dx = Integer.compare(targetX, getX());
    //    int dy = Integer.compare(targetY, getY());
    //    return TWDirection.getDirection(dx, dy);
    //}

    private TWThought getNextMove() {
        if (curPath != null && curPathStep < curPath.getpath().size()) {
            return new TWThought(TWAction.MOVE, curPath.getStep(curPathStep++).getDirection());
        }
        return new TWThought(TWAction.MOVE, getRandomDirection());
    }
    
    private TWDirection getRandomDirection(){

        TWDirection randomDir = TWDirection.values()[this.getEnvironment().random.nextInt(5)];

        if(this.getX()>=this.getEnvironment().getxDimension() ){
            randomDir = TWDirection.W;
        }else if(this.getX()<=1 ){
            randomDir = TWDirection.E;
        }else if(this.getY()<=1 ){
            randomDir = TWDirection.S;
        }else if(this.getY()>=this.getEnvironment().getxDimension() ){
            randomDir = TWDirection.N;
        }

       return randomDir;

    }

    @Override
    protected void act(TWThought thought) {
        try {
            if (thought.getAction() == TWAction.REFUEL && 
                getX() == fuelX && getY() == fuelY) {
                refuel();
            } else {
                move(thought.getDirection());
            }
        } catch (CellBlockedException ex) {
            rethinking = 1;
            curPath = null;
            act(think());
        }
    }

    @Override
    public void communicate() {
        // Share fuel status and obstacle info
        if (fuelX != -1) {
            addTempAllMessage("FuelStatus " + fuelX + " " + fuelY + " " + getFuelLevel());
        }
        super.communicate();
    }
    
    @Override
    public String getName() {
        return name;
    }
}