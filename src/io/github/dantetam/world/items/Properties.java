package io.github.dantetam.world.items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.dantetam.toolbox.MapUtil;

public class Properties {
	
	private Map<String, List<ItemProperty>> itemSpecProperties;
	private List<ItemProperty> allProperties; //For storing all properties for reference
	
	public Properties() {
		this.itemSpecProperties = new HashMap<>();
		this.allProperties = new ArrayList<>();
	}
	
	public void addProperty(ItemProperty prop) {
		MapUtil.insertNestedListMap(this.itemSpecProperties, prop.propertyType, prop);
		this.allProperties.add(prop);
	}
	
	public void removeProperty(ItemProperty prop) {
		MapUtil.removeSafeNestListMap(this.itemSpecProperties, prop.propertyType, prop);
		this.allProperties.remove(prop);
	}
	
	public boolean hasProperty(String propType) {
		return this.itemSpecProperties.containsKey(propType);
	}
	
	public List<ItemProperty> getPropByName(String propType) {
		if (this.hasProperty(propType)) {
			return this.itemSpecProperties.get(propType);
		}
		return null;
	}
	
	public List<ItemProperty> getAllProperties() {
		return allProperties;
	}
	
}
