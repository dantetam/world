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

import io.github.dantetam.localdata.ConstantData;
import io.github.dantetam.toolbox.log.CustomLog;
import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.ai.Pathfinder.ScoredPath;
import io.github.dantetam.world.civilization.Household;
import io.github.dantetam.world.civilization.Society;
import io.github.dantetam.world.dataparse.ItemData;
import io.github.dantetam.world.dataparse.ProcessData;
import io.github.dantetam.world.dataparse.WorldCsvParser;
import io.github.dantetam.world.grid.LocalGrid;
import io.github.dantetam.world.grid.LocalTile;
import io.github.dantetam.world.life.Human;
import io.github.dantetam.world.life.LivingEntity;
import io.github.dantetam.world.worldgen.LocalGridBiome;
import io.github.dantetam.world.worldgen.LocalGridInstantiate;

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
	
	protected LocalGrid grid;
	
	public Pathfinder(LocalGrid grid) {
		this.grid = grid;
	}
	
	/**
	 * @param minRestrict,maxRestrict Inclusive bounds on where to find neighbors, i.e.
	 * if minRestrict is (x,y,z), then do not include neighbors with coordinates less than 
	 * x,y,z in each dimension respectively. If null, there are no restrictions in one direction
	 * @return
	 */
	protected Set<LocalTile> validNeighbors(LivingEntity being, LocalTile tile) {
		Set<LocalTile> candidates = grid.getAllTiles14Pathfinding(tile.coords); //grid.getAccessibleNeighbors(tile);
		return candidates;
	}
	
	//Generate distance between two neighboring tiles, assuming they're adjacent
	protected double getTileDist(LocalTile a, LocalTile b) {
		if (a.coords.manhattanDist(b.coords) > 1) {
			return 1.4;
		}
		return 1.0;
	}
	
	private int getTileAccessibilityPenalty(LocalTile tile) {
		if (tile == null) return 0;

		if (tile.tileBlockId != ItemData.ITEM_EMPTY_ID) {
			int harvestTime = ItemData.getPickupTime(tile.tileBlockId);
			return (int) Math.max(1, Math.log10(harvestTime) + 1);
		}

		if (tile.building != null) {
			if (tile.building.calculatedLocations != null) {
				if (tile.building.calculatedLocations.contains(tile.coords)) {
					int tileId = tile.building.getRespectiveBlockId(tile.coords);
					int harvestTime = ItemData.getPickupTime(tileId);
					return (int) Math.max(1, Math.log10(harvestTime));
				}
			}
		}
		return 0;
	}
	
	public boolean hasValidPath(LivingEntity being, Vector3i start, Vector3i end) {
		ScoredPath scoredPath = grid.pathfinder.findPath(
				being, start, end);
		return scoredPath.isValid();
	}
	
	public ScoredPath findPath(LivingEntity being, Vector3i start, Vector3i end) {
		return findPath(being, grid.getTile(start), grid.getTile(end), null, null);
	}
	public ScoredPath findPath(LivingEntity being, LocalTile start, LocalTile end) {
		return findPath(being, start, end, null, null);
	}
	public ScoredPath findPath(LivingEntity being, LocalTile start, LocalTile end,
    		Vector3i minRestrict, Vector3i maxRestrict) {	
        int nodesExpanded = 0;
    	
    	if (start == null || end == null) {
        	//throw new IllegalArgumentException("Start or end null, start: " + start + ", end: " + end);
    		return new ScoredPath(null, 999);
    	}
    	
    	/*
    	CustomLog.outPrintln("-----------");
    	CustomLog.outPrintln("Finding path between: " + start.coords + " -> " + end.coords + ",\n with bounds: "
    			+ minRestrict + " <-> " + maxRestrict);
    	*/
    	
    	List<LocalTile> results = new ArrayList<>();
        if (start.equals(end)) {
            results.add(end);
            return new ScoredPath(results, 0);
        }
        Set<LocalTile> visited = new HashSet<>();
        final HashMap<LocalTile, Double> dist = new HashMap<>();
        Map<LocalTile, LocalTile> prev = new HashMap<>();
        PriorityQueue<LocalTile> fringe;
        
        fringe = new PriorityQueue<LocalTile>(16, new Comparator<LocalTile>() {
            @Override
            public int compare(LocalTile o1, LocalTile o2) {
            	LocalTile n1 = (LocalTile) o1;
            	LocalTile n2 = (LocalTile) o2;
                if (n1.equals(n2)) return 0;
                //return (int)((dist.get(n1) - dist.get(n2) + end.dist(n1) - end.dist(n2)*16.0d));
                int accessScore1 = getTileAccessibilityPenalty(n1);
                int accessScore2 = getTileAccessibilityPenalty(n2);
                return (
                		1 * (dist.get(n1) - dist.get(n2)) + 
                		1.05 * (getTileDist(end, n1) - getTileDist(end, n2)) + 
                		1 * (accessScore1 - accessScore2)
                		) > 0 ? 1 : -1;
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
            if (minRestrict != null) {
            	if (v.coords.x < minRestrict.x || 
            		v.coords.y < minRestrict.y ||
            		v.coords.z < minRestrict.z) { //If tile is not within the inclusive bounds
            		continue;
            	}
            }
            if (maxRestrict != null) {
            	if (v.coords.x > maxRestrict.x || 
            		v.coords.y > maxRestrict.y ||
            		v.coords.z > maxRestrict.z) {
            		continue;
            	}
            }
            nodesExpanded++;
            if (v.equals(end)) {
                do {
                    results.add(0, v);
                    v = prev.get(v);
                } while (v != null);
                return new ScoredPath(results, dist.get(end).doubleValue());
            }
            for (LocalTile c : validNeighbors(being, v)) {
                if ((!dist.containsKey(c)) || (dist.containsKey(c) && dist.get(c) > dist.get(v) + getTileDist(v, c))) {
                    dist.put(c, 1*dist.get(v) + 1.05*getTileDist(v, c)); //+ 0*getTileAccessibilityPenalty(c)
                    //c.queue = dist.get(v) + v.dist(c) + c.dist(end);
                    fringe.add(c);
                    prev.put(c, v);
                }
            }
        }
        
        //CustomLog.outPrintln("Nodes expanded: " + nodesExpanded);
        return new ScoredPath(null, 999);
    }
    
    public static class ScoredPath implements Comparable<ScoredPath> {
    	private List<LocalTile> path;
    	public double score;
    	public ScoredPath(List<LocalTile> path, double score) {
    		this.path = path;
    		this.score = score;
    	}
    	/**
    	 * @return Use this method to determine if a path exists and is valid (no null ambiguity)
    	 */
    	public boolean isValid() {
    		return this.path != null && this.path.size() > 0;
    	}
    	
    	public LocalTile getDest() {
    		if (!this.isValid()) {
    			throw new IllegalArgumentException("Last tile of path undefined for an invalid path");
    		}
    		return this.path.get(this.path.size() - 1);
    	}
    	
		@Override
		public int compareTo(ScoredPath o) {
			//Auto-generated method stub
			return (int) (o.score - this.score); //Inverted for higher score/longer distance = less ranking
		}
		
		public List<LocalTile> getPath(LocalGrid grid) {
			return path;
		}
    }
    
    //Pathfinding trials for analysis, since pathfinding is an intensive and ubiquitous calculation.
    public static void main(String[] args) {
    	WorldCsvParser.init();
    	
    	Vector3i sizes = new Vector3i(200,200,50);
		int biome = 3;
		ConstantData.ADVANCED_PATHING = false;
		LocalGrid activeLocalGrid = new LocalGridInstantiate(sizes, LocalGridBiome.defaultBiomeTest())
				.setupGrid();
		
		Society testSociety = new Society("TestSociety", activeLocalGrid);
		testSociety.societyCenter = new Vector3i(20,20,10);
		
		List<Human> people = new ArrayList<>();
		for (int j = 0; j < 1; j++) {
			int r = (int) (Math.random() * 99), c = (int) (Math.random() * 99);
			int h = activeLocalGrid.findHighestGroundHeight(r,c);
			
			Human human = new Human(testSociety, "Human" + j, "Human");
			people.add(human);
			activeLocalGrid.addLivingEntity(human, new Vector3i(r,c,h));
		}
		testSociety.addHousehold(new Household("", people));
		
		CustomLog.outPrintln("Start pathfinding time trial now");
		
		for (int i = 0; i < 10; i++) {
			long startTime = Calendar.getInstance().getTimeInMillis();
			
			//int r = (int) (Math.random() * 95) + 5;
			//int c = (int) (Math.random() * 95) + 5;
			int r = i*5, c = i*5;
			
			LocalTile baseTile = people.get(0).location;
			Vector3i coords = new Vector3i(
					baseTile.coords.x + r, 
					baseTile.coords.y + c, 
					activeLocalGrid.findHighestGroundHeight(baseTile.coords.x + r, baseTile.coords.y + c) - 3
			);
			activeLocalGrid.createTile(coords);
			
			CustomLog.outPrintln("Finding path from " + baseTile.coords + " to " + coords);
			
			new Pathfinder(activeLocalGrid).findPath(people.get(0), 
					baseTile, activeLocalGrid.getTile(coords));
			
			long endTime = Calendar.getInstance().getTimeInMillis();
			
			CustomLog.outPrintln("Completed trials in " + (endTime - startTime) + "ms");
		}
    }

}
