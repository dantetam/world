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
	
    public static List<LocalTile> findPath(LocalGrid grid, LivingEntity being, LocalTile start, LocalTile end) {
        List<LocalTile> results = new ArrayList<>();
        if (start.equals(end)) {
            results.add(end);
            return results;
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
                return (dist.get(n1) - dist.get(n2) + getTileDist(end, n1) - getTileDist(end, n2)) > 0 ? 1 : -1;
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
                return results;
            }
            for (LocalTile c : validNeighbors(grid, being, v)) {
                if ((!dist.containsKey(c)) || (dist.containsKey(c) && dist.get(c) > dist.get(v) + getTileDist(v, c))) {
                    dist.put(c, dist.get(v) + getTileDist(v, c));
                    //c.queue = dist.get(v) + v.dist(c) + c.dist(end);
                    fringe.add(c);
                    prev.put(c, v);
                }
            }
        }
        return null;
    }

}
