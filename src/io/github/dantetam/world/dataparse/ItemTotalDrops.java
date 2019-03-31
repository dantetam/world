package io.github.dantetam.world.dataparse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.dantetam.toolbox.MathUti;

/**
 * Represent a propabilistic item dropping. The format for one 'trial':
 * item id (required),
 * item drop min (default: 1), 
 * item drop max (default: 1), 
 * item probability (default: 1), 
 * distribution (default: uniform from min to max inclusive)
 * @author Dante
 *
 */

public class ItemTotalDrops {

	public List<ItemDropTrial> independentDrops;
	
	public ItemTotalDrops() {
		independentDrops = new ArrayList<>();
	}
	
	public List<int[]> getItemDrops() {
		List<int[]> trialDrops = new ArrayList<>();
		for (ItemDropTrial trial : independentDrops) {
			int[] drop = trial.getItemDrop();
			trialDrops.add(drop);
		}
		return trialDrops;
	}
	
	public Set<Integer> getAllItems() {
		Set<Integer> allItems = new HashSet<>();
		for (ItemDropTrial trial: independentDrops) {
			for (ItemDrop drop: trial.itemDrops) {
				allItems.add(drop.itemId);
			}
		}
		return allItems;
	}
	
	public Map<Integer, Double> itemExpectation() {
		Map<Integer, Double> allItems = new HashMap<>();
		for (ItemDropTrial trial: independentDrops) {
			for (ItemDrop drop: trial.itemDrops) {
				if (!allItems.containsKey(drop.itemId)) {
					allItems.put(drop.itemId, 0.0);
				}
				allItems.put(drop.itemId, allItems.get(drop.itemId) + (drop.min + drop.max) / 2 * drop.probability);
			}
		}
		return allItems;
	}
	
	//Establish a single independent 'trial' that can only drop one item
	public static class ItemDropTrial {
		public List<ItemDrop> itemDrops;
		
		public ItemDropTrial(List<ItemDrop> itemDrops) {
			this.itemDrops = itemDrops;
			double sum = 0;
			for (ItemDrop drop : itemDrops) {
				sum += drop.probability;
			}
			if (sum < 0 || sum > 1 || itemDrops.size() == 0) {
				throw new IllegalArgumentException("Probabilities sum for item drops invalid");
			}
		}
		
		/** 
		 * TODO: Implement custom probability distributions
		 * @return The item id and item quantity, respectively
		 */
		public int[] getItemDrop() {
			double rollingSum = 0.0;
			double chosenRandom = Math.random();
			for (ItemDrop itemDrop : itemDrops) {
				rollingSum += itemDrop.probability;
				if (chosenRandom < rollingSum) {
					int quantity = MathUti.discreteUniform(itemDrop.min, itemDrop.max);
					return new int[] {itemDrop.itemId, quantity};
				}
			}
			return null;
		}
	}
	
	public static class ItemDrop {
		public int itemId, min, max;
		public double probability;
		public char distribution = 0;
		
		public ItemDrop(int itemId) {
			this(itemId, 1, 1, 1.0);
		}
		public ItemDrop(int itemId, int num) {
			this(itemId, num, num, 1.0);
		}
		public ItemDrop(int itemId, int min, int max) {
			this(itemId, min, max, 1.0);
		}
		public ItemDrop(int itemId, int min, int max, double probability) {
			this.itemId = itemId;
			this.min = min;
			this.max = max;
			this.probability = probability;
		}
		
		public String toString() {
			return itemId + ", " + min + ", " + max + ", " + probability;
		}
	}
	
}
