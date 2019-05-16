package io.github.dantetam.world.grid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.items.Inventory;
import io.github.dantetam.world.life.LivingEntity;

public class LocalBuilding {

	public int buildingId;
	public String name;
	
	private Vector3i primaryLocation;
	private List<Vector3i> locationOffsets; //Contains every location, including primary
	public Set<Vector3i> calculatedLocations; //Contains the absolute location of every part of this building
	public List<Integer> buildingBlockIds;
	
	public LivingEntity owner;
	public LivingEntity currentUser;
	
	public Inventory inventory;
	
	public LocalBuilding(int buildingId, String name, Vector3i primaryLoc, List<Vector3i> offsets, List<Integer> blockIds) {
		this.buildingId = buildingId;
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
	
	public Vector3i getPrimaryLocation() {
		return primaryLocation;
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
			calculatedLocations = new HashSet<>();
			for (Vector3i offset: offsets) {
				calculatedLocations.add(primaryLocation.getSum(offset));
			}
		}
	}
	
	public List<Vector3i> getLocationOffsets() {
		return locationOffsets;
	}
	
}
