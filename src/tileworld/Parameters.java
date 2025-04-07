package tileworld;

/**
 * Parameters
 *
 * @author michaellees
 * Created: Apr 21, 2010
 *
 * Copyright michaellees 
 *
 * Description:
 *
 * Class used to store global simulation parameters.
 * Environment related parameters are still in the TWEnvironment class.
 *
 */
public class Parameters {

    //Simulation Parameters
    public final static int seed = 4162012; //no effect with gui
    public static final long endTime = 5000; //no effect with gui

    //Agent Parameters
    public static final int defaultFuelLevel = 500;
    public static final int defaultSensorRange = 3;
    
    public static final int config1XYDimensions = 50;
    public static final double config1Mean = 0.2;
    public static final double config1Dev = 0.05f;
    public static final int config1Lifetime = 100;
    
    public static final int config2XYDimensions = 80;
    public static final double config2Mean = 2.0;
    public static final double config2Dev = 0.5f;
    public static final int config2Lifetime = 30;
    
    

    //Environment Parameters
    // public static final int xDimension = config1XYDimensions; //size in cells
    // public static final int yDimension = config1XYDimensions;
    

    // public static final double tileMean = config1Mean;
    // public static final double holeMean = config1Mean;
    // public static final double obstacleMean = config1Mean;
    // public static final double tileDev = config1Dev;
    // public static final double holeDev = config1Dev;
    // public static final double obstacleDev = config1Dev;
    // public static final int lifeTime = config1Lifetime;
    
    public static final int xDimension = config2XYDimensions; //size in cells
    public static final int yDimension = config2XYDimensions;
    public static final double tileMean = config2Mean;
    public static final double holeMean = config2Mean;
    public static final double obstacleMean = config2Mean;
    public static final double tileDev = config2Dev;
    public static final double holeDev = config2Dev;
    public static final double obstacleDev = config2Dev;
    public static final int lifeTime = config2Lifetime;

}
