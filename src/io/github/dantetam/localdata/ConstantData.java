package io.github.dantetam.localdata;

import java.util.HashMap;
import java.util.Map;

import io.github.dantetam.world.dataparse.ItemData;

public class ConstantData {

	public static final String MOUSE_HIGHLIGHT_NO_CLICK = "_highlight";

	public static Map<Integer, Double> clusterUbiquityMap = new HashMap<Integer, Double>() {{
		put(ItemData.getIdFromName("Copper Sludge"), 12.0);
		put(ItemData.getIdFromName("Iron Sludge"), 4.0);
		
		put(ItemData.getIdFromName("Wild Fruit Bush"), 8.0);
		put(ItemData.getIdFromName("Wild Berry Bush"), 15.0);
		put(ItemData.getIdFromName("Wheat Stalks"), 12.0);
		put(ItemData.getIdFromName("Cotton Plant"), 8.0);
		put(ItemData.getIdFromName("Flax Plant"), 8.0);
	}};
	
	public static Map<Integer, Double> clusterSizesMap = new HashMap<Integer, Double>() {{
		put(ItemData.getIdFromName("Copper Sludge"), 3.0);
		put(ItemData.getIdFromName("Iron Sludge"), 2.0);
		
		put(ItemData.getIdFromName("Wild Fruit Bush"), 4.0);
		put(ItemData.getIdFromName("Wild Berry Bush"), 5.0);
		put(ItemData.getIdFromName("Wheat Stalks"), 6.0);
		put(ItemData.getIdFromName("Cotton Plant"), 4.0);
		put(ItemData.getIdFromName("Flax Plant"), 4.0);
	}};
	
}
