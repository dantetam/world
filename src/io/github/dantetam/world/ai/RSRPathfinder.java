package io.github.dantetam.world.ai;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Map.Entry;

import io.github.dantetam.localdata.ConstantData;
import io.github.dantetam.toolbox.MapUtil;
import io.github.dantetam.toolbox.Pair;
import io.github.dantetam.toolbox.VecGridUtil;
import io.github.dantetam.toolbox.VecGridUtil.RectangularSolid;
import io.github.dantetam.toolbox.log.CustomLog;
import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.ai.Pathfinder.ScoredPath;
import io.github.dantetam.world.civilization.Household;
import io.github.dantetam.world.civilization.Society;
import io.github.dantetam.world.dataparse.ProcessData;
import io.github.dantetam.world.dataparse.WorldCsvParser;
import io.github.dantetam.world.grid.ClusterVector3i;
import io.github.dantetam.world.grid.LocalGrid;
import io.github.dantetam.world.grid.LocalTile;
import io.github.dantetam.world.life.Human;
import io.github.dantetam.world.life.LivingEntity;
import io.github.dantetam.world.worldgen.LocalGridBiome;
import io.github.dantetam.world.worldgen.LocalGridInstantiate;

/**
 * 
 * An implementation of rectangular symmetry pathfinding, which aims to lessen the number of paths considered,
 * by creating straight lines that hide nodes in between.
 * 
 * This is pathfinding research found in the paper 
 * D. Harabor, A. Botea, and P. Kilby. Path Symmetries in Uniform-cost Grid Maps. 2011.
 * 
 * @author Dante
 *
 */
public class RSRPathfinder extends Pathfinder {

	private Map<Vector3i, Set<Vector3i>> macroEdgeConnections; //Shortcuts represented as a one-way path
	private boolean[][][] prunedRectTiles; //True iff the corresponding Vector3i in the grid, is in the interior of a rectangular solid
	private List<RectangularSolid> solids; //List of all the maximal rect solids as determined by the alg. 
		//(see VecGridUtil::findMaximalRectSolid())
	
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
			for (int d0 = minPoint.y + 1; d0 < minPoint.y + solid.solidDimensions.y - 1; d0++) {
				for (int d1 = minPoint.z + 1; d1 < minPoint.z + solid.solidDimensions.z - 1; d1++) {
					Vector3i sideA = new Vector3i(newDim, d0, d1);
					Vector3i sideB = new Vector3i(otherSide, d0, d1);
					MapUtil.insertNestedSetMap(macroEdgeConnections, sideA, sideB);
					MapUtil.insertNestedSetMap(macroEdgeConnections, sideB, sideA);
					for (int pruneDim = newDim + 1; pruneDim < otherSide; pruneDim++) {
						prunedRectTiles[pruneDim][d0][d1] = true;
					}
				}
			}
		}
		else if (direction == 'c') {
			int newDim = minPoint.y;
			int otherSide = minPoint.y + solid.solidDimensions.y - 1;
			for (int d0 = minPoint.x + 1; d0 < minPoint.x + solid.solidDimensions.x - 1; d0++) {
				for (int d1 = minPoint.z + 1; d1 < minPoint.z + solid.solidDimensions.z - 1; d1++) {
					Vector3i sideA = new Vector3i(d0, newDim, d1);
					Vector3i sideB = new Vector3i(d0, otherSide, d1);
					MapUtil.insertNestedSetMap(macroEdgeConnections, sideA, sideB);
					MapUtil.insertNestedSetMap(macroEdgeConnections, sideB, sideA);
					for (int pruneDim = newDim + 1; pruneDim < otherSide; pruneDim++) {
						prunedRectTiles[d0][pruneDim][d1] = true;
					}
				}
			}
		}
		else if (direction == 'h') {
			int newDim = minPoint.z;
			int otherSide = minPoint.z + solid.solidDimensions.z - 1;
			for (int d0 = minPoint.x + 1; d0 < minPoint.x + solid.solidDimensions.x - 1; d0++) {
				for (int d1 = minPoint.y + 1; d1 < minPoint.y + solid.solidDimensions.y - 1; d1++) {
					Vector3i sideA = new Vector3i(d0, d1, newDim);
					Vector3i sideB = new Vector3i(d0, d1, otherSide);
					MapUtil.insertNestedSetMap(macroEdgeConnections, sideA, sideB);
					MapUtil.insertNestedSetMap(macroEdgeConnections, sideB, sideA);
					for (int pruneDim = newDim + 1; pruneDim < otherSide; pruneDim++) {
						prunedRectTiles[d0][d1][pruneDim] = true;
					}
				}
			}
		}
	}
	
	public void tempNodeInRectSolid(Vector3i location, boolean addingMode) {
		for (RectangularSolid solid: this.solids) {
			if (solid.insideInterior(location)) {
				tempNodeInRectSolid(solid, location, addingMode);
				tempAvailSolid(solid, addingMode);
				return;
			}
		}
	}
	
	//Connect a temporary node to its nearest perimeter
	public void tempNodeInRectSolid(RectangularSolid solid, Vector3i location, boolean addingMode) {
		this.prunedRectTiles[location.x][location.y][location.z] = false;
		
		Vector3i minR = location.clone().x(solid.topLeftCorner.x);
		Vector3i minC = location.clone().y(solid.topLeftCorner.y);
		Vector3i minH = location.clone().z(solid.topLeftCorner.z);
		
		Vector3i maxR = location.clone().x(solid.topLeftCorner.x + solid.solidDimensions.x - 1);
		Vector3i maxC = location.clone().y(solid.topLeftCorner.y + solid.solidDimensions.y - 1);
		Vector3i maxH = location.clone().z(solid.topLeftCorner.z + solid.solidDimensions.z - 1);
		
		List<Vector3i> connections = new ArrayList<>();
		connections.add(minR); connections.add(minC); connections.add(minH);
		connections.add(maxR); connections.add(maxC); connections.add(maxH);
		
		if (addingMode) {
			for (Vector3i connection: connections) {
				MapUtil.insertNestedSetMap(this.macroEdgeConnections, location, connection);
				MapUtil.insertNestedSetMap(this.macroEdgeConnections, connection, location);
			}
		}
		else {
			for (Vector3i connection: connections) {
				MapUtil.removeSafeNestSetMap(this.macroEdgeConnections, location, connection);
				MapUtil.removeSafeNestSetMap(this.macroEdgeConnections, connection, location);
			}
		}
	}
	
	public void tempAvailSolid(RectangularSolid solid, boolean pruned) {
		Vector3i minPoint = solid.topLeftCorner;
		for (int d0 = minPoint.x + 1; d0 <= minPoint.x + solid.solidDimensions.x - 1; d0++) {
			for (int d1 = minPoint.y + 1; d1 <= minPoint.y + solid.solidDimensions.y - 1; d1++) {
				for (int d2 = minPoint.z + 1; d2 <= minPoint.z + solid.solidDimensions.z - 1; d2++) {
					this.prunedRectTiles[d0][d1][d2] = pruned;
				}
			}
		}
	}
	
	public void adjustRSR(Vector3i coords, boolean isSolidBlock) {
		for (Vector3i neighbor: this.grid.getAllNeighbors14(coords)) {
			if (grid.tileIsPartAccessible(neighbor)) {
				this.tempNodeInRectSolid(neighbor, isSolidBlock);
			}
		}
 	}
	
	@Override
	protected Set<LocalTile> validNeighbors(LivingEntity being, LocalTile tile) {
		//TODO
		//Implement the ability to traverse tiles that need to be dug (add accessibility penalty)
		//Also try to improve height and diagonal height walking across terrain
		
		Set<LocalTile> candidates = grid.getAccessibleNeighbors(tile); //grid.getAllTiles14Pathfinding(tile.coords);
		if (macroEdgeConnections.containsKey(tile.coords)) {
			Set<Vector3i> coords = macroEdgeConnections.get(tile.coords);
			for (Vector3i coord: coords) {
				//CustomLog.outPrintln("Added shortcut from " + tile.coords + " to " + coord);
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
		
		//return candidates;
	}
	
	//Generate distance between two neighboring tiles
	@Override
	protected double getTileDist(LocalTile a, LocalTile b) {
		return a.coords.manhattanDist(b.coords);
	}
	
	
	/**
	 * The main RSR pathfinding routine:
	 * 1) prune by quickly exiting when the path is known to be impossible;
	 * 2) connect start and end to the RSR memotized structure, per the paper; 
	 * 3) find a path through normal A* in the augmented graph;
	 * 4) fix the graph by removing temporary nodes added in 2);
	 * 5) return the resulting path if one exists.
	 */
	@Override
	public ScoredPath findPath(LivingEntity being, LocalTile start, LocalTile end,
    		Vector3i minRestrict, Vector3i maxRestrict) {
		if (start == null || end == null) {
        	CustomLog.errPrintln("Start or end null, start: " + start + ", end: " + end);
    		return new ScoredMacroedgePath(null, Integer.MAX_VALUE / 2);
    	}
		if (!grid.tileIsPartAccessible(start.coords) || !grid.tileIsPartAccessible(end.coords)) {
			//CustomLog.outPrintln("Warning, start or end not accessible: " + start + "; " + end);
			return new ScoredMacroedgePath(null, Integer.MAX_VALUE / 2);
		}
		
		/*
		//Do quick check if path is possible (i.e. in same connected component)
		//TODO: Fix or remove this comment block
		if (grid.connectedCompsMap.containsKey(start.coords) && grid.connectedCompsMap.containsKey(end.coords)) {
			if (grid.connectedCompsMap.get(start.coords) != grid.connectedCompsMap.get(end.coords)) {
				CustomLog.errPrintln("No match valid component (3d shortcutting)");
				return new ScoredMacroedgePath(null, Integer.MAX_VALUE / 2);
			}
		}
		else {
			CustomLog.errPrintln("Not in comp: " + grid.connectedCompsMap.containsKey(start.coords) + 
					"; " + grid.connectedCompsMap.containsKey(end.coords));
			return new ScoredMacroedgePath(null, Integer.MAX_VALUE / 2);
		}
		*/
		
		//Temporarily insert nodes and macro edges for the start and end as needed
		RectangularSolid startSolid = null, endSolid = null;
		for (RectangularSolid solid: solids) {
			if (solid.insideInterior(start.coords)) {
				startSolid = solid;
			}
			if (solid.insideInterior(end.coords)) {
				endSolid = solid;
			}
		}
		if (startSolid != null && startSolid == endSolid) { //If in the same rect solid
			tempAvailSolid(startSolid, false);
		}
		if (startSolid != null) {
			tempNodeInRectSolid(startSolid, start.coords, true);
		}
		if (endSolid != null && startSolid != endSolid) {
			tempNodeInRectSolid(endSolid, end.coords, true);
		}
		
		ScoredPath scoredSuperMethod = super.findPath(being, start, end, minRestrict, maxRestrict);
		ScoredMacroedgePath macroedgePath = new ScoredMacroedgePath(
				scoredSuperMethod.getPath(grid), scoredSuperMethod.score);
		
		//Remove the temporary data if it was created
		if (startSolid != null && startSolid == endSolid) {
			tempAvailSolid(startSolid, false);
		}
		else if (startSolid != null) {
			tempNodeInRectSolid(startSolid, start.coords, false);
		}
		else if (endSolid != null) {
			tempNodeInRectSolid(endSolid, end.coords, false);
		}	
		
		return macroedgePath;
	}
	
	/**
	 * Much like the method below for pathfinding across a batch, but simply take the first available path.
	 * Use the same pruning procedures.
	 * @return A pair of localTile -> scoredPath maps: the first indicates normal paths, 
	 * 	   the second indicates impossible paths. 
	 */
	public ScoredPath batchPathfindingFirst(LivingEntity being, LocalTile start, 
			List<LocalTile> listEndGoals,
    		Vector3i minRestrict, Vector3i maxRestrict) {
		for (LocalTile end: listEndGoals) {
			ScoredPath path = this.findPath(being, start, end, minRestrict, maxRestrict);
			if (path.isValid()) {
				return path;
			}
		}
		return null;
	}
	/*
	public ScoredPath batchPathfindingFirst(LivingEntity being, LocalTile start, 
			List<LocalTile> listEndGoals,
    		Vector3i minRestrict, Vector3i maxRestrict) {
		//TODO; Also, for RSR paths, do not convert macroedges into paths until actually needed;
		Map<LocalTile, ScoredPath> impossiblePaths = new HashMap<>();
		for (LocalTile end: listEndGoals) {
			if (impossiblePaths.containsKey(end)) {
				continue;
			}
			ScoredPath path = this.findPath(being, start, end, minRestrict, maxRestrict);
			if (path.isValid()) {
				return path;
			}
			else {
				impossiblePaths.put(end, null);
				//Prune paths to all of the cluster, which should be inaccessible as well
				ClusterVector3i cluster = this.grid.find3dClusterWithCoord(end.coords);
				if (cluster == null) continue;
				for (LocalTile pruneCand: listEndGoals) {
					if (cluster.clusterData.contains(pruneCand.coords)) {
						impossiblePaths.put(pruneCand, null);
					}
				}
			}
		}
		return null;
	}
	*/
	public ScoredPath batchPathfindingFirst(LivingEntity being, LocalTile start, 
			List<LocalTile> listEndGoals) {
		return this.batchPathfindingFirst(being, start, listEndGoals, null, null);
	}
	
	/**
	 * 
	 * @param being
	 * @param start
	 * @param listEndGoals
	 * @param minRestrict
	 * @param maxRestrict
	   //Implement a batch-pathfinding problem to prune this place and its neighbors i.e.
	   //if A -> X returns no valid path, then batch all accessible neighbors/clustered vecs Y,Z,etc.
	   //and prune these paths by returning no path for A -> Y, A -> Z, even if they seem viable.
	 * @return A pair of localTile -> scoredPath maps: the first indicates normal paths, 
	 * 	   the second indicates impossible paths. 
	 */
	public Pair<Map<LocalTile, ScoredPath>> batchPathfinding(LivingEntity being, LocalTile start, 
			List<LocalTile> listEndGoals,
    		Vector3i minRestrict, Vector3i maxRestrict) {
		Map<LocalTile, ScoredPath> scoring = new HashMap<>();
		Map<LocalTile, ScoredPath> impossiblePaths = new HashMap<>();
		for (LocalTile end: listEndGoals) {
			if (scoring.containsKey(end) || impossiblePaths.containsKey(end)) {
				continue;
			}
			ScoredPath path = this.findPath(being, start, end, minRestrict, maxRestrict);
			if (path.isValid()) {
				scoring.put(end, path);
			}
			else {
				impossiblePaths.put(end, null);
				//Prune paths to all of the cluster, which should be inaccessible as well
				ClusterVector3i cluster = this.grid.find3dClusterWithCoord(end.coords);
				if (cluster == null) continue;
				for (LocalTile pruneCand: listEndGoals) {
					if (!scoring.containsKey(pruneCand)) {
						if (cluster.clusterData.contains(pruneCand.coords)) {
							impossiblePaths.put(pruneCand, null);
						}
					}
				}
			}
		}
		return new Pair<Map<LocalTile, ScoredPath>>(
				MapUtil.getSortedMapByValueAsc(scoring), impossiblePaths);
	}
	public Pair<Map<LocalTile, ScoredPath>> batchPathfinding(LivingEntity being, LocalTile start, 
			List<LocalTile> listEndGoals) {
		return this.batchPathfinding(being, start, listEndGoals, null, null);
	}
		
	public static class ScoredMacroedgePath extends ScoredPath implements Comparable<ScoredPath> {
		private List<LocalTile> memotizedFullPath = null;
		public double score;
		
		public ScoredMacroedgePath(List<LocalTile> pathTilesCorners, double score) {
			super(pathTilesCorners, score);
		}
		
		/**
		 * @param path  A list of tiles representing a path with possible straight shortcuts
		 * @return A new path with the straight shortcuts replaced by individual tile movements
		 */
		@Override
		public List<LocalTile> getPath(LocalGrid grid) {
			if (memotizedFullPath != null) return memotizedFullPath;
			if (path == null) return null;
			List<LocalTile> newPath = new ArrayList<>();
			for (int i = 0; i < path.size(); i++) {
				LocalTile tile = path.get(i);
				LocalTile nextTile = null;
				newPath.add(tile);
				if (i != path.size() - 1) {
					nextTile = path.get(i+1);
					int distR = nextTile.coords.x - tile.coords.x;
					int distC = nextTile.coords.y - tile.coords.y;
					int distH = nextTile.coords.z - tile.coords.z;
					if (Math.abs(distR) > 1) {
						for (int d = tile.coords.x + 1; d <= nextTile.coords.x - 1; 
								d += (int) Math.signum(distR)) {
							newPath.add(grid.getTile(tile.coords.x(d)));
						}
					}
					else if (Math.abs(distC) > 1) {
						for (int d = tile.coords.y + 1; d <= nextTile.coords.y - 1; 
								d += (int) Math.signum(distC)) {
							newPath.add(grid.getTile(tile.coords.y(d)));
						}
					}
					else if (Math.abs(distH) > 1) {
						for (int d = tile.coords.z + 1; d <= nextTile.coords.z - 1; 
								d += (int) Math.signum(distH)) {
							newPath.add(grid.getTile(tile.coords.z(d)));
						}
					}
				}
			}
			memotizedFullPath = newPath; //Save in memory for later
			return newPath;
		}
		
		public int getNumTiles() {
			return getPath(null).size();
		}
		
		public int compareTo(ScoredMacroedgePath o) {
			//Auto-generated method stub
			return (int) (o.score - this.score); //Inverted for higher score/longer distance = less ranking
		}
	}
	
    //Pathfinding trials for analysis, since pathfinding is an intensive and ubiquitous calculation.
    public static void main(String[] args) {
    	WorldCsvParser.init();
    	
    	Vector3i sizes = new Vector3i(200,200,50);
		ConstantData.ADVANCED_PATHING = true;
		LocalGrid activeLocalGrid = new LocalGridInstantiate(sizes, LocalGridBiome.defaultBiomeTest())
				.setupGrid();
		
		Society testSociety = new Society("TestSociety", activeLocalGrid);
		testSociety.societyCenter = new Vector3i(20,20,10);
		
		List<Human> people = new ArrayList<>();
		for (int j = 0; j < 20; j++) {
			int r = (int) (Math.random() * 99), c = (int) (Math.random() * 99);
			int h = activeLocalGrid.findHighestGroundHeight(r,c);
			
			Human human = new Human(testSociety, "Human" + j, "Human");
			people.add(human);
			activeLocalGrid.addLivingEntity(human, new Vector3i(r,c,h));
		}
		testSociety.addHousehold(new Household("", people));
		
		RSRPathfinder pathfinder = new RSRPathfinder(activeLocalGrid);
		Pathfinder backupPathfinder = new Pathfinder(activeLocalGrid);
		
		CustomLog.outPrintln("Start pathfinding time trial now");
		
		for (int i = 0; i < 10; ) {
			int r = (int) (Math.random() * 95) + 5;
			int c = (int) (Math.random() * 95) + 5;
			
			int randPersonIndex = (int) (Math.random() * people.size());
			
			LocalTile baseTile = people.get(randPersonIndex).location;
			Vector3i coords = new Vector3i(
					r,
					c,
					activeLocalGrid.findHighestGroundHeight(r, c)
			);
			activeLocalGrid.createTile(coords);
			
			if (!activeLocalGrid.tileIsPartAccessible(baseTile.coords)) continue;
			if (!activeLocalGrid.tileIsPartAccessible(coords)) continue;
			i++;
			
			CustomLog.outPrintln("Finding path from " + baseTile.coords + " to " + coords);
			//CustomLog.errPrintln(activeLocalGrid.getAccessibleNeighbors(baseTile));
			CustomLog.errPrintln("Below: " + activeLocalGrid.getTile(baseTile.coords.getSum(0, 0, -1)));
			
			long startTime = Calendar.getInstance().getTimeInMillis();
			
			ScoredPath path = pathfinder.findPath(people.get(0), 
					baseTile, activeLocalGrid.getTile(coords));
			
			long endTime = Calendar.getInstance().getTimeInMillis();
			
			if (path.isValid())
				CustomLog.outPrintln("Found path, size: " + path.getNumTiles());
			else
				CustomLog.outPrintln("No path found");
			
			CustomLog.outPrintln("Completed trial in " + (endTime - startTime) + "ms");
			CustomLog.outPrintln();
			
			startTime = Calendar.getInstance().getTimeInMillis();
			
			CustomLog.outPrintln("Backup pathfinder: ");
			path = backupPathfinder.findPath(people.get(0), 
					baseTile, activeLocalGrid.getTile(coords));
			
			endTime = Calendar.getInstance().getTimeInMillis();
			
			if (path.isValid()) {
				CustomLog.outPrintln("Found backup path, size: " + path.getNumTiles());
				//for (LocalTile tile: path.getPath(activeLocalGrid)) CustomLog.errPrintln("\t" + tile.coords);
				CustomLog.errPrintln("Path validated: " + validatePath(path.getPath(activeLocalGrid)));
			} else
				CustomLog.outPrintln("No backup path found");
			
			CustomLog.outPrintln("Completed backup trial in " + (endTime - startTime) + "ms");
			
			CustomLog.outPrintln("-----------------------");
			
		}
    }

}
