package io.github.dantetam.world.grid;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.civilization.Human;
import io.github.dantetam.world.civilization.LivingEntity;
import io.github.dantetam.world.dataparse.ItemData;
import io.github.dantetam.world.dataparse.WorldCsvParser;

/**
 * A grid representing one section of the whole world, i.e. a self contained world with its own people, tiles, and so on.
 * @author Dante
 *
 */

public class LocalGrid {

	public int rows, cols, heights; //Sizes of the three dimensions of this local grid
	private LocalTile[][][] grid; //A 3D grid, with the third dimension representing height, such that [][][50] is one level higher than [][][49].
	
	public LocalGrid(Vector3i size) {
		rows = size.x; cols = size.y; heights = size.z;
		grid = new LocalTile[rows][cols][heights];
	}
	
	/**
	 * @return The tile at coords (r,c,h), null if the coord is out of bounds.
	 */
	public LocalTile getTile(Vector3i coords) {
		int r = coords.x, c = coords.y, h = coords.z;
		if (r < 0 || c < 0 || h < 0 || r >= rows || c >= cols || h >= heights)
			return null;
		return grid[r][c][h];
	}
	
	/**
	 * Used only for world creation, to create individual tiles
	 * @param coords
	 * @param tile
	 */
	public void setTileInstantiate(Vector3i coords, LocalTile tile) {
		int r = coords.x, c = coords.y, h = coords.z;
		if (r < 0 || c < 0 || h < 0 || r >= rows || c >= cols || h >= heights)
			throw new IllegalArgumentException("Invalid tile added to coords: " + coords.toString());
		grid[r][c][h] = tile;
	}
	
	public boolean buildingCanFitAt(LocalBuilding building, Vector3i newPrimaryCoords) {
		//Check to see if the building fits
		List<Vector3i> offsets = building.getLocationOffsets();
		for (Vector3i offset: offsets) {
			Vector3i candidate = newPrimaryCoords.getSum(offset);
			LocalTile candidateTile = this.getTile(candidate);
			if (candidateTile == null || candidateTile.building != null) {
				return false;
			}
		}
		return true;
	}
	
	public void addBuilding(LocalBuilding building, Vector3i newPrimaryCoords, boolean overrideGrid) {
		if (buildingCanFitAt(building, newPrimaryCoords) || overrideGrid) {
			removeBuilding(building);
			building.setPrimaryLocation(newPrimaryCoords);
			List<Vector3i> newAbsLocations = building.calculatedLocations;
			for (int buildingTileIndex = 0; buildingTileIndex < newAbsLocations.size(); buildingTileIndex++) {
				Vector3i newAbsLocation = newAbsLocations.get(buildingTileIndex);
				LocalTile absTile = getTile(newAbsLocation);
				if (absTile == null) {
					LocalTile newTile = new LocalTile(newAbsLocation);
					grid[newAbsLocation.x][newAbsLocation.y][newAbsLocation.z] = newTile;
					absTile = newTile;
				}
				absTile.building = building;
				absTile.tileBlockId = building.buildingBlockIds.get(buildingTileIndex);
			}
		}
	}
	
	public void removeBuilding(LocalBuilding building) {
		List<Vector3i> oldAbsLocations = building.calculatedLocations;
		for (Vector3i oldAbsLocation: oldAbsLocations) {
			LocalTile oldTile = getTile(oldAbsLocation);
			if (oldTile != null) {
				getTile(oldAbsLocation).building = null;
				getTile(oldAbsLocation).tileBlockId = ItemData.ITEM_EMPTY_ID;
			}
		}
		building.setPrimaryLocation(null);
	}
	
	public void addHuman(Human human, Vector3i coords) {
		LocalTile tile = getTile(coords);
		if (tile != null) {
			human.location = tile;
		}
	}
	
	public boolean personHasAccessTile(LivingEntity person, LocalTile tile) {
		if (tile == null) {
			return false;
		}
		return true;
	}
	
	public void movePerson(LivingEntity person, LocalTile tile) {
		if (person.location != null) {
			person.location.removePerson(person);
		}
		person.location = tile;
		if (person.location != null) {
			person.location.addPerson(person);
		}
	}
	
	public int findLowestEmptyHeight(int r, int c) {
		for (int h = heights - 1; h >= 0; h--) {
			if (getTile(new Vector3i(r, c, h)) == null) {
				continue;
			}
			else {
				return h+1;
			}
		}
		return 0;
	}
	
	public void claimTile(Human human, Vector3i coords, boolean override) {
		LocalTile tile = getTile(coords);
		if (tile != null) {
			if (tile.humanClaim == null || override) {
				unclaimTile(coords);
				tile.humanClaim = human;
				human.allClaims.add(coords);
			}
		}
	}
	
	public void unclaimTile(Vector3i coords) {
		LocalTile tile = getTile(coords);
		if (tile != null) {
			if (tile.humanClaim != null) {
				tile.humanClaim.allClaims.remove(coords);
				tile.humanClaim = null;
			}
		}
	}
	
	public static void main(String[] args) {
		WorldCsvParser.init();
		
		LocalGrid grid = new LocalGrid(new Vector3i(200,200,50));
		List<Vector3i> listOffsets = Arrays.asList(new Vector3i[] {
				new Vector3i(0),
				new Vector3i(1,0,10),
				new Vector3i(0,0,10)
		});
		List<Integer> listBlockIds = Arrays.asList(new Integer[] {
				ItemData.getIdFromName("Wooden Artisan"),
				ItemData.getIdFromName("Brick Wall"),
				ItemData.getIdFromName("Anvil")
				});
		LocalBuilding building = new LocalBuilding("Test1", new Vector3i(25, 25, 35), listOffsets, listBlockIds);
		
		grid.addBuilding(building, new Vector3i(25,25,35), true);
		
		System.out.println(Arrays.toString(building.calculatedLocations.toArray())); //[25 25 25, 26 25 25, 25 28 25]
		System.out.println(grid.getTile(new Vector3i(26,25,45))); //This tile should have been created
		System.out.println(grid.getTile(new Vector3i(25,25,45)).building.name); //Test1
		
		grid.removeBuilding(building);
		
		System.out.println(grid.getTile(new Vector3i(26,25,45)).building); //null
	}
	
}
