package io.github.dantetam.world.worldgen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.github.dantetam.lwjglEngine.terrain.ForestGeneration.ProceduralTree;
import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.dataparse.ItemData;
import io.github.dantetam.world.grid.LocalBuilding;
import io.github.dantetam.world.grid.LocalGrid;
import io.github.dantetam.world.grid.LocalTile;

public class TreeVoxelGeneration {

	public static void generateSingle3dTree(LocalGrid grid, int[] coords, ProceduralTree proceduralTree) {
		int startingHeight = grid.findLowestEmptyHeight(coords[0], coords[1]);
		Vector3i startLocation = new Vector3i(coords[0], coords[1], startingHeight);
		
		List<Vector3i> treeLocations = new ArrayList<>();
		List<Integer> blockIds = new ArrayList<>();
		
		int woodId = ItemData.getIdFromName("Pine Log");
		int leavesId = ItemData.getIdFromName("Pine Needles");
		
		int treeHeight = Math.max((int) proceduralTree.size / 4, 5);
		for (int h = 0; h < treeHeight; h++) {
			treeLocations.add(new Vector3i(0,0,startingHeight + h));
			blockIds.add(woodId);
		}
		
		for (int h = 0; h <= treeHeight / 4.0; h++) {
			for (int r = -2; r <= 2; r++) {
				for (int c = -2; c <= 2; c++) {
					if (r == 0 && c == 0 && h == 0) continue;
					treeLocations.add(new Vector3i(r,c,h));
					blockIds.add(leavesId);
				}
			}
		}
		
		LocalBuilding treeBuilding = new LocalBuilding("Pine Tree", startLocation, treeLocations, blockIds);
		grid.addBuilding(treeBuilding, startLocation, true);
	}
	
}
