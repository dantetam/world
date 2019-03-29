package io.github.dantetam.world.worldgen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.github.dantetam.lwjglEngine.terrain.ForestGeneration.ProceduralTree;
import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.grid.LocalBuilding;
import io.github.dantetam.world.grid.LocalGrid;
import io.github.dantetam.world.grid.LocalTile;

public class TreeVoxelGeneration {

	public static void generateSingle3dTree(LocalGrid grid, int[] coords, ProceduralTree proceduralTree) {
		int startingHeight = grid.findHighestEmptyHeight(coords[0], coords[1]);
		LocalTile startTile = grid.getTile(new Vector3i(coords[0], coords[1], startingHeight));
		
		int treeHeight = Math.max((int) proceduralTree.size / 4, 5);
		List<Vector3i> treeLocations = new ArrayList<>();
		for (int h = 0; h < treeHeight; h++) {
			treeLocations.add(new Vector3i(0,0,startingHeight + h));
		}
		
		for (int r = -2; r <= 2; r++) {
			for (int c = -2; c <= 2; c++) {
				//if ()
			}
		}
		
		//LocalBuilding treeBuilding = new LocalBuilding("Tree", startTile, treeLocations);
	}
	
}
