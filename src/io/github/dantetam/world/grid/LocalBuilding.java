package io.github.dantetam.world.grid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.items.Inventory;

public class LocalBuilding {

	public String name;
	
	private Vector3i primaryLocation;
	private List<Vector3i> locationOffsets; //Contains every location, including primary
	public List<Vector3i> calculatedLocations; //Contains the absolute location of every part of this building
	public List<Integer> buildingBlockIds;
	
	public Inventory inventory;
	
	public LocalBuilding(String name, Vector3i primaryLoc, List<Vector3i> offsets, List<Integer> blockIds) {
		this.name = name;
		primaryLocation = primaryLoc;
		if (offsets.size() != blockIds.size()) {
			throw new IllegalArgumentException("When creating building, offset list and block id list"
					+ "must have same size");
		}
		buildingBlockIds = blockIds;
		inventory = new Inventory();
		setLocationOffsets(offsets);
	}
	
	public void setPrimaryLocation(Vector3i primaryLocation) {
		this.primaryLocation = primaryLocation;
		setLocationOffsets(locationOffsets);
	}
	
	public void setLocationOffsets(List<Vector3i> offsets) {
		if (offsets == null) {
			offsets = Collections.singletonList(new Vector3i(0));
		}
		locationOffsets = offsets;
		
		if (primaryLocation == null) {
			calculatedLocations = null;
		}
		else {
			calculatedLocations = new ArrayList<>();
			for (Vector3i offset: offsets) {
				calculatedLocations.add(primaryLocation.getSum(offset));
			}
		}
	}
	
	public List<Vector3i> getLocationOffsets() {
		return locationOffsets;
	}
	
}
