package tileworld.agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import sim.display.Console;
import tileworld.planners.AstarPathGenerator;
import tileworld.planners.TWPath;
import tileworld.enums.AgentState;
import tileworld.enums.RouteType;
import tileworld.environment.TWEnvironment;
import tileworld.environment.TWHole;
import tileworld.environment.TWTile;
import tileworld.utils.Constants;
import tileworld.utils.Helpers;

public class TeamAgent extends TWAgent {
    private String name = Constants.DEFAULT_NAME_AGENT_1;
    private String tempMessage = "";
    private String tempAllMessage = "";
    private int fuelX = -1;
    private int fuelY = -1;
    private final int mapSizeX = this.getEnvironment().getxDimension();
    private final int mapSizeY = this.getEnvironment().getyDimension();
    private int[][] seenMap = new int[mapSizeX][mapSizeY];
    private ArrayList<int[]> mapChain = new ArrayList<int[]>(); // [[x1,y1], [x2,y2], [x3,y3],...]
    private int mapChainLength = 0;
    private AgentState agentState1 = AgentState.INITIAL;
    private AgentState agentState2 = AgentState.UNDEFINED_0;
    private AgentState agentState3 = AgentState.UNDEFINED_0;
    private String otherAgentState1 = "";
    private int otherAgentState2 = 0;
    private int otherAgentState3 = 0;
    private int otherCarriedTiles = 0;
    private int[] otherASF = new int[] { -5, -5, -5, -5 };
    private TWPath curPath = null;
    private int curPathStep = 0;
    private AstarPathGenerator pathGenerator = new AstarPathGenerator(this.getEnvironment(), this, this.mapSizeX + this.mapSizeY);
    private ArrayList<int[]> searchTileChain = new ArrayList<int[]>();
    private int rethinking = 0;
    private int pickVanished = 0;
    private int[] otherAgentPosition = { -1, -1 };
    public ArrayList<int[]> pick_tile_chain = new ArrayList<int[]>();
    private ArrayList<int[]> bPlanPickArea = new ArrayList<int[]>();
    private int[] bPlanPickTarget = new int[] { -1, -1 };
    private ArrayList<int[]> cPlanSearchPoint = new ArrayList<int[]>();
    private int[] cPlanFuelPoint = new int[] { -1, -1 };

    public TeamAgent(String name, int xpos, int ypos, TWEnvironment env, double fuelLevel) {
	super(xpos, ypos, env, fuelLevel);
	this.name = name;
	this.initializeMapChain();
    }

    private void cPlanSearchPointInitial() {
	if (this.cPlanSearchPoint.size() > 0)
	    return;

	int searchPointX = this.mapSizeX - 4;
	int searchPointY = this.mapSizeY - 4;
	if (Math.abs(this.mapSizeX / 2 - this.fuelX) < Math.abs(this.mapSizeY / 2 - this.fuelY)) {
	    searchPointX = this.mapSizeX / 2 - 4;
	} else {
	    searchPointY = this.mapSizeY / 2 - 4;
	}

	this.addSearchPoints(searchPointX, searchPointY);
	this.cPlanInitalFuelCheckpoint();
    }

    private void addSearchPoints(int searchPointX, int searchPointY) {
	this.cPlanSearchPoint.clear();
	this.cPlanSearchPoint.add(new int[] { 3, 3 });
	cPlanSearchPoint.add(new int[] { searchPointX, 3 });
	cPlanSearchPoint.add(new int[] { searchPointX, searchPointY });
	cPlanSearchPoint.add(new int[] { 3, searchPointY });
    }

    private void initializeMapChain() {
	int curX = 3;
	int curY = 3;
	int index = 0;
	int direction = 1;
	do {
	    int[] positions = this.stepInitializeMapChain(curX, curY, direction);
	    curX = positions[0];
	    curY = positions[1];
	} while (curX != 3 || curY != 3);

	this.mapChainLength = this.mapChain.size();
	return;
    }

    private int[] stepInitializeMapChain(int currentX, int currentY, int direction) {
	this.mapChain.add(new int[] { Math.min(currentX, this.mapSizeX - 3), Math.min(currentY, this.mapSizeY - 3) });

	if (direction == 1 && currentX + 3 < this.mapSizeX - 1) {
	    currentX += 7;
	} else if (direction == 1 && currentY + 3 < this.mapSizeY - 1) {
	    currentY += 7;
	    direction = -1;
	} else if (direction == 1) {
	    direction = -1;
	    currentX -= 7;
	} else if (direction == -1 && currentX > 11) {
	    currentX -= 7;
	} else if (direction == -1 && currentY + 3 < this.mapSizeY - 1) {
	    direction = 1;
	    currentY += 7;
	} else if (direction == -1) {
	    currentX -= 7;
	    direction = 0;
	} else if (direction == 0) {
	    currentY -= 7;
	}

	return new int[] { currentX, currentY };
    }

    private void cPlanInitalFuelCheckpoint() {
	int currentDistance = 99999;
	for (int k = 0; k <= 3; k++) {
	    currentDistance = this.stepCPlanForFuelCheckpoint(k, currentDistance);
	}
	System.out.println("cPlanFuelPoint x,y:" + cPlanFuelPoint[0] + " " + cPlanFuelPoint[1]);
    }

    private int stepCPlanForFuelCheckpoint(int k, int currentDistance) {
	int start = this.cPlanSearchPoint.get(k)[k % 2];
	int to = this.cPlanSearchPoint.get((k + 1) % 4)[k % 2];
	for (int i = Math.min(start, to); i < Math.max(start, to); i++) {
	    int currentX = (k % 2 == 0) ? i : this.cPlanSearchPoint.get(k)[0];
	    int currentY = (k % 2 == 1) ? i : this.cPlanSearchPoint.get(k)[1];
	    if (Math.abs(this.fuelX - currentX) + Math.abs(this.fuelY - currentY) < currentDistance) {
		this.cPlanFuelPoint = new int[] { currentX, currentY };
		currentDistance = Math.abs(this.fuelX - currentX) + Math.abs(this.fuelY - currentY);
	    }
	}
	return currentDistance;
    }

    private int nearSearchChainPoint() {
	int tileChainLength = this.searchTileChain.size();
	int nearIndex = 0;
	int curDis = 9999;
	for (int index = 0; index < tileChainLength; index++) {
	    if (Math.abs(this.getX() - this.searchTileChain.get(index)[0])
		    + Math.abs(this.getY() - searchTileChain.get(index)[1]) < curDis) {
		nearIndex = index;
		curDis = Math.abs(this.getX() - this.searchTileChain.get(index)[0])
			+ Math.abs(this.getY() - searchTileChain.get(index)[1]);
	    }
	}
	if (curDis == 0)
	    return (nearIndex + 1) % tileChainLength;
	return nearIndex;
    }

    private void initialSearchTileChain(int x1, int y1, int x2, int y2, int ax, int ay) {
	this.searchTileChain.clear();
	if (this.agentState2 == AgentState.UNDEFINED_1) {
	    this.initialSearchTileChainST1(x1, y1, x2, y2, ax, ay);
	} else if (this.agentState2 == AgentState.UNDEFINED_2) {
	    this.initalSearchTileChainST2(ax, ay);
	} else {
	    System.out.println("Throw Exception here!");
	}
    }

    private void initialSearchTileChainST1(int x1, int y1, int x2, int y2, int ax, int ay) {
	int currentX = x1;
	int currentY = y1;
	int index = -1;
	int currentDistance = 9999;
	int nearIndex = -1;
	int direction = 1;

	ArrayList<int[]> currentSearchTrain = new ArrayList<int[]>();
	do {
	    currentSearchTrain.add(new int[] { Math.min(currentX, x2), Math.min(currentY, y2) });
	    int[] values = this.stepInitialSearchTileChain(currentSearchTrain, direction, currentX, currentY, x1, y1,
		    x2, y2, ax, ay, currentDistance, index, nearIndex);
	} while (currentX != x1 || currentY != y1);

	for (int i = 0; i <= index; i++) {
	    this.searchTileChain.add(new int[] { (currentSearchTrain.get((nearIndex + i) % (index + 1))[0]),
		    (currentSearchTrain.get((nearIndex + i) % (index + 1))[1]) });
	}
    }

    private int[] stepInitialSearchTileChain(ArrayList<int[]> currentSearchTrain, int direction, int currentX,
	    int currentY, int x1, int y1, int x2, int y2, int ax, int ay, int currentDistance, int index,
	    int nearIndex) {

	index++;
	if (Math.abs(currentX - ax) + Math.abs(currentY - ay) < currentDistance) {
	    currentDistance = Math.abs(currentX - ax) + Math.abs(currentY - ay);
	    nearIndex = index;
	}
	if (direction == 1 && currentX < x2) {
	    currentX += 7;
	} else if (direction == 1 && currentY < y2) {
	    currentY += 7;
	    direction = -1;
	} else if (direction == 1) {
	    direction = -1;
	    currentX -= 7;
	} else if (direction == -1 && currentX > x1 + 7) {
	    currentX -= 7;
	} else if (direction == -1 && currentY < y2) {
	    direction = 1;
	    currentY += 7;
	} else if (direction == -1) {
	    currentX -= 7;
	    direction = 0;
	} else if (direction == 0) {
	    currentY -= 7;
	}
	return new int[] { index, nearIndex, currentX, currentY, currentDistance };
    }

    private void initalSearchTileChainST2(int ax, int ay) {
	boolean sizeMoreThan60 = false;
	int curX = this.mapSizeX / 2 - 17;
	int curY = this.mapSizeY / 2 - 17;
	int[] allPoint = new int[] { 0, 0, 1, 0, 1, 1, 2, 1, 2, 0, 3, 0, 3, 1, 4, 1, 4, 0, 5, 0, 5, 1, 5, 2, 5, 3, 4, 3,
		4, 2, 3, 2, 3, 3, 2, 3, 2, 2, 1, 2, 1, 3, 1, 4, 2, 4, 3, 4, 4, 4, 5, 4, 5, 5, 4, 5, 3, 5, 2, 5, 1, 5, 0,
		5, 0, 4, 0, 3, 0, 2, 0, 1 };
	int len = 6 * 6;
	if (this.mapSizeX >= 60 && this.mapSizeY >= 60) {
	    sizeMoreThan60 = true;
	    curX = this.mapSizeX / 2 - 24;
	    curY = this.mapSizeY / 2 - 21;
	    allPoint = new int[] { 0, 0, 1, 0, 1, 1, 2, 1, 2, 0, 3, 0, 3, 1, 4, 1, 4, 0, 5, 0, 5, 1, 6, 1, 6, 0, 7, 0,
		    7, 1, 7, 2, 7, 3, 6, 3, 6, 2, 5, 2, 5, 3, 4, 3, 4, 2, 3, 2, 3, 3, 2, 3, 2, 2, 1, 2, 1, 3, 1, 4, 1,
		    5, 2, 5, 2, 4, 3, 4, 3, 5, 4, 5, 4, 4, 5, 4, 5, 5, 6, 5, 6, 4, 7, 4, 7, 5, 7, 6, 6, 6, 5, 6, 4, 6,
		    3, 6, 2, 6, 1, 6, 0, 6, 0, 5, 0, 4, 0, 3, 0, 2, 0, 1 };
	    len = 8 * 7;
	}
	int x1 = curX;
	int y1 = curY;
	int cI = 0;
	int cDis = 999;
	for (int i = 0; i < len; i++) {
	    int px = allPoint[2 * i] * 7 + curX;
	    int py = allPoint[2 * i + 1] * 7 + curY;
	    if (Math.abs(px - ax) + Math.abs(py - ay) < cDis) {
		cDis = Math.abs(px - ax) + Math.abs(py - ay);
		cI = i;
	    }

	}
	for (int i = 0; i < len; i++) {
	    int realI = (i + cI) % (len);
	    this.searchTileChain.add(new int[] { allPoint[2 * realI] * 7 + curX, allPoint[2 * realI + 1] * 7 + curY });
	}

	for (int i = 0; i < this.searchTileChain.size(); i++) {
	    System.out.println(
		    "Bplan SearchChain x,y" + this.searchTileChain.get(i)[0] + " " + this.searchTileChain.get(i)[1]);
	}

    }

    private void updateSeenMap(int x, int y) {
	for (int i = x - 3; i < x + 4; i++) {
	    for (int j = y - 3; j < y + 4; j++) {
		if (0 <= i & i <= this.mapSizeX - 1 & 0 <= j & j <= this.mapSizeY - 1) {
		    seenMap[i][j] = 0;
		}
	    }
	}
    }

    private void unseenMapOneStep() {
	for (int i = 0; i < this.mapSizeX; i++) {
	    for (int j = 0; j < this.mapSizeY; j++) {
		if (seenMap[i][j] != -1 && seenMap[i][j] <= this.getEstimatedLifeTime() + 5000)
		    seenMap[i][j] += 1;
	    }
	}
    }

    public void clearTempMessage() {
	this.tempMessage = "";
	this.tempAllMessage = "";
    }

    public void addTempMessage(String mes) {
	this.tempMessage = this.tempMessage + ";" + mes;
    }

    public void addTempAllMessage(String mes) {
	this.tempAllMessage = this.tempAllMessage + ";" + mes;
    }

    @Override
    public void communicate() {
	// System.out.println(this.getName() + " communicate");
	Message message = new Message(this.name, "private", tempMessage);
	this.getEnvironment().receiveMessage(message); // this will send the message to the broadcast channel of the
						       // environment
	Message message2 = new Message(this.name, "all", tempAllMessage);
	this.getEnvironment().receiveMessage(message2);
	this.clearTempMessage();
    }

    private double objectLifeRemainEstimate(TWAgentPercept twp) {
	return this.getMemory().objectLifeRemainEstimate(twp);
    }

    private double objectLifetimeEstimate(TWAgentPercept twp) {
	return this.getMemory().objectLifetimeEstimate(twp);
    }

    private double distanceScore(double distance, TWAgentPercept currentMemory) {
	if (objectLifeRemainEstimate(currentMemory) <= distance) {
	    return 0.0;
	} else if (distance <= 2) {
	    return 1.0;
	}
	return 1.0 * Math.min((objectLifeRemainEstimate(currentMemory) - distance) / distance, 1);
    }

    private double memoryTileScore(int ax, int ay) {
	return this.memoryTilesScore(ax, ay, 2);
    }

    private double memoryTilesScore(int ax, int ay, int cTiles) {
	double totalScoreTile = 0;
	double totalScoreHole = 0;

	for (int x = 0; x < this.getMemory().getObjects().length; x++) {
	    for (int y = 0; y < this.getMemory().getObjects()[x].length; y++) {
		TWAgentPercept currentMemory = this.getMemory().getObjects()[x][y];
		if (currentMemory != null && currentMemory.getO() instanceof TWTile) {
		    totalScoreTile += this.distanceScore(Math.abs(ax - x) + Math.abs(ay - y), currentMemory);
		}
		if (currentMemory != null && currentMemory.getO() instanceof TWHole) {
		    totalScoreHole += this.distanceScore(Math.abs(ax - x) + Math.abs(ay - y), currentMemory);
		}
	    }
	}
	return totalScoreTile * (4 - cTiles) / 4 + totalScoreHole * cTiles / 4;
    }

    private int[] getSearchField() {
	return getSearchFieldFull(-5, -5, -5, -5);
    }

    private int[] getSearchFieldFull(int nx1, int ny1, int nx2, int ny2) {
	int[] xYLen = this.getXYLen();
	int xlen = xYLen[0];
	int ylen = xYLen[1];
	double curMax = 0.0;
	int curX = 0;
	int curY = 0;
	for (int i = 0; i <= this.mapSizeX - xlen; i += 7) {
	    for (int j = 0; j <= this.mapSizeY - ylen; j += 7) {
		double curScore = 0.0;
		for (int ii = 0; ii < xlen; ii++) {
		    for (int jj = 0; jj < ylen; jj++) {
			if ((nx1 - 3 - ii - i) * (ii + i - nx2 - 4) < 0 && (ny1 - 3 - jj - j) * (jj + j - ny2 - 4) < 0)
			    curScore += seenMap[i + ii][j + jj];
		    }
		}
		curScore /= (1 + (Math.abs(this.getX() - i - xlen / 2) + Math.abs(this.getX() - j - ylen / 2)) / 80);
		if (curScore >= curMax) {
		    curMax = curScore;
		    curX = i;
		    curY = j;
		}
	    }
	}
	return new int[] { curX + 3, curY + 3, curX + xlen - 4, curY + ylen - 4 };
    }

    private int[] getXYLen() {
	int xlen = 14;
	int ylen = 14;
	if (this.getEstimatedLifeTime() <= 50) {

	} else if (this.getEstimatedLifeTime() <= 75) {
	    xlen = 21;
	} else if (this.getEstimatedLifeTime() <= 100) {
	    xlen = 28;
	} else {
	    xlen = 21;
	    ylen = 28;
	}
	return new int[] { xlen, ylen };
    }

    private double getEstimatedLifeTime() {
	return this.getMemory().estimateLifeTime;
    }
    
    // TODO: Refactor code
    private int getPickRoute(RouteType type, int ax, int ay, int cTiles){
        int curCarried = cTiles;
        ArrayList<TWAgentPercept> possibleTH = this.getPossibleTH(ax, ay);
        
        if (possibleTH.size()>=8){
            for (int i=possibleTH.size()-1; i >= 8; i--){
                possibleTH.remove(possibleTH.get(i));
            }
        }
        // System.out.println("pickup Lenth " + possibleTH.size());

        ArrayList<int[]> curPossibleRoute = new ArrayList<int[]>();
        int curHighScore=0;
        int curPredictStep=0;
        ArrayList<int[]> finalRoute = new ArrayList<int[]>();
        curPossibleRoute.add(new int[]{0, curCarried});

        while (curPossibleRoute.size()>0){
            int[] tempR = curPossibleRoute.get(0);
            for (int i=0; i<possibleTH.size(); i++){
                if (Helpers.has(tempR, i, 2)) continue;
                TWAgentPercept curObj = possibleTH.get(i);
                int cDis;
                if (tempR[0]==0) cDis = (Math.abs(ax-curObj.getO().getX())+Math.abs(ay-curObj.getO().getY()) );
                else cDis = (int) curObj.getO().getDistanceTo(possibleTH.get(tempR[tempR.length-1]).getO());
                if (cDis + tempR[0] < objectLifeRemainEstimate(curObj)){
                    if (curObj.getO() instanceof TWTile && tempR[1] < 3) {
                        int[] tempR0=new int[tempR.length+1];
                        System.arraycopy(tempR, 0, tempR0, 0, tempR.length);
                        tempR0[0] = cDis + tempR[0];
                        tempR0[1] += 1;
                        tempR0[tempR0.length-1] = i;
                        curPossibleRoute.add(tempR0);
                    } else if (curObj.getO() instanceof TWHole && tempR[1] > 0){
                        int[] tempR0=new int[tempR.length+1];
                        System.arraycopy(tempR, 0, tempR0, 0, tempR.length);
                        tempR0[0] = cDis + tempR[0];
                        tempR0[1] -= 1;
                        tempR0[tempR0.length-1] = i;
                        curPossibleRoute.add(tempR0);
                    }
                }
            }
            if (tempR.length-2 > curHighScore || (tempR.length-2==curHighScore && tempR[0] < curPredictStep)){
                curHighScore = tempR.length-2;
                curPredictStep = tempR[0];
                finalRoute.clear();
                for (int j=2; j<tempR.length; j++) finalRoute.add(new int[]{possibleTH.get(tempR[j]).getO().getX(), possibleTH.get(tempR[j]).getO().getY()});
            }
            curPossibleRoute.remove(tempR);
        }
        if (type == RouteType.SCORE) return curHighScore;
        else {return 0;}
    }
    
    private ArrayList<TWAgentPercept> getPossibleTH(int ax, int ay) {
	TWAgent me=this;
	ArrayList<TWAgentPercept> possibleTH = new ArrayList<TWAgentPercept>();
	for (int x = 0; x < this.getMemory().getObjects().length; x++) {
            for (int y = 0; y < this.getMemory().getObjects()[x].length; y++) {
                TWAgentPercept currentMemory =  this.getMemory().getObjects()[x][y];
                if (!(currentMemory == null)  && (currentMemory.getO() instanceof TWTile || currentMemory.getO() instanceof TWHole )) {
                    if ( (Math.abs(ax-currentMemory.getO().getX())+Math.abs(ay-currentMemory.getO().getY()) ) <
                            objectLifeRemainEstimate(currentMemory)) possibleTH.add(currentMemory);
                }
            }
        }
	
	Collections.sort(possibleTH, new Comparator<TWAgentPercept>() {
            @Override
            public int compare(TWAgentPercept twa1, TWAgentPercept twa2){
                if (me.getDistanceTo(twa1.getO()) < me.getDistanceTo(twa2.getO())) return -1;
                else if (me.getDistanceTo(twa1.getO()) == me.getDistanceTo(twa2.getO())) return 0;
                return 1;
            }
        });
	return possibleTH;
    }
    
    
    @Override
    protected TWThought think() {
	return new TWThought(TWAction.MOVE);
    }
    
    @Override
    protected void act(TWThought thought) {
	return;
    }
    
    @Override
    public String getName() {
        return name;
    }

}