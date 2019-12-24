package io.github.dantetam.world.grid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import io.github.dantetam.toolbox.MapUtil;
import io.github.dantetam.toolbox.MathAndDistrUti;
import io.github.dantetam.world.dataparse.ItemData;
import io.github.dantetam.world.items.GroupItem;
import io.github.dantetam.world.items.Inventory;
import io.github.dantetam.world.items.InventoryItem;
import io.github.dantetam.world.items.InventoryItem.ItemQuality;
import io.github.dantetam.world.life.LivingEntity;
import io.github.dantetam.world.process.LocalProcess;
import io.github.dantetam.world.process.LocalProcess.ProcessStep;
import kdtreegeo.KdTree;

public class ItemMetricsUtil {

	private static Map<ItemQuality, Double> qualModMap = new HashMap<ItemQuality, Double>() {{
		put(ItemQuality.TERRIBLE, 0.3);
		put(ItemQuality.NORMAL, 0.7);
		put(ItemQuality.GOOD, 1.0);
		put(ItemQuality.GREAT, 1.25);
		put(ItemQuality.LEGENDARY, 1.5);
	}};
	/**
	 * Custom method for searching for specific item id and qualities for multiple inventories
	 * @param invs
	 * @param itemId
	 * @param mainHuman
	 * @param otherOwners
	 * @param desiredCount
	 * @return a score denoting the maximum item score, with optimal (highest quality and quantity) usage of items
	 */
	public static double scoreItemsQualMetric(List<Inventory> invs, 
			int itemId, LivingEntity mainHuman, Set<LivingEntity> otherOwners,
			int desiredCount) {
		List<InventoryItem> allItems = new ArrayList<>();
		for (Inventory inv: invs) {
			allItems.addAll(inv.findItems(itemId, mainHuman, otherOwners));
		}
		Collections.sort(allItems, new Comparator<InventoryItem>() {
			@Override
			public int compare(InventoryItem o1, InventoryItem o2) {
				int q1 = ItemQuality.getIndex(o1.quality), q2 = ItemQuality.getIndex(o2.quality);
				return q2 - q1;
			}
		});
		double score = 0;
		while (desiredCount >= 0 && allItems.size() > 0) {
			InventoryItem item = allItems.remove(0);
			int usedAmount = (int) Math.min(item.quantity, desiredCount);
			desiredCount -= usedAmount;
			double qualMod = qualModMap.get(item.quality);
			score += usedAmount * qualMod;
		}
		return score;
	}
	
	public static class DefaultItemMetric {
		public double score(LivingEntity being, Set<LivingEntity> owners, LocalGrid grid, LocalTile tile, 
				Map<Integer, Integer> itemsNeeded, GroupItem groupNeed) {
			List<GroupItem> itemGroupsAmtNeeded = new ArrayList<>();
			itemGroupsAmtNeeded.add(groupNeed);
			return this.score(being, owners, grid, tile, itemsNeeded, itemGroupsAmtNeeded);
		}
		public double score(LivingEntity being, Set<LivingEntity> owners, LocalGrid grid, LocalTile tile, 
				Map<Integer, Integer> itemsNeeded, List<GroupItem> itemGroupsAmtNeeded) {
			if (itemsNeeded == null) {
				itemsNeeded = new HashMap<>();
			}
			if (itemGroupsAmtNeeded == null) {
				itemGroupsAmtNeeded = new ArrayList<>();
			}
			
			Set<Integer> itemsNeededIds = new HashSet<>(itemsNeeded.values());
			Map<Integer, Integer> itemAmtNeeded = new HashMap<>();
			
			for (Entry<Integer, Integer> entry: itemsNeeded.entrySet()) {
				MapUtil.insertKeepMaxMap(itemAmtNeeded, entry.getKey(), entry.getValue());
			}
			for (GroupItem entry: itemGroupsAmtNeeded) {
				Set<Integer> groupIds = ItemData.getIdsFromNameOrGroup(entry.group);
				for (Integer groupId: groupIds) {
					itemsNeededIds.add(groupId);
					MapUtil.insertKeepMaxMap(itemAmtNeeded, groupId, entry.desiredCount);
				}
			}
			
			double utility = 0;
			for (Integer itemId: itemsNeededIds) {
				//Factor in item quality into the score
				List<Inventory> invs = new ArrayList<>();
				invs.add(tile.itemsOnFloor); 
				if (tile.building != null)
					invs.add(tile.building.inventory);
				
				int desiredCount = itemAmtNeeded.get(itemId);
				double invScore = ItemMetricsUtil.scoreItemsQualMetric(invs, itemId, being, owners, desiredCount);
				
				utility += invScore;
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
				Map<Integer, Integer> itemsNeeded, List<GroupItem> itemGroupsNeeded) {
			GroupItem armorMapping = new GroupItem("Armor", ItemQuality.TERRIBLE, 1);
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
				Map<Integer, Integer> itemsNeeded, List<GroupItem> itemGroupsNeeded) {
			GroupItem armorMapping = new GroupItem("Clothing", ItemQuality.TERRIBLE, 1);
			double baseScore = super.score(being, owners, grid, tile, itemsNeeded, armorMapping);

			//Use metrics of clothing utility, including wealth, 
			//i.e. find clothing which matches the wealth of a person
			double humanWealth = being.getTotalPowerPrestige(); 
			
			//Fit log-quadratic, 500 -> 0.2, 50000 -> 0.02, beyond 50000 -> 0.2
			double desiredTotalWealthClothing = 0.02 + 0.18 * (1 - Math.pow(Math.log10(humanWealth), 2) / 25);
			desiredTotalWealthClothing = MathAndDistrUti.clamp(desiredTotalWealthClothing, 0.02, 0.2);
			
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
				Map<Integer, Integer> itemsNeeded, List<GroupItem> itemGroupsNeeded) {
			double baseScore = super.score(being, owners, grid, tile, itemsNeeded, itemGroupsNeeded);
			
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
