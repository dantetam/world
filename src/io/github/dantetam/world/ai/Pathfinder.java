package io.github.dantetam.world.ai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import io.github.dantetam.world.civilization.LivingEntity;
import io.github.dantetam.world.dataparse.ItemData;
import io.github.dantetam.world.dataparse.ProcessData;
import io.github.dantetam.world.grid.LocalGrid;
import io.github.dantetam.world.grid.LocalTile;

/**
 * Created by Dante on 6/23/2016.
 *
 * This is an implementation of A* which relies on the Traversable interface.
 * A* finds a best path (a,b) between two points. It searches through candidates v
 * by the lowest sum of current distance (a -> v) and a heuristic distance (v -> b).
 */
public class Pathfinder {
	
	/*
	public static List<Tile> findPath(GameEntity en, Tile start, Tile end) {
		if (start.grid != end.grid) {
			throw new IllegalArgumentException("Start and end tiles for pathfinding not from same grid");
		}
		List<Tile> results = new Pathfinder<Tile>().findPath(new Object[] {en}, start, end);
		return results;
	}
	*/
	
	private static Set<LocalTile> validNeighbors(LocalGrid grid, LivingEntity being, LocalTile tile) {
		return grid.getAccessibleNeighbors(tile, being);
	}
	
	private static double getTileDist(LocalTile a, LocalTile b) {
		return a.coords.dist(b.coords);
	}
	
	private static int getTileAccessibilityPenalty(LocalTile tile) {
		if (tile == null) return 0;

		if (tile.tileBlockId != ItemData.ITEM_EMPTY_ID) {
			String blockName = ItemData.getNameFromId(tile.tileBlockId);
			int harvestTime = ProcessData.getProcessByName("Harvest Tile " + blockName).totalTime();
			return harvestTime;
		}

		if (tile.building != null) {
			if (tile.building.calculatedLocations != null) {
				if (tile.building.calculatedLocations.contains(tile.coords)) {
					return 1000;
				}
			}
		}
		return 0;
	}
	
    public static ScoredPath findPath(LocalGrid grid, LivingEntity being, LocalTile start, LocalTile end) {
        List<LocalTile> results = new ArrayList<>();
        if (start.equals(end)) {
            results.add(end);
            return new ScoredPath(results, 0);
        }
        Set<LocalTile> visited = new HashSet<>();
        final HashMap<LocalTile, Double> dist = new HashMap<>();
        Map<LocalTile, LocalTile> prev = new HashMap<>();
        PriorityQueue<LocalTile> fringe;

        fringe = new PriorityQueue<LocalTile>(16, new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
            	LocalTile n1 = (LocalTile) o1;
            	LocalTile n2 = (LocalTile) o2;
                if (n1.equals(n2)) return 0;
                //return (int)((dist.get(n1) - dist.get(n2) + end.dist(n1) - end.dist(n2)*16.0d));
                int accessScore1 = getTileAccessibilityPenalty(n1);
                int accessScore2 = getTileAccessibilityPenalty(n2);
                return (dist.get(n1) - dist.get(n2) + getTileDist(end, n1) - getTileDist(end, n2)
                		+ accessScore1 - accessScore2) > 0 ? 1 : -1;
            }
        });

        fringe.add(start);
        dist.put(start, 0.0);
        while (!fringe.isEmpty()) {
            LocalTile v = fringe.poll();
            if (visited.contains(v)) {
                continue;
            }
            visited.add(v);
            if (v.equals(end)) {
                do {
                    results.add(0, v);
                    v = prev.get(v);
                } while (v != null);
                return new ScoredPath(results, dist.get(end).intValue());
            }
            System.out.println(validNeighbors(grid, being, v) + " <<<<>>>>" + v);
            for (LocalTile c : validNeighbors(grid, being, v)) {
                if ((!dist.containsKey(c)) || (dist.containsKey(c) && dist.get(c) > dist.get(v) + getTileDist(v, c))) {
                    dist.put(c, dist.get(v) + getTileDist(v, c) + getTileAccessibilityPenalty(c));
                    //c.queue = dist.get(v) + v.dist(c) + c.dist(end);
                    fringe.add(c);
                    prev.put(c, v);
                }
            }
        }
        return null;
    }
    
    public static class ScoredPath {
    	public List<LocalTile> path;
    	public int score;
    	public ScoredPath(List<LocalTile> path, int score) {
    		this.path = path;
    		this.score = score;
    	}
    }

}
