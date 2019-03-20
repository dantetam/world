package io.github.dantetam.world.grid;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.github.dantetam.vector.Vector3i;

/**
 * A grid representing one section of the whole world, i.e. a self contained world with its own people, tiles, and so on.
 * @author Dante
 *
 */

public class LocalGrid {

	public int rows, cols, heights; //Sizes of the three dimensions of this local grid
	private LocalTile[][][] grid; 
	
	public LocalGrid(Vector3i size) {
		rows = size.x; cols = size.y; heights = size.z;
		grid = new LocalTile[rows][cols][heights];
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				for (int h = 0; h < heights; h++) {
					grid[r][c][h] = new LocalTile(new Vector3i(r,c,h));
				}
			}
		}
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
	
	public void addBuilding(LocalBuilding building, Vector3i newPrimaryCoords) {
		if (buildingCanFitAt(building, newPrimaryCoords)) {
			removeBuilding(building);
			building.setPrimaryLocation(getTile(newPrimaryCoords));
			List<Vector3i> newAbsLocations = building.calculatedLocations;
			for (Vector3i newAbsLocation: newAbsLocations) {
				getTile(newAbsLocation).building = building;
			}
		}
	}
	
	public void removeBuilding(LocalBuilding building) {
		List<Vector3i> oldAbsLocations = building.calculatedLocations;
		for (Vector3i oldAbsLocation: oldAbsLocations) {
			getTile(oldAbsLocation).building = null;
		}
		building.setPrimaryLocation(null);
	}
	
	public boolean personHasAccessTile(Person person, LocalTile tile) {
		if (tile == null) {
			return false;
		}
		return true;
	}
	
	public void movePerson(Person person, LocalTile tile) {
		if (person.location != null) {
			person.location.removePerson(person);
		}
		person.location = tile;
		if (person.location != null) {
			person.location.addPerson(person);
		}
	}
	
	public static void main(String[] args) {
		LocalGrid grid = new LocalGrid(new Vector3i(200,200,50));
		List<Vector3i> listOffsets = Arrays.asList(new Vector3i[] {
				new Vector3i(0),
				new Vector3i(1,0,0),
				new Vector3i(0,3,0)
		});
		LocalTile primaryTile = grid.getTile(new Vector3i(0));
		LocalBuilding building = new LocalBuilding("Test1", primaryTile, listOffsets);
		grid.addBuilding(building, new Vector3i(25,25,25));
		
		System.out.println(Arrays.toString(building.calculatedLocations.toArray())); //[25 25 25, 26 25 25, 25 28 25]
		System.out.println(grid.getTile(new Vector3i(25,28,25)).building.name); //Test1
		
		grid.removeBuilding(building);
		
		System.out.println(grid.getTile(new Vector3i(25,28,25)).building); //null
	}
	
}