package io.github.dantetam.world.dataparse;

import java.util.ArrayList;
import java.util.List;

import io.github.dantetam.toolbox.CustomMathUtil;

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
		 * @return The item id and item quantity, respectively
		 */
		public int[] getItemDrop() {
			double rollingSum = 0.0;
			double chosenRandom = Math.random();
			for (ItemDrop itemDrop : itemDrops) {
				rollingSum += itemDrop.probability;
				if (chosenRandom < rollingSum) {
					int quantity = CustomMathUtil.discreteUniform(itemDrop.min, itemDrop.max);
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
