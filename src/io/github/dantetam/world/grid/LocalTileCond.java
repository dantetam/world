package io.github.dantetam.world.grid;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.civilization.Society;
import io.github.dantetam.world.dataparse.ItemData;
import io.github.dantetam.world.life.Human;

//Represent a logical boolean function (a predicate) in the form of f(Tile) : Tile -> Boolean
//i.e. a filter or selection on all tiles

public abstract class LocalTileCond {

	public abstract boolean isDesiredTile(LocalGrid grid, Vector3i coords);
	
	
	public static Set<Vector3i> filter(Set<Vector3i> vecs, LocalGrid grid, LocalTileCond cond) {
		if (cond == null) {
			System.err.println("Warning, null condition 'cond' given to LocalTileCond::filter(...)");
			return vecs;
		}
		
		Set<Vector3i> results = new HashSet<>();
		for (Vector3i vec: vecs) {
			if (cond.isDesiredTile(grid, vec)) {
				results.add(vec);
			}
		}
		return results;
	}
	
	public static class IsBuildingTileCond extends LocalTileCond {
		@Override
		public boolean isDesiredTile(LocalGrid grid, Vector3i coords) {
			LocalTile belowTile = grid.getTile(coords.getSum(0, -1, 0));
			return grid.tileIsAccessible(coords) && 
					ItemData.getGroupIds("BuildingMaterial").contains(belowTile.tileBlockId);
		}
	}
	
	public static class IsValidLandOwnerSoc extends LocalTileCond {
		private Society society;
		private Set<Human> validLandOwners;
		public IsValidLandOwnerSoc(Society society, Set<Human> validLandOwners) {
			this.society = society;
			this.validLandOwners = validLandOwners;
		}
		
		@Override
		public boolean isDesiredTile(LocalGrid grid, Vector3i coords) {
			// TODO Auto-generated method stub
			Human tileOwner = society.getRecordClaimInGrid(grid, coords);
			return this.validLandOwners.contains(tileOwner);
		}
	}
	
	public static class IsWithinIntervalsStrict extends LocalTileCond {
		private Collection<ClusterVector3i> validAreas;
		public IsWithinIntervalsStrict(Collection<ClusterVector3i> validAreas) {
			this.validAreas = validAreas;
		}
		
		@Override
		public boolean isDesiredTile(LocalGrid grid, Vector3i coords) {
			// TODO Auto-generated method stub
			for (ClusterVector3i cluster: validAreas) {
				if (cluster.clusterData.contains(coords)) {
					return true;
				}
			}
			return false;
		}
	}
	
}
