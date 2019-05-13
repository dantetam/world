package io.github.dantetam.world.ai;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.civilization.Household;
import io.github.dantetam.world.civilization.Human;
import io.github.dantetam.world.civilization.LivingEntity;
import io.github.dantetam.world.civilization.Society;
import io.github.dantetam.world.dataparse.ItemData;
import io.github.dantetam.world.dataparse.ProcessData;
import io.github.dantetam.world.dataparse.WorldCsvParser;
import io.github.dantetam.world.grid.LocalGrid;
import io.github.dantetam.world.grid.LocalTile;
import io.github.dantetam.world.worldgen.LocalGridTerrainInstantiate;

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
	
	private LocalGrid grid;
	
	public Pathfinder(LocalGrid grid) {
		this.grid = grid;
	}
	
	private Set<LocalTile> validNeighbors(LivingEntity being, LocalTile tile) {
		return grid.getAccessibleNeighbors(tile, being);
	}
	
	private double getTileDist(LocalTile a, LocalTile b) {
		return a.coords.dist(b.coords);
	}
	
	private int getTileAccessibilityPenalty(LocalTile tile) {
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
	
    public ScoredPath findPath(LivingEntity being, LocalTile start, LocalTile end) {
        int nodesExpanded = 0;
    	
    	if (start == null || end == null) {
        	throw new IllegalArgumentException("Start or end null, start: " + start + ", end: " + end);
        }
    	
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
                return (dist.get(n1) - dist.get(n2) + 0.1*getTileDist(end, n1) - 0.1*getTileDist(end, n2)
                		+ 0.1*accessScore1 - 0.1*accessScore2) > 0 ? 1 : -1;
            }
        });
        
        fringe.add(start);
        dist.put(start, 0.0);
        while (!fringe.isEmpty()) {
            LocalTile v = fringe.poll();
            nodesExpanded++;
            if (visited.contains(v)) {
                continue;
            }
            visited.add(v);
            if (v.equals(end)) {
                do {
                    results.add(0, v);
                    v = prev.get(v);
                } while (v != null);
                
                System.out.println("Nodes expanded: " + nodesExpanded);
                
                return new ScoredPath(results, dist.get(end).intValue());
            }
            for (LocalTile c : validNeighbors(being, v)) {
                if ((!dist.containsKey(c)) || (dist.containsKey(c) && dist.get(c) > dist.get(v) + getTileDist(v, c))) {
                    dist.put(c, dist.get(v) + 0.1*getTileDist(v, c) + 0.1*getTileAccessibilityPenalty(c));
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
    
    //Pathfinding trials for analysis, since pathfinding is an intensive and ubiquitous calculation.
    public static void main(String[] args) {
    	WorldCsvParser.init();
    	
    	Vector3i sizes = new Vector3i(200,200,50);
		int biome = 3;
		LocalGrid activeLocalGrid = new LocalGridTerrainInstantiate(sizes, biome).setupGrid();
		
		Society testSociety = new Society("TestSociety", activeLocalGrid);
		testSociety.societyCenter = new Vector3i(20,20,10);
		
		List<Human> people = new ArrayList<>();
		for (int j = 0; j < 1; j++) {
			int r = (int) (Math.random() * 99), c = (int) (Math.random() * 99);
			int h = activeLocalGrid.findHighestGroundHeight(r,c);
			
			Human human = new Human(testSociety, "Human" + j);
			people.add(human);
			activeLocalGrid.addHuman(human, new Vector3i(r,c,h));
			
			human.inventory.addItem(ItemData.randomItem());
			human.inventory.addItem(ItemData.randomItem());
			human.inventory.addItem(ItemData.randomItem());
			human.inventory.addItem(ItemData.randomItem());  
			human.inventory.addItem(ItemData.item("Wheat Seeds", 50));
			human.inventory.addItem(ItemData.item("Pine Wood", 50));
		}
		testSociety.addHousehold(new Household(people));
		
		System.out.println("Start pathfinding time trial now");
		
		for (int i = 0; i < 10; i++) {
			long startTime = Calendar.getInstance().getTimeInMillis();
			
			int r = (int) (Math.random() * 95) + 5;
			int c = (int) (Math.random() * 95) + 5;
			
			LocalTile baseTile = people.get(0).location;
			Vector3i coords = new Vector3i(
					baseTile.coords.x + r, 
					baseTile.coords.y + c, 
					activeLocalGrid.findHighestGroundHeight(baseTile.coords.x + r, baseTile.coords.y + c) - 3
			);
			activeLocalGrid.createTile(coords);
			
			System.out.println("Finding path from " + baseTile.coords + " to " + coords);
			
			Pathfinder.findPath(activeLocalGrid, people.get(0), baseTile, activeLocalGrid.getTile(coords));
			
			long endTime = Calendar.getInstance().getTimeInMillis();
			
			System.out.println("Completed trials in " + (endTime - startTime) + "ms");
		}
    }

}
