package io.github.dantetam.world.dataparse;

import java.util.List;
import java.util.Set;

import io.github.dantetam.world.combat.CombatData;
import io.github.dantetam.world.combat.CombatMod;
import io.github.dantetam.world.items.InventoryItem;

public class CombatItem {
	
	public Set<String> bodyPartsCovered;
	
	public int itemWeight;
	
	public int combatItemId;
	public String name;
	
	public CombatItem(InventoryItem item) {
		combatItemId = item.itemId;
		name = item.name;
		itemWeight = 1;
		bodyPartsCovered = CombatData.getBodyPartsCover(combatItemId);
	}
	
}
