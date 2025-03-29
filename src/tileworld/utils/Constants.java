package tileworld.utils;

public class Constants {
	public static String DEFAULT_NAME_AGENT_1 = "agent1"; 
	
	public static String PRIVATE_CHANNEL = "PRIVATE_CHANNEL";
	public static String PUBLIC_CHANNEL = "PUBLIC_CHANNEL";
	
	public static final String REQUEST_MESSAGE = "Request";
	public static final String REQUIRE_MESSAGE = "Require";
	public static final String PATROL_MESSAGE = "cPlanSearchPointInitial";
	public static final String LAST_SEARCH_FIELD_MESSAGE = "MyLastSearchField";
	public static final String UPDATE_MEMORY_MAP_MESSAGE = "UpdateMemoryMap";
	public static final String MY_ESTIMATED_LIFE_TIME_MESSAGE = "MyEstimateLifeTime";
	public static final String POSITION_MESSAGE = "MyPosition";
	public static final String CARRIED_TILES_MESSAGE = "MyCarriedTiles";
	public static final String STATE_MESSAGE = "MyState";
	public static final String GO_FIND_FUEL_STATION_MESSAGE = "GoToFindFuelStation";
	public static final String FOUND_FUEL_STATION_MESSAGE = "FindFuelStation";
	public static final String DENSE_AREA_FOUND = "bPlanPickAreaUpdate";
	public static final String DISTANCE_TO_FUEL_STATION_MESSAGE = "DistanceToFuelStation";
	public static final String REQUEST_ZONE_SWITCH = "RequestZoneSwitch";
	public static final String ACCEPT_ZONE_SWITCH = "AcceptZoneSwitch";
	
	public static final String ZONE_1_NAME = "Zone1Name";
	public static final String ZONE_2_NAME = "Zone2Name";
	public static final String ZONE_3_NAME = "Zone3Name";
	
	public static final String REFUEL_SCHEDULE = "RefuelSchedule";
	
	public static String SEARCH_SCHEME = "A";
	public static String PICK_SCHEME = "B";
	public static String PATROL_SCHEME = "C";
	
	public static final String SEARCHING_TILE_STATE = "SearchingTile";
	public static final String PICKING_TILE_STATE = "PickingTile";
	public static final String PATROL_STATE = "planC";
	public static final String FUEL_STATION_FINDING_STATE = "FuelStationFinding";
	public static final String ADDING_FUEL_STATE = "AddingFuel";
	public static final String IDLE_STATE = "idle";
	
}

