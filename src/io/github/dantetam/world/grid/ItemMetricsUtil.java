package io.github.dantetam.world.grid;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.dataparse.ItemData;
import io.github.dantetam.world.dataparse.ProcessData;
import io.github.dantetam.world.items.InventoryItem;
import io.github.dantetam.world.life.LivingEntity;
import io.github.dantetam.world.process.LocalProcess;
import kdtreegeo.KdTree;

public class ItemMetricsUtil {

	public static class DefaultItemMetric {
		public double score(LivingEntity being, LocalGrid grid, LocalTile tile, 
				Map<InventoryItem, Integer> itemsNeeded, Map<String, Integer> itemGroupsNeeded) {{
			Set<Integer> groupIds = ItemData.getGroupIds(itemGroupName);
			Vector3i bestLocation = null;
			double bestScore = 0;
			for (int groupId: groupIds) {
				List<LocalProcess> outputItemProcesses = ProcessData.getProcessesByOutput(groupId);
				for (LocalProcess process: outputItemProcesses) {
					if (process.name.startsWith("Harvest ")) {
						int inputTileId = ItemData.getIdFromName(process.requiredTileNameOrGroup);
						double pickupTime = ItemData.getPickupTime(inputTileId);
						double expectedNumItem = process.outputItems.itemExpectation().get(inputTileId);
						
						KdTree<Vector3i> itemTree = grid.getKdTreeForTile(inputTileId);
						int numCandidates = Math.min(itemTree.size(), 10);
						Collection<Vector3i> nearestCoords = itemTree.nearestNeighbourListSearch(
								numCandidates, being.location.coords);
						for (Vector3i itemCoords: nearestCoords) {
							int distUtil = being.location.coords.manhattanDist(itemCoords);
							if (bestLocation == null || distUtil < bestScore) {
								bestLocation = itemCoords;
								bestScore = distUtil * expectedNumItem / pickupTime;
							}
						}
					}
				}
			}
		}
	}
	
	public static class AvailFitArmorMetric extends DefaultItemMetric {

		TODO
		@Override
		public double score(LivingEntity being, LocalGrid grid, LocalTile tile, 
				Map<InventoryItem, Integer> itemsNeeded, Map<String, Integer> itemGroupsNeeded) {
			
		}
		
	}
	
}
