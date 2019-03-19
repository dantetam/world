package io.github.dantetam.world.grid;

import io.github.dantetam.vector.Vector3i;

public class LocalTile {

	public Vector3i coords;
	
	public LocalBuilding building;
	
	public LocalTile(Vector3i coords) {
		this.coords = coords;
	}
	
}
