package io.github.dantetam.world.grid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.dantetam.vector.Vector3i;

public class LocalBuilding {

	public String name;
	
	private LocalTile primaryLocation;
	private List<Vector3i> locationOffsets; //Contains every location, including primary
	public List<Vector3i> calculatedLocations; //Contains the absolute location of every part of this building
	
	public LocalBuilding(String name, LocalTile tile) {
		this(name, tile, null);
	}
	
	public LocalBuilding(String name, LocalTile tile, List<Vector3i> offsets) {
		this.name = name;
		primaryLocation = tile;
		setLocationOffsets(offsets);
	}
	
	public void setPrimaryLocation(LocalTile primaryLocation) {
		this.primaryLocation = primaryLocation;
		setLocationOffsets(locationOffsets);
	}
	
	public void setLocationOffsets(List<Vector3i> offsets) {
		if (primaryLocation == null) {
			calculatedLocations = null;
		}
		else {
			if (offsets == null) {
				offsets = Collections.singletonList(new Vector3i(0));
			}
			locationOffsets = offsets;
			calculatedLocations = new ArrayList<>();
			for (Vector3i offset: offsets) {
				calculatedLocations.add(primaryLocation.coords.getSum(offset));
			}
		}
	}
	
	public List<Vector3i> getLocationOffsets() {
		return locationOffsets;
	}
	
}
