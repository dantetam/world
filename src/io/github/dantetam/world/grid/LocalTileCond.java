package io.github.dantetam.world.grid;

import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.dataparse.ItemData;

//Represent a logical boolean function (a predicate) in the form of f(Tile) : Tile -> Boolean
//i.e. a filter or selection on all tiles

public abstract class LocalTileCond {

	public abstract boolean isDesiredTile(LocalGrid grid, Vector3i coords);
	
	
	public static class IsBuildingTileCond extends LocalTileCond {
		@Override
		public boolean isDesiredTile(LocalGrid grid, Vector3i coords) {
			LocalTile belowTile = grid.getTile(coords.getSum(0, -1, 0));
			return grid.tileIsAccessible(coords) && 
					ItemData.getGroupIds("BuildingMaterial").contains(belowTile.tileBlockId);
		}
	}
	
}
