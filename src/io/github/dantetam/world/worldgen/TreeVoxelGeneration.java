package io.github.dantetam.world.worldgen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.github.dantetam.lwjglEngine.terrain.ForestGeneration.ProceduralTree;
import io.github.dantetam.vector.Vector2i;
import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.dataparse.ItemData;
import io.github.dantetam.world.grid.LocalBuilding;
import io.github.dantetam.world.grid.LocalGrid;
import io.github.dantetam.world.grid.LocalTile;

public class TreeVoxelGeneration {

	public static void generateSingle3dTree(LocalGrid grid, Vector2i coordsNoHeight, ProceduralTree proceduralTree) {
		Vector3i startLocation = grid.findLowestAccessibleHeight(coordsNoHeight.x, coordsNoHeight.y);
		if (startLocation == null) return;
		
		List<Vector3i> treeLocations = new ArrayList<>();
		List<Integer> blockIds = new ArrayList<>();
		
		int woodId = ItemData.getIdFromName("Pine Log");
		int leavesId = ItemData.getIdFromName("Pine Needles");
		
		int treeHeight = Math.max((int) proceduralTree.size / 4, 10);
		for (int h = 0; h < treeHeight; h++) {
			treeLocations.add(new Vector3i(0,0,h));
			blockIds.add(woodId);
		}
		
		for (int h = 3; h <= treeHeight; h++) {
			for (int r = -2; r <= 2; r++) {
				for (int c = -2; c <= 2; c++) {
					if (r == 0 && c == 0 && h < treeHeight / 2) continue;
					treeLocations.add(new Vector3i(r,c,h));
					blockIds.add(leavesId);
				}
			}
		}
		
		LocalBuilding treeBuilding = new LocalBuilding(ItemData.getIdFromName("Pine Tree"), 
				"Pine Tree", startLocation, treeLocations, blockIds);
		grid.addBuilding(treeBuilding, startLocation, true, null);
	}
	
}
