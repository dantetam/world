package io.github.dantetam.world.civilization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.dataparse.ItemData;
import io.github.dantetam.world.dataparse.ItemTotalDrops;
import io.github.dantetam.world.grid.LocalGrid;
import io.github.dantetam.world.grid.LocalTile;

public class Society {

	public LocalGrid grid;
	private List<Human> inhabitants;
	
	public Society(LocalGrid grid) {
		this.grid = grid;
		inhabitants = new ArrayList<>();
	}
	
	private Map<Integer, Double> findRawResourcesRarity() {
		Map<Integer, Double> itemRarity = new HashMap<>();
		for (int r = 0; r < grid.rows; r++) {
			for (int c = 0; c < grid.cols; c++) {
				int startHeight = grid.findLowestEmptyHeight(r, c) - 1;
				for (int h = startHeight; h >= 0; h--) {
					LocalTile tile = grid.getTile(new Vector3i(r, c, h));
					if (tile.tileBlockId != ItemData.ITEM_EMPTY_ID) {
						ItemTotalDrops drops = ItemData.getOnBlockItemDrops(tile.tileBlockId);
						Map<Integer, Double> itemExpectations = drops.itemExpectation();
						for (Entry<Integer, Double> entry: itemExpectations.entrySet()) {
							double prevEntry = itemRarity.containsKey(entry.getKey()) ? itemRarity.get(entry.getKey()) : 0;
							itemRarity.put(entry.getKey(), prevEntry + entry.getValue());
						}
					}
				}
			}
		}
		return itemRarity;
	}
	
	private Map<Integer, Double> findAllAvailableResourceRarity() {
		Set<Integer> visitedItemIds = new HashSet<>();
		Map<Integer, Double> rawResources = findRawResourcesRarity();
		int previousSize = 0; //Expand all of the item process chains until this map does not change size
		while (rawResources.size() != previousSize) {
			previousSize = rawResources.size();
			
		}
	}
	
}
