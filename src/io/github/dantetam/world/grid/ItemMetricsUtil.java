package io.github.dantetam.world.grid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import io.github.dantetam.toolbox.MapUtil;
import io.github.dantetam.toolbox.MathUti;
import io.github.dantetam.world.dataparse.ItemData;
import io.github.dantetam.world.items.InventoryItem;
import io.github.dantetam.world.life.LivingEntity;
import io.github.dantetam.world.process.LocalProcess;
import io.github.dantetam.world.process.LocalProcess.ProcessStep;
import kdtreegeo.KdTree;

public class ItemMetricsUtil {

	public static class DefaultItemMetric {
		public double score(LivingEntity being, Set<LivingEntity> owners, LocalGrid grid, LocalTile tile, 
				Map<Integer, Integer> itemsNeeded, Map<String, Integer> itemGroupsAmtNeeded) {
			if (itemsNeeded == null) {
				itemsNeeded = new HashMap<>();
			}
			if (itemGroupsAmtNeeded == null) {
				itemGroupsAmtNeeded = new HashMap<>();
			}
			
			Set<Integer> itemsNeededIds = new HashSet<>(itemsNeeded.values());
			Map<Integer, Integer> itemAmtNeeded = new HashMap<>();
			
			for (Entry<Integer, Integer> entry: itemsNeeded.entrySet()) {
				MapUtil.insertKeepMaxMap(itemAmtNeeded, entry.getKey(), entry.getValue());
			}
			for (Entry<String, Integer> entry: itemGroupsAmtNeeded.entrySet()) {
				Set<Integer> groupIds = ItemData.getIdsFromNameOrGroup(entry.getKey());
				for (Integer groupId: groupIds) {
					itemsNeededIds.add(groupId);
					MapUtil.insertKeepMaxMap(itemAmtNeeded, groupId, entry.getValue());
				}
			}
			
			double utility = 0;
			for (Integer itemId: itemsNeededIds) {
				int tileInvCount = tile.itemsOnFloor.findItemCount(itemId, being, owners);
				if (tile.building != null)
					tileInvCount += tile.building.inventory.findItemCount(itemId, being, owners);
				
				int effCount = Math.min(tileInvCount, itemAmtNeeded.get(itemId)); 
				utility += effCount;
			}
			
			double dist = being.location.coords.squareDist(tile.coords);
			return utility - dist;
		}
	}
	
	//TODO; //Use and impl. in LocalGridTimeExecution when deconst. process "Put On Armor" (or in combat?)
	//TODO; //Use armor metrics in combat data/combat engine execution
	public static class AvailFitArmorMetric extends DefaultItemMetric {

		@Override
		public double score(LivingEntity being, Set<LivingEntity> owners, LocalGrid grid, LocalTile tile, 
				Map<Integer, Integer> itemsNeeded, Map<String, Integer> itemGroupsNeeded) {
			Map<String, Integer> armorMapping = new HashMap<>();
			armorMapping.put("Armor", 1);
			double baseScore = super.score(being, owners, grid, tile, itemsNeeded, armorMapping);
			
			boolean foundWearableItem = false;
			List<InventoryItem> items = tile.itemsOnFloor.getItems();
			for (InventoryItem item: items) {
				if (being.body.canWearClothes(item)) {
					foundWearableItem = true;
					break;
				}
			}
			if (!foundWearableItem) {
				baseScore = 0;
			}
			
			return baseScore;
		}
	}
	
	//TODO; //Use and impl. in LocalGridTimeExecution when deconst. process "Put On Armor" (or in combat?)
	public static class AvailClothingMetric extends DefaultItemMetric {

		@Override
		public double score(LivingEntity being, Set<LivingEntity> owners, LocalGrid grid, LocalTile tile, 
				Map<Integer, Integer> itemsNeeded, Map<String, Integer> itemGroupsNeeded) {
			Map<String, Integer> armorMapping = new HashMap<>();
			armorMapping.put("Clothing", 1);
			double baseScore = super.score(being, owners, grid, tile, itemsNeeded, armorMapping);

			//Use metrics of clothing utility, including wealth, 
			//i.e. find clothing which matches the wealth of a person
			double humanWealth = being.getTotalPowerPrestige(); 
			
			//Fit log-quadratic, 500 -> 0.2, 50000 -> 0.02, beyond 50000 -> 0.2
			double desiredTotalWealthClothing = 0.02 + 0.18 * (1 - Math.pow(Math.log10(humanWealth), 2) / 25);
			desiredTotalWealthClothing = MathUti.clamp(desiredTotalWealthClothing, 0.02, 0.2);
			
			boolean foundWearableItem = false;
			List<InventoryItem> items = tile.itemsOnFloor.getItems();
			for (InventoryItem item: items) {
				if (being.body.canWearClothes(item)) {
					foundWearableItem = true;
					
					double wealthItem = ItemData.getBaseItemValue(item.itemId);
					double wealthDifferential = Math.abs(Math.log10(wealthItem) - Math.log10(desiredTotalWealthClothing)); 
					
					baseScore -= wealthDifferential;
					
					break;
				}
			}
			if (!foundWearableItem) {
				baseScore = 0;
			}
			
			return baseScore;
		}
	}
	
	public static class GroupAndUtilMetric extends DefaultItemMetric {
		public String[] itemGroups;
		public String useUtilityTypePref;
		public GroupAndUtilMetric(String useUtil, String[] groups) {
			this.useUtilityTypePref = useUtil;
			this.itemGroups = groups;
		}
		
		@Override
		public double score(LivingEntity being, Set<LivingEntity> owners, LocalGrid grid, LocalTile tile, 
				Map<Integer, Integer> itemsNeeded, Map<String, Integer> itemGroupsNeeded) {
			Map<String, Integer> armorMapping = new HashMap<>();
			for (String itemGroup: itemGroups) {
				armorMapping.put(itemGroup, 1);
			}
			double baseScore = super.score(being, owners, grid, tile, itemsNeeded, armorMapping);
			
			double maxFoodUtil = 0;
			List<InventoryItem> items = tile.itemsOnFloor.getItems();
			for (InventoryItem item: items) {
				double amt = ItemData.confirmItemActions(item.itemId, useUtilityTypePref);
				if (amt > maxFoodUtil && amt > 0) {
					maxFoodUtil = amt;
				}
			}
			
			return baseScore * maxFoodUtil;
		}
	}
	
	public static class FoodMetric extends GroupAndUtilMetric {
		public FoodMetric() {
			super("Eat", new String[] {"Food"});
		}
	}
	
	public static class BedRestMetric extends GroupAndUtilMetric {
		public BedRestMetric() {
			super("Rest", new String[] {"Bed"});
		}
	}
	
}
