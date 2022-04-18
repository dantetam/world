package io.github.dantetam.localdata;

import java.util.HashMap;
import java.util.Map;

import io.github.dantetam.vector.Vector2i;
import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.dataparse.ItemData;

public class ConstantData {

	public static final Vector2i WORLD_NUM_GRIDS = new Vector2i(1,1);
	public static final Vector3i GRID_SIZE = new Vector3i(200,200,50);
	
	public static boolean ADVANCED_PATHING = true;
	public static double A_STAR_CUR_PATH_MULTIPLIER = 1.0,
			A_STAR_HEUR_MULTIPLIER = 0.45,
			A_STAR_ACCESS_PEN_MULTI = 30;
	
	public static int MEMORY_PATH_CUTOFF = 30; //Latch onto stored paths 
	
	public static int NUM_JOBPROCESS_CONSIDER = 40;
	
	public static final String MOUSE_HIGHLIGHT_NO_CLICK = "_highlight";

	public static Map<Integer, Double> clusterUbiquityMap = new HashMap<Integer, Double>() {{
		put(ItemData.getIdFromName("Copper Sludge"), 16.0);
		put(ItemData.getIdFromName("Iron Sludge"), 6.0);
		
		put(ItemData.getIdFromName("Wild Fruit Bush"), 8.0);
		put(ItemData.getIdFromName("Wild Berry Bush"), 15.0);
		put(ItemData.getIdFromName("Wheat Stalks"), 12.0);
		put(ItemData.getIdFromName("Cotton Plant"), 8.0);
		put(ItemData.getIdFromName("Flax Plant"), 8.0);
		put(ItemData.getIdFromName("Grass"), 32.0);
	}};
	
	public static Map<Integer, Double> clusterSizesMap = new HashMap<Integer, Double>() {{
		put(ItemData.getIdFromName("Copper Sludge"), 3.0);
		put(ItemData.getIdFromName("Iron Sludge"), 2.0);
		
		put(ItemData.getIdFromName("Wild Fruit Bush"), 4.0);
		put(ItemData.getIdFromName("Wild Berry Bush"), 5.0);
		put(ItemData.getIdFromName("Wheat Stalks"), 6.0);
		put(ItemData.getIdFromName("Cotton Plant"), 4.0);
		put(ItemData.getIdFromName("Flax Plant"), 4.0);
		put(ItemData.getIdFromName("Grass"), 10.0);
	}};
	
}
