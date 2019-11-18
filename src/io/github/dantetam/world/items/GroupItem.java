package io.github.dantetam.world.items;

import io.github.dantetam.world.items.InventoryItem.ItemQuality;

public class GroupItem { //A representation of a desire for some items within a group
	public String group;
	public ItemQuality qualityMin;
	public int desiredCount;
	public GroupItem(String group) {
		this(group, null, 0);
	}
	public GroupItem(String group, ItemQuality qualityMin, int desiredCount) {
		this.group = group;
		this.qualityMin = qualityMin;
		this.desiredCount = desiredCount;
	}
	public GroupItem clone() {
		return new GroupItem(this.group, this.qualityMin, this.desiredCount);
	}
	public String toString() {
		return "GroupNeed: " + group + "; " + this.qualityMin + "; " + desiredCount;
	}
}