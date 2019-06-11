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

import io.github.dantetam.toolbox.MapUtil;
import io.github.dantetam.toolbox.VecGridUtil;
import io.github.dantetam.toolbox.VecGridUtil.RectangularSolid;
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
import io.github.dantetam.world.worldgen.LocalGridTerrainInstantiate;

/**
 * 
 * @author Dante
 *
 */
public class RSRPathfinder extends Pathfinder {

	private Map<Vector3i, Set<Vector3i>> macroEdgeConnections;
	private boolean[][][] prunedRectTiles;
	private List<RectangularSolid> solids;
	
	public RSRPathfinder(LocalGrid grid) {
		super(grid);
		prunedRectTiles = new boolean[grid.rows][grid.cols][grid.heights];
		macroEdgeConnections = new HashMap<>();
		fillMacroedgesWithBlocks();
	}
	
	private void fillMacroedgesWithBlocks() {
		solids = VecGridUtil.findMaximalRectSolids(this.grid);
		for (RectangularSolid solid: solids) {
			connectMacroedgeToOtherSide(solid);
		}
	}
	
	public void connectMacroedgeToOtherSide(RectangularSolid solid) {
		connectMacroedgeToOtherSide(solid, 'r');
		connectMacroedgeToOtherSide(solid, 'c');
		connectMacroedgeToOtherSide(solid, 'h');
	}
	public void connectMacroedgeToOtherSide(RectangularSolid solid, char direction) {
		Vector3i minPoint = solid.topLeftCorner;
		if (direction == 'r') {
			int newDim = minPoint.x;
			int otherSide = minPoint.x + solid.solidDimensions.x - 1;
			for (int d0 = minPoint.y + 1; d0 <= minPoint.y + solid.solidDimensions.y - 1; d0++) {
				for (int d1 = minPoint.z + 1; d1 <= minPoint.z + solid.solidDimensions.z - 1; d1++) {
					Vector3i sideA = new Vector3i(newDim, d0, d1);
					Vector3i sideB = new Vector3i(otherSide, d0, d1);
					MapUtil.insertNestedSetMap(macroEdgeConnections, sideA, sideB);
					MapUtil.insertNestedSetMap(macroEdgeConnections, sideB, sideA);
					for (int pruneDim = newDim + 1; pruneDim <= otherSide - 1; pruneDim++) {
						prunedRectTiles[pruneDim][d0][d1] = true;
					}
				}
			}
		}
		else if (direction == 'c') {
			int newDim = minPoint.y;
			int otherSide = minPoint.y + solid.solidDimensions.y - 1;
			for (int d0 = minPoint.x + 1; d0 <= minPoint.x + solid.solidDimensions.x - 1; d0++) {
				for (int d1 = minPoint.z + 1; d1 <= minPoint.z + solid.solidDimensions.z - 1; d1++) {
					Vector3i sideA = new Vector3i(d0, newDim, d1);
					Vector3i sideB = new Vector3i(d0, otherSide, d1);
					MapUtil.insertNestedSetMap(macroEdgeConnections, sideA, sideB);
					MapUtil.insertNestedSetMap(macroEdgeConnections, sideB, sideA);
					for (int pruneDim = newDim + 1; pruneDim <= otherSide - 1; pruneDim++) {
						prunedRectTiles[d0][pruneDim][d1] = true;
					}
				}
			}
		}
		else if (direction == 'h') {
			int newDim = minPoint.z;
			int otherSide = minPoint.z + solid.solidDimensions.z - 1;
			for (int d0 = minPoint.x + 1; d0 <= minPoint.x + solid.solidDimensions.x - 1; d0++) {
				for (int d1 = minPoint.y + 1; d1 <= minPoint.y + solid.solidDimensions.y - 1; d1++) {
					Vector3i sideA = new Vector3i(d0, d1, newDim);
					Vector3i sideB = new Vector3i(d0, d1, otherSide);
					MapUtil.insertNestedSetMap(macroEdgeConnections, sideA, sideB);
					MapUtil.insertNestedSetMap(macroEdgeConnections, sideB, sideA);
					for (int pruneDim = newDim + 1; pruneDim <= otherSide - 1; pruneDim++) {
						prunedRectTiles[d0][d1][pruneDim] = true;
					}
				}
			}
		}
	}
	
	//Connect a temporary node to its nearest perimeter
	public void putTempNodeInRectSolid(RectangularSolid solid, Vector3i location) {
		this.macroEdgeConnections
	}
	
	@Override
	protected Set<LocalTile> validNeighbors(LivingEntity being, LocalTile tile) {
		Set<LocalTile> candidates = grid.getAccessibleNeighbors(tile);
		if (macroEdgeConnections.containsKey(tile.coords)) {
			Set<Vector3i> coords = macroEdgeConnections.get(tile.coords);
			for (Vector3i coord: coords) {
				//System.out.println("Added shortcut from " + tile.coords + " to " + coord);
				candidates.add(grid.getTile(coord));
			}
		}
		
		Set<LocalTile> filtered = new HashSet<>();
		for (LocalTile candidate: candidates) {
			if (!prunedRectTiles[candidate.coords.x][candidate.coords.y][candidate.coords.z]) {
				filtered.add(candidate);
			}
		}
		
		return filtered;
	}
	
	//Generate distance between two neighboring tiles, assuming they're adjacent
	protected double getTileDist(LocalTile a, LocalTile b) {
		return a.coords.manhattanDist(b.coords);
	}
	
	@Override
	protected ScoredPath findPath(LivingEntity being, LocalTile start, LocalTile end,
    		Vector3i minRestrict, Vector3i maxRestrict) {	
		//Temporarily insert nodes and macro edges for the start and end as needed
		RectangularSolid startSolid = null, endSolid = null;
		for (RectangularSolid solid: solids) {
			if (solid.insideInterior(start.coords)) {
				startSolid = solid;
				break;
			}
			if (solid.insideInterior(end.coords)) {
				endSolid = solid;
				break;
			}
		}
		if (startSolid != null && startSolid == endSolid) { //If in the same rect solid
			TODO
		}
		if (startSolid != null) {
			TODO
		}
		if (endSolid != null) {
			TODO
		}
		
		ScoredPath path = super.findPath(being, start, end, minRestrict, maxRestrict);
		
		//Remove the temporary data if it was created
		TODO
		
		return path;
	}
	
    //Pathfinding trials for analysis, since pathfinding is an intensive and ubiquitous calculation.
    public static void main(String[] args) {
    	WorldCsvParser.init();
    	
    	Vector3i sizes = new Vector3i(200,200,50);
		int biome = 3;
		LocalGrid activeLocalGrid = new LocalGridTerrainInstantiate(sizes, biome).setupGrid(false);
		
		Society testSociety = new Society("TestSociety", activeLocalGrid);
		testSociety.societyCenter = new Vector3i(20,20,10);
		
		List<Human> people = new ArrayList<>();
		for (int j = 0; j < 1; j++) {
			int r = (int) (Math.random() * 99), c = (int) (Math.random() * 99);
			int h = activeLocalGrid.findHighestGroundHeight(r,c);
			
			Human human = new Human(testSociety, "Human" + j);
			people.add(human);
			activeLocalGrid.addHuman(human, new Vector3i(r,c,h));
		}
		testSociety.addHousehold(new Household(people));
		
		RSRPathfinder pathfinder = new RSRPathfinder(activeLocalGrid);
		
		System.out.println("Start pathfinding time trial now");
		
		for (int i = 0; i < 10; i++) {
			long startTime = Calendar.getInstance().getTimeInMillis();
			
			//int r = (int) (Math.random() * 95) + 5;
			//int c = (int) (Math.random() * 95) + 5;
			int r = i*5 + 5, c = i*5 + 5;
			
			LocalTile baseTile = people.get(0).location;
			Vector3i coords = new Vector3i(
					baseTile.coords.x + r, 
					baseTile.coords.y + c, 
					activeLocalGrid.findHighestGroundHeight(baseTile.coords.x + r, baseTile.coords.y + c) - 3
			);
			activeLocalGrid.createTile(coords);
			
			System.out.println("Finding path from " + baseTile.coords + " to " + coords);
			
			ScoredPath path = pathfinder.findPath(people.get(0), 
					baseTile, activeLocalGrid.getTile(coords));
			
			long endTime = Calendar.getInstance().getTimeInMillis();
			
			System.out.println(path.path);
			
			System.out.println("Completed trials in " + (endTime - startTime) + "ms");
		}
    }

}
