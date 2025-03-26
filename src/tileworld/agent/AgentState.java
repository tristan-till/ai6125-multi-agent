package tileworld.agent;

public class AgentState {
    public String state1;
    public int schemeOrSearchPattern;
    public int targetIndex;
    
    public AgentState(String state1, int schemeOrSearchPattern, int targetIndex) {
	this.state1 = state1;
	this.schemeOrSearchPattern = schemeOrSearchPattern;
	this.targetIndex = targetIndex;
    }
}
