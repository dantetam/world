package io.github.dantetam.world.items;

import java.util.HashMap;
import java.util.Map;

import io.github.dantetam.world.grid.LocalBuilding;

public class Recipe {

	public LocalBuilding requiredBuilding;
	public Map<Integer, Integer> input; //Map: item id -> required quantity
	public Map<Integer, Integer> output; //Map: item id -> required quantity
	
	public Recipe() {
		input = new HashMap<>();
		output = new HashMap<>();
	}
	
}
