package io.github.dantetam.world.grid;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import io.github.dantetam.localdata.ConstantData;
import io.github.dantetam.toolbox.CollectionUtil;
import io.github.dantetam.toolbox.MapUtil;
import io.github.dantetam.toolbox.VecGridUtil;
import io.github.dantetam.toolbox.VecGridUtil.RectangularSolid;
import io.github.dantetam.toolbox.log.CustomLog;
import io.github.dantetam.vector.Vector2i;
import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.ai.RSRPathfinder;
import io.github.dantetam.world.civilization.Society;
import io.github.dantetam.world.dataparse.ItemData;
import io.github.dantetam.world.dataparse.WorldCsvParser;
import io.github.dantetam.world.grid.SpaceFillingAlg.NeighborMode;
import io.github.dantetam.world.items.Inventory;
import io.github.dantetam.world.items.InventoryItem;
import io.github.dantetam.world.life.Human;
import io.github.dantetam.world.life.LivingEntity;
import io.github.dantetam.world.process.LocalProcess;
import io.github.dantetam.world.worldgen.LocalGridBiome;
import io.github.dantetam.world.worldgen.LocalGridInstantiate;
import kdtreegeo.KdTree;

/**
 * A grid representing one section of the whole world, i.e. a self contained world with its own people, tiles, and so on.
 * @author Dante
 *
 * This grid contains a ton of memotized information for quick retrieval and update later.
 * Most of this was implemented to search across a grid in less than grid time,
 * while also updating good records in constant time per access/change.
 *
 */


public class LocalGrid {

	public int rows, cols, heights; //Sizes of the three dimensions of this local grid
	private LocalTile[][][] grid; //A 3D grid, with the third dimension representing height, such that [][][50] is one level higher than [][][49].
	private Set<LocalBuilding> allBuildings; //Contains all buildings within this grid, updated when necessary
	private Set<LivingEntity> allLivingBeings;
	
	private Map<Integer, KdTree<Vector3i>> itemIdQuickTileLookup; //Search for item ids quickly in 3d space
	private KdTree<Vector3i> globalItemsLookup; //Search for all items left on the floor, for quicker kdtree search over a smaller space
	private Map<String, KdTree<Vector3i>> itemGroupTileLookup; //Search for items by group, in kdtree space 
	
	private Map<Integer, KdTree<Vector3i>> globalTileBlockLookup;
	private KdTree<Vector3i> buildingLookup;
	public Map<Integer, Double> tileIdCounts; //Updated per tile update
	
	//Updated per tile, accessibility records defined by this enum
	public enum LocalGridAccess {FULL_ACCESS, IF_HARVEST_ACCESS, NO_ACCESS};
	private LocalGridAccess[][][] accessibleTileRecord;
	
	//TODO: Memotize beauty data and other properties of tiles for lookup later
	
	private Map<Integer, KdTree<Vector3i>> buildIdQuickTileLookup;
	
	private KdTree<Vector3i> peopleLookup;
	
	public RSRPathfinder pathfinder;
	
	public List<LocalGridLandClaim> localLandClaims;
	
	//3d connected components
	//Update these two values below when the grid updates, using local (linear time in size of cluster) updates
	
	//Mapping of all available vectors to a unique numbered space (a 3d volume)
	//Note that these are NOT exactly the indices in any other list of clusters,
	//but two keys with the same value always guarantee they share the same cluster.
	//All connected, accessible 14-neighbors definition.
	public Map<Vector3i, Integer> connectedCompsMap;
	public KdTree<ClusterVector3i> clustersList3d;
	public int compNumCounter = 0; //Always ensure that a new cluster can use this unused index
	
	//2d connected surfaces
	//All connected, accessible 6-neighbors definition.
	public Map<Vector3i, Integer> connectedCompsMap2d;
	public KdTree<ClusterVector3i> clustersListFlat2dSurfaces;
	
	//2d connected surfaces where neighbors can move height by at most one (14-neighbors)
	public Map<Vector3i, Integer> connectedCompsMap2dOneHeight; //TODO;
	public KdTree<ClusterVector3i> clustersListOneHeight2dSurfaces;
	
	public LocalGrid(Vector3i size) {
		rows = size.x; cols = size.y; heights = size.z;
		grid = new LocalTile[rows][cols][heights];
		allBuildings = new HashSet<>();
		allLivingBeings = new HashSet<>();
		
		itemIdQuickTileLookup = new HashMap<>();
		globalItemsLookup = new KdTree<>();
		itemGroupTileLookup = new HashMap<>();
		
		globalTileBlockLookup = new HashMap<>();
		buildingLookup = new KdTree<>();
		tileIdCounts = new HashMap<>();
		accessibleTileRecord = new LocalGridAccess[rows][cols][heights];
		
		buildIdQuickTileLookup = new HashMap<>();
		
		peopleLookup = new KdTree<>();
		
		localLandClaims = new ArrayList<>();
	}
	
	/**
	 * @return The tile at coords (r,c,h), null if the coord is out of bounds.
	 */
	public LocalTile getTile(Vector3i coords) {
		int r = coords.x, c = coords.y, h = coords.z;
		if (!inBounds(coords))
			return null;
		return grid[r][c][h];
	}
	
	public boolean inBounds(Vector3i coords) {
		int r = coords.x, c = coords.y, h = coords.z;
		return !(r < 0 || c < 0 || h < 0 || r >= rows || c >= cols || h >= heights);
	}
	
	public boolean tileIsOccupied(Vector3i coords) {
		LocalTile tile = getTile(coords);
		if (tile == null) return false;
		if (tile.tileBlockId != ItemData.ITEM_EMPTY_ID) {
			if (ItemData.doesItemIdHaveProp(tile.tileBlockId, "Walkthrough")) {
				//TODO Better treatment of this item property here
				return false;
			}
			return true;
		}
		//if (tile.getPeople() != null) return true;
		if (tile.building != null) {
			if (tile.building.calculatedLocations != null) {
				return tile.building.calculatedLocations.contains(coords);
			}
		}
		return false;
	}
	
	/**
	 * Precondition of this method: tile access records must be updated with
	 * LocalGrid::updateAllTilesAccessInit();
	 */
	private LocalGridAccess tileIsFullyAccessible(Vector3i coords) {
		if (!this.inBounds(coords))
			return LocalGridAccess.NO_ACCESS;
		return this.accessibleTileRecord[coords.x][coords.y][coords.z];
	}
	public boolean tileIsFullAccessible(Vector3i coords) {
		if (!this.inBounds(coords))
			return false;
		return this.accessibleTileRecord[coords.x][coords.y][coords.z] == LocalGridAccess.FULL_ACCESS;
	}
	public boolean tileIsPartAccessible(Vector3i coords) {
		if (!this.inBounds(coords))
			return false;
		LocalGridAccess access = this.accessibleTileRecord[coords.x][coords.y][coords.z];
		return access == LocalGridAccess.FULL_ACCESS || access == LocalGridAccess.IF_HARVEST_ACCESS;
	}
	
	private LocalGridAccess determineTileIsAccessible(Vector3i coords) {
		LocalTile tile = getTile(coords);
		
		if (!this.inBounds(coords))
			return LocalGridAccess.NO_ACCESS;
		
		Vector3i belowCoords = new Vector3i(coords.x, coords.y, coords.z - 1);
		
		if (tile == null) {
			if (inBounds(belowCoords)) {
				return tileIsOccupied(belowCoords) ? LocalGridAccess.FULL_ACCESS : LocalGridAccess.NO_ACCESS;
			}
			else {
				return LocalGridAccess.NO_ACCESS;
			}
		}
		
		if (tile.tileBlockId != ItemData.ITEM_EMPTY_ID) {
			//Check for not traversable blocks, i.e. air, water, etc.
			return LocalGridAccess.IF_HARVEST_ACCESS;
		}
		if (tile.building != null) {
			if (tile.building.calculatedLocations != null) {
				if (tile.building.calculatedLocations.contains(coords)) {
					return LocalGridAccess.NO_ACCESS;
				}
			}
		}
		if (tile.tileFloorId != ItemData.ITEM_EMPTY_ID) {
			return LocalGridAccess.FULL_ACCESS;
		}
		
		if (inBounds(belowCoords)) {
			return tileIsOccupied(belowCoords) ? LocalGridAccess.FULL_ACCESS : LocalGridAccess.NO_ACCESS;
		}
		else {
			return LocalGridAccess.NO_ACCESS;
		}
	}
	
	private void updateTileAccessibility(Vector3i coords) {
		if (!this.inBounds(coords)) {
			throw new IllegalArgumentException("Cannot update tile access info of block over level 50");
		}
		this.accessibleTileRecord[coords.x][coords.y][coords.z] = determineTileIsAccessible(coords);
	}
	public void updateAllTilesAccessInit() {
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				for (int h = 0; h < heights; h++) {
					updateTileAccessibility(new Vector3i(r,c,h));
				}
			}
		}
	}
	
	public Vector3i getRandomNearAccessTile(Vector3i base, int squareDist) {
		int trials = 15;
		Vector3i target = null;
		do {
			trials--;
			int dr = (int) (Math.random() * squareDist * 2) - squareDist;
			int dc = (int) (Math.random() * squareDist * 2) - squareDist;
			target = new Vector3i(base.x + dr, base.y + dc, base.z);
			if (!inBounds(target)) {
				continue;
			}
			target.z = this.findHighestEmptyHeight(target.x, target.y);
		} while (tileIsFullyAccessible(target) == LocalGridAccess.NO_ACCESS && trials > 0);
		return target;
	}
	
	private Set<Vector3i> getAllNeighbors(Vector3i coords, Set<Vector3i> vecNeighbors) {
		Set<Vector3i> candidates = new HashSet<>();
		for (Vector3i adjOffset: vecNeighbors) {
			Vector3i neighbor = coords.getSum(adjOffset);
			if (inBounds(neighbor)) {
				candidates.add(neighbor);
			}
		}
		return candidates;
	}
	
	public static final Set<Vector3i> directAdjOffsets6 = new HashSet<Vector3i>() {
		{add(new Vector3i(1,0,0)); add(new Vector3i(-1,0,0)); add(new Vector3i(0,1,0));
			add(new Vector3i(0,-1,0)); add(new Vector3i(0,0,1)); add(new Vector3i(0,0,-1));}};
	public Set<Vector3i> getAllNeighbors6(Vector3i coords) {
		return getAllNeighbors(coords, directAdjOffsets6);
	}
	
	public static final Set<Vector3i> allAdjOffsets8 = new HashSet<Vector3i>() {
		{add(new Vector3i(1,0,0)); add(new Vector3i(-1,0,0)); add(new Vector3i(0,1,0)); add(new Vector3i(0,-1,0)); 
		add(new Vector3i(1,1,0)); add(new Vector3i(1,-1,0)); add(new Vector3i(-1,-1,0)); add(new Vector3i(-1,1,0));}};
	public Set<Vector3i> getAllNeighbors8(Vector3i coords) {
		return getAllNeighbors(coords, allAdjOffsets8);
	}
	
	public static final Set<Vector3i> allAdjOffsets14 = new HashSet<Vector3i>() {
		{add(new Vector3i(1,0,0)); add(new Vector3i(-1,0,0)); add(new Vector3i(0,1,0));
			add(new Vector3i(0,-1,0)); add(new Vector3i(0,0,1)); add(new Vector3i(0,0,-1));
			
			add(new Vector3i(1,0,1)); add(new Vector3i(-1,0,1)); 
			add(new Vector3i(0,1,1)); add(new Vector3i(0,-1,1));
			
			add(new Vector3i(1,0,-1)); add(new Vector3i(-1,0,-1)); 
			add(new Vector3i(0,1,-1)); add(new Vector3i(0,-1,-1));
		}};
	public Set<Vector3i> getAllNeighbors14(Vector3i coords) {
		return getAllNeighbors(coords, allAdjOffsets14);
	}
	public Set<LocalTile> getAllTiles14Pathfinding(Vector3i coords) {
		Set<Vector3i> vecs = getAllNeighbors(coords, allAdjOffsets14);
		Set<LocalTile> tiles = new HashSet<>();
		for (Vector3i vec: vecs) {
			LocalTile tile = this.getTile(vec);
			if (tile != null) {
				tiles.add(tile);
			}
		}
		return tiles;
	}
	
	public Set<LocalTile> getAccessibleNeighbors(LocalTile tile) {
		Set<Vector3i> neighbors = this.getAllNeighbors14(tile.coords);
		Set<LocalTile> candidateTiles = new HashSet<>();
		for (Vector3i neighbor: neighbors) {
			if (this.tileIsFullyAccessible(neighbor) != LocalGridAccess.NO_ACCESS) {
				LocalTile neighborTile = getTile(neighbor);
				if (neighborTile == null) {
					neighborTile = createTile(neighbor);
				}
				candidateTiles.add(neighborTile);
			}
		}
		return candidateTiles;
	}
	//Conditional access to local tiles based on actor biology
	//public Set<LocalTile> getAccessibleNeighbors(LocalTile tile, LivingEntity being)
	
	public static final Set<Vector3i> allVertAdjOffsets26 = new HashSet<Vector3i>() {{
		add(new Vector3i(1,0,0)); add(new Vector3i(-1,0,0)); add(new Vector3i(0,1,0)); add(new Vector3i(0,-1,0)); 
		add(new Vector3i(1,1,0)); add(new Vector3i(1,-1,0)); add(new Vector3i(-1,-1,0)); add(new Vector3i(-1,1,0));
		add(new Vector3i(1,0,1)); add(new Vector3i(-1,0,1)); add(new Vector3i(0,1,1)); add(new Vector3i(0,-1,1)); 
		add(new Vector3i(1,1,1)); add(new Vector3i(1,-1,1)); add(new Vector3i(-1,-1,1)); add(new Vector3i(-1,1,1));
		add(new Vector3i(1,0,-1)); add(new Vector3i(-1,0,-1)); add(new Vector3i(0,1,-1)); add(new Vector3i(0,-1,-1)); 
		add(new Vector3i(1,1,-1)); add(new Vector3i(1,-1,-1)); add(new Vector3i(-1,-1,-1)); add(new Vector3i(-1,1,-1));
		add(new Vector3i(0,0,1)); add(new Vector3i(0,0,-1));
		}};
	public Set<Vector3i> getAllNeighbors26(Vector3i coords) {
		return getAllNeighbors(coords, allVertAdjOffsets26);
	}
	public void markAllAdjAsExposed(Vector3i coords) {
		Set<Vector3i> neighbors = this.getAllNeighbors26(coords);
		neighbors.add(coords);
		for (Vector3i neighbor: neighbors) {
			LocalTile tile = getTile(neighbor);
			if (tile != null) {
				tile.exposedToAir = true;
			}
		}
	}
	
	
	//Find all 14-neighbords of coords, sorted in ascending distance to target
	public List<Vector3i> getAll14NeighborsSorted(Vector3i coords, Vector3i target) {
		List<Vector3i> vecs = new ArrayList<>(this.getAllNeighbors14(coords));
		Collections.sort(vecs, new VecGridUtil.VecDistComp(target));
		return vecs;
	}
	
	public Set<Vector3i> squareSpiralTiles(Vector3i coords, int radius) {
		Set<Vector3i> results = new HashSet<>();
		for (int r = -radius; r <= +radius; r++) {
			Vector3i offsetA = new Vector3i(r, -radius, 0);
			Vector3i offsetB = new Vector3i(r, +radius, 0);
			results.add(coords.getSum(offsetA));
			results.add(coords.getSum(offsetB));
		}
		for (int c = -radius; c <= +radius; c++) {
			Vector3i offsetA = new Vector3i(-radius, -c, 0);
			Vector3i offsetB = new Vector3i(+radius, +c, 0);
			results.add(coords.getSum(offsetA));
			results.add(coords.getSum(offsetB));
		}
		return results;
	}
	
	public Set<Vector3i> rangeLargeSquareTiles(Vector3i coords, 
			int startRadiusInc, int endRadiusInc) {
		Set<Vector3i> allRings = new HashSet<Vector3i>() {{
			for (int i = startRadiusInc; i <= endRadiusInc; i++) {
				addAll(squareSpiralTiles(coords, i));
			}
		}};
		return allRings;
	}
	
	/**
	 * Used only for world creation, to create individual tiles
	 * @param coords
	 * @param tile
	 */
	public void setTileInstantiate(Vector3i coords, LocalTile tile) {
		int r = coords.x, c = coords.y, h = coords.z;
		if (r < 0 || c < 0 || h < 0 || r >= rows || c >= cols || h >= heights)
			throw new IllegalArgumentException("Invalid tile added to coords: " + coords.toString());
		grid[r][c][h] = tile;
		
		if (!globalTileBlockLookup.containsKey(tile.tileBlockId)) {
			globalTileBlockLookup.put(tile.tileBlockId, new KdTree<>());
		}
		globalTileBlockLookup.get(tile.tileBlockId).add(new Vector3i(r,c,h));
		
		//this.putBlockIntoTile(coords, tile == null ? ItemData.ITEM_EMPTY_ID : tile.tileBlockId);
	}
	
	public LocalTile createTile(Vector3i coords) {
		if (inBounds(coords)) {
			LocalTile tile = new LocalTile(coords);
			grid[coords.x][coords.y][coords.z] = tile;
			//this.putBlockIntoTile(coords, tile == null ? ItemData.ITEM_EMPTY_ID : tile.tileBlockId);
			return tile;
		}
		return null;
	}
	
	public void putBlockIntoTile(Vector3i coords, int newBlockId) {
		if (!ItemData.isPlaceable(newBlockId)) {
			throw new IllegalArgumentException("Cannot place down block id: " + newBlockId);
		}
		if (!this.inBounds(coords)) {
			throw new IllegalArgumentException("Invalid tile added to coords: " + coords.toString());
		}
		
		LocalTile tile = getTile(coords);
		int oldItemId;
		if (tile == null) {
			oldItemId = ItemData.ITEM_EMPTY_ID;
			tile = this.createTile(coords);
		}
		else {
			oldItemId = tile.tileBlockId;
		}
		
		if (tile != null) {
			tile.tileBlockId = newBlockId;
			if (tile.tileBlockId != ItemData.ITEM_EMPTY_ID) {
				if (!(globalTileBlockLookup.containsKey(newBlockId))) {
					globalTileBlockLookup.put(newBlockId, new KdTree<Vector3i>());
				}
				globalTileBlockLookup.get(newBlockId).add(coords);
				if (oldItemId != ItemData.ITEM_EMPTY_ID)
					MapUtil.addNumMap(this.tileIdCounts, oldItemId, -1);
				MapUtil.addNumMap(this.tileIdCounts, newBlockId, 1);
			}
			else {
				if (oldItemId != ItemData.ITEM_EMPTY_ID) {
					if (globalTileBlockLookup.containsKey(oldItemId)) {
						globalTileBlockLookup.get(oldItemId).remove(coords);
					}
				}
				markAllAdjAsExposed(coords);
			}
			updateClusterInfo(coords, oldItemId, newBlockId);
		}
		
		//Do not create/modify RSR pathfinding nodes while world is being created
		if (this.pathfinder != null) {
			this.pathfinder.adjustRSR(coords, oldItemId != ItemData.ITEM_EMPTY_ID);
		}
		
		updateTileAccessibility(coords);
	}
	
	/**
	 * Update the 2d and 3d memotized cluster data to maintain correct records of clusters
	 * 
	 * The first case handles the disappearance of the block, which merges all adjacent 2d and 3d clusters.
	 * The second case handles a new placed block, which creates one or more split clusters.
	 * 
	 * @param coords
	 * @param oldItemId
	 * @param newItemId
	 */
	private void updateClusterInfo(Vector3i coords, int oldItemId, int newItemId) {
		if (oldItemId == newItemId) return;
		if (this.connectedCompsMap == null) return; //Cluster data not initialized yet
		
		Set<Vector3i> neighbors14 = this.getAllNeighbors14(coords);
		Set<Vector3i> neighbors8 = this.getAllNeighbors8(coords);
		
		if (newItemId == ItemData.ITEM_EMPTY_ID) {
			Map<Integer, Vector3i> firstVecInCluster = new HashMap<>();
			Integer minComponentIndex = null;
			
			for (Vector3i neighbor: neighbors14) {
				if (!this.connectedCompsMap.containsKey(neighbor)) {
					continue;
				}
				int componentNum = this.connectedCompsMap.get(neighbor);
				if (!firstVecInCluster.containsKey(componentNum)) {
					firstVecInCluster.put(componentNum, neighbor);
					if (minComponentIndex == null || componentNum < minComponentIndex) {
						minComponentIndex = componentNum;
					}
				}
			}
			
			//System.err.println(coords);
			//System.err.println(firstVecInCluster);
			
			//Join the 3d clusters
			if (firstVecInCluster.size() > 0) {
				//Gather all vectors for the new joined together cluster
				Set<ClusterVector3i> clustersToRemove = new HashSet<>();
				
				//CustomLog.errPrintln(this.clustersList3d);
				
				for (Entry<Integer, Vector3i> vecEntry: firstVecInCluster.entrySet()) {
					for (ClusterVector3i cluster: this.clustersList3d) {
						if (cluster.clusterData.contains(vecEntry.getValue())) {
							clustersToRemove.add(cluster);
							break;
						}
					}
				}
				
				//System.err.println(clustersToRemove);
				
				Vector3i newRandomVec = null;
				Set<Vector3i> newClusterVecs = new HashSet<>();
				for (ClusterVector3i cluster: clustersToRemove) {
					newClusterVecs.addAll(cluster.clusterData);
					if (newRandomVec == null) {
						//System.err.println("Added cluster vec: " + cluster.center);
						newRandomVec = cluster.center;
					}
					//Change map number index
					for (Vector3i vec: cluster.clusterData) {
						this.connectedCompsMap.put(vec, minComponentIndex);
					}
					//Remove cluster from all clusters
					this.clustersList3d.remove(cluster);
				}
				ClusterVector3i newCluster = new ClusterVector3i(newRandomVec, newClusterVecs);
				this.clustersList3d.add(newCluster);
			}
			
			firstVecInCluster = new HashMap<>();
			minComponentIndex = null;
			for (Vector3i neighbor: neighbors8) {
				if (!this.connectedCompsMap2d.containsKey(neighbor)) {
					continue;
				}
				int componentNum = this.connectedCompsMap2d.get(neighbor);
				if (!firstVecInCluster.containsKey(componentNum)) {
					firstVecInCluster.put(componentNum, neighbor);
					if (minComponentIndex == null || componentNum < minComponentIndex) {
						minComponentIndex = componentNum;
					}
				}
			}
			//Join the 2d clusters
			if (firstVecInCluster.size() > 0) {
				//Do the same for the 2d surfaces
				//Gather all vectors for the new joined together cluster
				Set<ClusterVector3i> clustersToRemove = new HashSet<>();
				for (ClusterVector3i cluster: this.clustersListFlat2dSurfaces) {
					for (Entry<Integer, Vector3i> vecEntry: firstVecInCluster.entrySet()) {
						if (cluster.clusterData.contains(vecEntry.getValue())) {
							clustersToRemove.add(cluster);
						}
					}
				}
				Vector3i newRandomVec = null;
				Set<Vector3i> newClusterVecs = new HashSet<>();
				for (ClusterVector3i cluster: clustersToRemove) {
					newClusterVecs.addAll(cluster.clusterData);
					if (newRandomVec == null) {
						newRandomVec = cluster.center;
					}
					//Change map number index
					for (Vector3i vec: cluster.clusterData) {
						this.connectedCompsMap.put(vec, minComponentIndex);
					}
					//Remove cluster from all clusters
					this.clustersListFlat2dSurfaces.remove(cluster);
				}
				ClusterVector3i cluster = new ClusterVector3i(newRandomVec, newClusterVecs);
				this.clustersListFlat2dSurfaces.add(cluster);
			}
		}
		else {
			ClusterVector3i clusterToRemove = null;
			for (ClusterVector3i cluster: this.clustersList3d) {
				if (cluster.clusterData.contains(coords)) {
					clusterToRemove = cluster;
					break;
				}
			}
			this.clustersList3d.remove(clusterToRemove);
			
			//Replace the cluster with one or more clusters, which is possible if a cluster becomes separated
			//from itself i.e. there is no path from some tile A -> B
			List<ClusterVector3i> foundClusters = SpaceFillingAlg.findAllComponents(
					this, neighbors14, NeighborMode.ADJ_14);
			for (ClusterVector3i cluster: foundClusters) {
				for (Vector3i vec: cluster.clusterData) {
					this.connectedCompsMap.put(vec, compNumCounter);
				}
				compNumCounter++;
				this.clustersList3d.add(cluster);
			}
			
			
			//Do the same for 2d clusters
			List<ClusterVector3i> foundClusters8 = SpaceFillingAlg.findAllComponents(
					this, neighbors8, NeighborMode.ADJ_8);
			
			for (ClusterVector3i cluster: foundClusters8) {
				for (Vector3i vec: cluster.clusterData) {
					this.connectedCompsMap2d.put(vec, compNumCounter);
				}
				compNumCounter++;
				this.clustersListFlat2dSurfaces.add(cluster);
			}
		}
	}
	
	public void initClustersData() {
		List<ClusterVector3i> clusters = VecGridUtil.contComp3dSolidsClustersSpec(null, null, this);
		this.clustersList3d = new KdTree(clusters);
		this.connectedCompsMap = VecGridUtil.convertGroupsToMap(clusters);
		this.compNumCounter = clusters.size();
		
		List<ClusterVector3i> clusters2d = SpaceFillingAlg.allFlatSurfaceContTiles(this, false);
		this.clustersListFlat2dSurfaces = new KdTree(clusters2d);
		this.connectedCompsMap2d = VecGridUtil.convertGroupsToMap(clusters2d);
	}
	
	public ClusterVector3i find3dClusterWithCoord(Vector3i coords) {
		for (ClusterVector3i cluster: this.clustersList3d) {
			if (cluster.clusterData.contains(coords)) {
				return cluster;
			}
		}
		return null;
	}
	
	public void addItemRecordsToWorld(Vector3i coords, List<InventoryItem> items) {
		for (InventoryItem item: items) {
			addItemRecordToWorld(coords, item);
		}
	}
	
	public void addItemRecordToWorld(Vector3i coords, InventoryItem item) {
		if (!(itemIdQuickTileLookup.containsKey(item.itemId))) {
			itemIdQuickTileLookup.put(item.itemId, new KdTree<Vector3i>());
		}
		itemIdQuickTileLookup.get(item.itemId).add(coords);
		
		Set<String> itemGroups = ItemData.getGroupNameById(item.itemId);
		if (itemGroups != null) {
			for (String itemGroup: itemGroups) {
				if (!(itemGroupTileLookup.containsKey(itemGroup))) {
					itemGroupTileLookup.put(itemGroup, new KdTree<Vector3i>());
				}
				itemGroupTileLookup.get(itemGroup).add(coords);
			}
		}
		
		globalItemsLookup.add(coords);
	}
	
	public void removeItemRecordToWorld(Vector3i coords, InventoryItem item) {
		LocalTile tile = getTile(coords);
		
		if (itemIdQuickTileLookup.containsKey(item.itemId)) {
			itemIdQuickTileLookup.get(item.itemId).remove(coords);
			if (tile.itemsOnFloor.size() == 0 && 
					tile.building == null && tile.building.inventory.hasItem(item)) {
				globalItemsLookup.remove(coords);
			}
		}
	
		Set<String> itemGroups = ItemData.getGroupNameById(item.itemId);
		for (String itemGroup: itemGroups) {
			if (itemGroupTileLookup.containsKey(itemGroup)) {
				itemGroupTileLookup.get(itemGroup).remove(coords);
				if (itemGroupTileLookup.get(itemGroup).size() == 0) {
					itemGroupTileLookup.remove(itemGroup);
				}
			}
		}
	}
	
	public boolean pickupItemAtTile(Vector3i coords, InventoryItem item) {
		LocalTile tile = getTile(coords);
		if (tile != null) {
			if (tile.itemsOnFloor.hasItem(item)) {
				tile.itemsOnFloor.subtractItem(item);
				globalItemsLookup.remove(coords);
				if (itemIdQuickTileLookup.containsKey(item.itemId)) {
					KdTree<Vector3i> coordRecords = itemIdQuickTileLookup.get(item.itemId);
					coordRecords.remove(coords);
					if (coordRecords.size() == 0) {
						itemIdQuickTileLookup.remove(item.itemId);
					}
				}
				return true;
			}
		}
		return false;
	}
	
	public void addOwnershipItem(LivingEntity being, InventoryItem item) {
		being.ownedItems.add(item);
		item.owner = being;
	}
	
	public void addOwnershipBuilding(LivingEntity being, LocalBuilding building) {
		being.ownedBuildings.add(building);
		building.owner = being;
	}
	
	public KdTree<Vector3i> getKdTreeForItemId(Integer itemId) {
		return this.itemIdQuickTileLookup.get(itemId);
	}
	
	public KdTree<Vector3i> getKdTreeForTile(Integer itemId) {
		return this.globalTileBlockLookup.get(itemId);
	}
	
	public KdTree<Vector3i> getKdTreeForItemGroup(String groupName) {
		if (ItemData.isGroup(groupName))
			return this.itemGroupTileLookup.get(groupName);
		return this.getKdTreeForTile(ItemData.getIdFromName(groupName));
	}
	
	public KdTree<Vector3i> getKdTreeForBuildings(Integer buildId) {
		return this.buildIdQuickTileLookup.get(buildId);
	}
	
	/**
	 * For information only, no modification of this data set
	 */
	public Map<Integer, KdTree<Vector3i>> getInfoMapBuildings() {
		return this.buildIdQuickTileLookup;
	}
	
	public boolean buildingWithinBounds(LocalBuilding building, Vector3i newPrimaryCoords) {
		List<Vector3i> offsets = building.getLocationOffsets();
		for (Vector3i offset: offsets) {
			Vector3i candidate = newPrimaryCoords.getSum(offset);
			if (!inBounds(candidate)) {
				return false;
			}
		}
		return true;
	}
	
	public boolean buildingCanFitAt(LocalBuilding building, Vector3i newPrimaryCoords) {
		if (!buildingWithinBounds(building, newPrimaryCoords)) {
			return false;
		}
		//Check to see if the building fits
		List<Vector3i> offsets = building.getLocationOffsets();
		for (Vector3i offset: offsets) {
			Vector3i candidate = newPrimaryCoords.getSum(offset);
			LocalTile candidateTile = this.getTile(candidate);
			if (candidateTile != null && candidateTile.building != null) {
				return false;
			}
		}
		return true;
	}
	
	public void addBuilding(LocalBuilding building, Vector3i newPrimaryCoords, boolean overrideGrid,
			LivingEntity owner) {
		if (buildingWithinBounds(building, newPrimaryCoords) && 
				(buildingCanFitAt(building, newPrimaryCoords) || overrideGrid)) {
			removeBuilding(building);
			building.setPrimaryLocation(newPrimaryCoords);
			Collection<Vector3i> newAbsLocations = building.calculatedLocations;

			for (Vector3i newAbsLocation: newAbsLocations) {
				LocalTile absTile = getTile(newAbsLocation);
				int oldBuildingId = (absTile != null && absTile.building != null) ? 
						absTile.building.buildingId : ItemData.ITEM_EMPTY_ID;
				
				if (absTile == null && this.inBounds(newAbsLocation)) {
					absTile = createTile(newAbsLocation);
					
					absTile.building = building;
					//absTile.tileBlockId = building.buildingBlockIds.get(buildingTileIndex);
					
					//Update pathfinder correctly, use neighboring tiles
					if (this.pathfinder != null)
						this.pathfinder.adjustRSR(newAbsLocation, true);
				}
				
				if (this.inBounds(newAbsLocation)) {
					updateClusterInfo(newAbsLocation, oldBuildingId, building.buildingId);
					updateTileAccessibility(newAbsLocation);
				}
			} 
			
			allBuildings.add(building);
			buildingLookup.add(newPrimaryCoords);
			if (!buildIdQuickTileLookup.containsKey(building.buildingId)) {
				buildIdQuickTileLookup.put(building.buildingId, new KdTree<>());
			}
			buildIdQuickTileLookup.get(building.buildingId).add(building.getPrimaryLocation());
			markAllAdjAsExposed(newPrimaryCoords);
			
			if (owner != null) {
				building.owner = owner;
				owner.ownedBuildings.add(building);
			}
		}
	}
	
	public void removeBuilding(LocalBuilding building) {
		Collection<Vector3i> oldAbsLocations = building.calculatedLocations;
		for (Vector3i oldAbsLocation: oldAbsLocations) {
			LocalTile oldTile = getTile(oldAbsLocation);
			if (oldTile != null) {
				oldTile.building = null;
				oldTile.tileBlockId = ItemData.ITEM_EMPTY_ID;
			}
			if (this.pathfinder != null)
				this.pathfinder.adjustRSR(oldAbsLocation, false);
			
			if (this.inBounds(oldAbsLocation))
				updateTileAccessibility(oldAbsLocation);
		}
		buildingLookup.remove(building.getPrimaryLocation());
		
		if (buildIdQuickTileLookup.containsKey(building.buildingId)) {
			buildIdQuickTileLookup.get(building.buildingId).remove(building.getPrimaryLocation());
			if (buildIdQuickTileLookup.get(building.buildingId).size() == 0) {
				buildIdQuickTileLookup.remove(building.buildingId);
			}
		}
		
		building.setPrimaryLocation(null);
		allBuildings.remove(building);
	}
	
	public Set<LocalBuilding> getAllBuildings() {
		return allBuildings;
	}
	
	public Collection<Vector3i> getNearestBuildings(Vector3i coords) {
		return this.buildingLookup.nearestNeighbourListSearch(10, coords);
	}
	
	public Collection<Vector3i> getNearestPeopleCoords(Vector3i coords) {
		return this.peopleLookup.nearestNeighbourListSearch(10, coords);
	}
	
	public Collection<LivingEntity> getNearestPeopleList(Vector3i coords) {
		Collection<LivingEntity> allPeople = new ArrayList<>();
		Collection<Vector3i> coordsList = this.peopleLookup.nearestNeighbourListSearch(10, coords);
		for (Vector3i personLoc: coordsList) {
			Collection<LivingEntity> people = this.getTile(personLoc).getPeople();
			for (LivingEntity person: people) {
				allPeople.add(person);
			}
		}
		return allPeople;
	}
	
	public Set<LivingEntity> getAllLivingBeings() {
		return this.allLivingBeings;
	}
	
	//TODO; Implement these heuristics in the generalized scoring metric functions
	/**
	 * 
	 * @param coords The desired coordinates to look for nearest available rooms
	 * @param requiredSpace
	 * @return An array of two vectors, representing the 3d coords of the top left corner (min r and min c)
	 * of the bounds, and then 2d rectangular dimensions of the bounds.
	 */
	/*
	public Object[] getNearestViableRoom(Set<Human> validLandOwners, Vector3i coords, Vector2i requiredSpace) {
		Collection<Vector3i> nearestBuildingCoords = this.buildingLookup.nearestNeighbourListSearch(50, coords);
		for (Vector3i nearestBuildingCoord: nearestBuildingCoords) {
			LocalBuilding building = getTile(nearestBuildingCoord).building;
			if (building != null) {
				Set<Vector3i> emptySpace = getFreeSpace(building);
				if (emptySpace.size() > requiredSpace.x * requiredSpace.y) { //Preliminary check for minimum bounding box
					int height = emptySpace.iterator().next().z;
					int[] boundsData = VecGridUtil.findBestRect(emptySpace, requiredSpace.x, requiredSpace.y);
					Vector3i nearOpenSpace = new Vector3i(boundsData[0], boundsData[1], height);
					Vector2i bounds2d = new Vector2i(boundsData[2], boundsData[3]);
					if (checkIfUseRoomSpace(nearOpenSpace, bounds2d) && 
							bounds2d.x >= requiredSpace.x && bounds2d.y >= requiredSpace.y) { 
						//Actual check for space within
						return new Object[] {nearOpenSpace, bounds2d};
					}
				}
			}
		}
		return null;
	}
	*/
	public GridRectInterval getNearestViableRoom(Society society, Set<Human> validLandOwners, Vector3i coords, 
			Vector2i requiredSpace) {
		GridRectInterval bestAvailSpace = SpaceFillingAlg.findAvailSpaceCloseFactorClaims(this, coords,
				requiredSpace.x, requiredSpace.y, true, 
				society, validLandOwners, false, false, new LocalTileCond.IsBuildingTileCond(), null);
		return bestAvailSpace;
	}
	
	public boolean checkIfUseRoomSpace(Vector3i nearOpenSpace, Vector2i bounds2d) {
		for (int r = nearOpenSpace.x; r < nearOpenSpace.x + bounds2d.x; r++) {
			for (int c = nearOpenSpace.y; c < nearOpenSpace.y + bounds2d.y; c++) {
				LocalTile tile = getTile(new Vector3i(r,c,nearOpenSpace.z));
				if (tile != null) {
					if (tile.harvestInUse) return true;
				}
			}
		}
		return false;
	}
	
	public void setInUseRoomSpace(GridRectInterval interval, boolean inUse) {
		setInUseRoomSpace(
				interval.getStart(), 
				interval.getEnd().getSubtractedBy(interval.getStart()).getSum(1, 1, 1).getXY(), 
				true
		);
	}
	public void setInUseRoomSpace(Vector3i nearOpenSpace, Vector2i bounds2d, boolean inUse) {
		for (int r = nearOpenSpace.x; r < nearOpenSpace.x + bounds2d.x; r++) {
			for (int c = nearOpenSpace.y; c < nearOpenSpace.y + bounds2d.y; c++) {
				LocalTile tile = getTile(new Vector3i(r,c,nearOpenSpace.z));
				tile.harvestInUse = inUse;
			}
		}
	}
	public void setInUseRoomSpace(Collection<Vector3i> coords, boolean inUse) {
		for (Vector3i coord: coords) {
			if (!this.inBounds(coord)) {
				throw new IllegalArgumentException("Could not set in use grid coords: " + coord + 
						" of collection of coords...: " + coords.toString());
			}
			
			LocalTile tile = getTile(coord);
			if (tile == null) {
				tile = createTile(coord);
			}
			tile.harvestInUse = inUse;
		}
	}
	
	public Set<Vector3i> getFreeSpace(LocalBuilding building) {
		Set<Vector3i> emptySpaces = new HashSet<>();
		for (Vector3i location: building.calculatedLocations) {
			if (this.tileIsFullyAccessible(location) == LocalGridAccess.FULL_ACCESS) {
				emptySpaces.add(location);
			}
		}
		return emptySpaces;
	}
	
	public void addLivingEntity(LivingEntity human, Vector3i coords) {
		LocalTile tile = getTile(coords);
		
		if (human.location != null) {
			human.location.removePerson(human);
			if (human.location.getPeople().size() == 0) {
				peopleLookup.remove(human.location.coords);
				allLivingBeings.remove(human);
			}
			human.location = null;
		}
		
		if (tile == null) {
			tile = createTile(coords);
		}
		if (tile != null) {
			human.location = tile;
			tile.addPerson(human);
			markAllAdjAsExposed(coords);
			peopleLookup.add(coords);
			allLivingBeings.add(human);
		}
	}
	
	public void movePerson(LivingEntity person, LocalTile tile) {
		if (person.location != null) {
			person.location.removePerson(person);
			if (person.location.getPeople().size() == 0) {
				this.peopleLookup.remove(person.location.coords);
			}
		}
		person.location = tile;
		if (person.location != null) {
			person.location.addPerson(person);
			markAllAdjAsExposed(tile.coords);
			peopleLookup.add(tile.coords);
		}
	}
	
	public boolean personHasAccessTile(LivingEntity person, LocalTile tile) {
		return true;
	}
	
	public int findHighestEmptyHeight(int r, int c) {
		for (int h = heights - 1; h >= 0; h--) {
			if (getTile(new Vector3i(r,c,h)) == null) {
				continue;
			}
			else {
				return h+1;
			}
		}
		return 0;
	}
	
	//See LocalGrid::tileIsAccessible(...)
	public Vector3i findLowestAccessibleHeight(int r, int c) {
		for (int h = 0; h < heights; h++) {
			Vector3i coords = new Vector3i(r,c,h);
			if (this.tileIsFullyAccessible(coords) == LocalGridAccess.FULL_ACCESS) {
				return coords;
			}
		}
		return null;
	}
	
	//See LocalGrid::tileIsAccessible(...)
	public Vector3i findHighestAccessibleHeight(int r, int c) {
		for (int h = heights - 1; h >= 0; h--) {
			Vector3i coords = new Vector3i(r,c,h);
			if (this.tileIsFullyAccessible(coords) == LocalGridAccess.FULL_ACCESS) {
				return coords;
			}
		}
		return null;
	}
	
	//Find an empty height at lowest possible level, i.e. an empty tile with ground right below it,
	//so that a Human can be placed.
	public int findHighestGroundHeight(int r, int c) {
		//Set<Integer> groundGroup = ItemData.getGroupIds("Ground");
		for (int h = heights - 1; h >= 0; h--) {
			if (this.tileIsFullyAccessible(new Vector3i(r, c, h)) == LocalGridAccess.FULL_ACCESS) {
				return h;
			}
		}
		return heights - 1;
	}
	
	public double averageBeauty(Vector3i coords) {
		Set<Vector3i> spiralCoords = rangeLargeSquareTiles(coords, 0, 4);
		double sumBeauty = 0;
		int numWeights = 0;
		final double itemWeighting = 0.25;
		
		for (Vector3i spiralCoord: spiralCoords) {
			int groundHeight = this.findHighestGroundHeight(spiralCoord.x, spiralCoord.y);
			int emptyHeight = this.findHighestEmptyHeight(spiralCoord.x, spiralCoord.y);
			if (emptyHeight != spiralCoord.z) {
				spiralCoord.z = groundHeight;
			}
			LocalTile tile = getTile(spiralCoord);	
			
			if (tile != null) {
				if (tile.tileBlockId != ItemData.ITEM_EMPTY_ID) {
					double beautyValue = ItemData.getItemBeautyValue(tile.tileBlockId);
					sumBeauty += beautyValue;
					numWeights++;
				}
				if (tile.itemsOnFloor.size() > 0) {
					List<InventoryItem> items = tile.itemsOnFloor.getItems();
					for (InventoryItem item: items) {
						double beautyValue = ItemData.getItemBeautyValue(item.itemId);
						sumBeauty += beautyValue * itemWeighting;
						numWeights += itemWeighting;
					}
				}
			}
		}
		
		if (numWeights == 0) return 0;
		return sumBeauty / numWeights;
	}
	
	public Vector3i getNearOpenSpace(Vector3i coords) {
		int dist = 5;
		
		/*
		boolean[][] occupiedNear = new boolean[dist*2 + 3][dist*2 + 3];
		int[][] heights = new int[dist*2 + 3][dist*2 + 3];
		
		for (int r = -dist*2 - 1; r <= dist*2 + 1; r++) {
			for (int c = -dist*2 - 1; c <= dist*2 + 1; c++) {
				if (inBounds(new Vector3i(r,c,coords.z))) {
					occupiedNear[r][c] = this.tileIsOccupied(new Vector3i(r,c,coords.z));
				}
			}
		}
		*/
		
		for (int r = -dist*2; r <= dist*2; r++) {
			for (int c = -dist*2; c <= dist*2; c++) {
				if (inBounds(new Vector3i(r,c,0))) {
					int h = findHighestGroundHeight(r,c);
					if (this.tileIsFullyAccessible(new Vector3i(r,c,h)) == LocalGridAccess.FULL_ACCESS) {
						return new Vector3i(r,c,h);
					}
				}
			}
		}
		return null;
	}
	
	public void claimTiles(Human human, GridRectInterval interval, LocalProcess purpose) {
		claimTiles(human, interval.getStart(), interval.getEnd(), purpose);
	}
	
	//Claim is equivalent to actual human ownership and full rights, as opposed to being in use
	public void claimTiles(Human human, Vector3i start, Vector3i end, LocalProcess purpose) {
		LocalGridLandClaim newLandClaim = new LocalGridLandClaim(human, start, end, purpose);
		localLandClaims.add(newLandClaim);
		human.allClaims.add(newLandClaim);
		
		Iterator<Vector3i> rangeIter = Vector3i.getRange(newLandClaim.boundary);
		while (rangeIter.hasNext()) {
			Vector3i vec = rangeIter.next();
			if (this.inBounds(vec)) 
				human.society.recordClaimInGrid(human, this, vec);
		}
	}
	
	//Claim is equivalent to actual human ownership and full rights, as opposed to being in use
	public void claimTiles(Human human, Set<Vector3i> vecs, LocalProcess purpose) {
		List<RectangularSolid> solids = VecGridUtil.findMaximalRectSolids(this, vecs); 
		for (RectangularSolid solid: solids) {
			GridRectInterval interval = new GridRectInterval(solid.topLeftCorner, 
					solid.topLeftCorner.getSum(solid.solidDimensions).getSum(-1,-1,-1));
			claimTiles(human, interval.getStart(), interval.getEnd(), purpose);
		}	
	}
	
	public void unclaimTiles(LocalGridLandClaim claim) {
		claim.claimant.allClaims.remove(claim);
		localLandClaims.remove(claim);
		claim.boundary = null;
		claim.claimant = null;
		
		Iterator<Vector3i> rangeIter = Vector3i.getRange(claim.boundary);
		while (rangeIter.hasNext()) {
			Vector3i vec = rangeIter.next();
			if (this.inBounds(vec)) 
				claim.claimant.society.recordClaimInGrid(null, this, vec);
		}
	}
	

	//TODO Different societies claim possibly overlapping tiles in a grid, use these claims
	//as a basis for societal wealth, as well as part of larger border disputes.
	public List<Human> findClaimantToTiles(GridRectInterval gridSpace) {
		List<Human> allClaimants = new ArrayList<>();
		for (LocalGridLandClaim claim: localLandClaims) {
			if (gridSpace.intersectsInterval(claim.boundary)) {
				allClaimants.add(claim.claimant);
			}
		}
		return allClaimants;
	}
	public List<Human> findClaimantToTile(Vector3i coords) {
		return this.findClaimantToTiles(new GridRectInterval(coords, coords));
	}
	
	/**
	 * Print the whole grid blocks in an aesthetically pleasing 3d format in the console,
	 * tabbed to give the illusion of three dimensions (for easy indexing by r,c,h coordinates).
	 * 
	 * Small grids only
	 */
	public String smallGridToString() {
		int numTilesToCheck = this.rows * this.cols * this.heights;
		if (numTilesToCheck > 10000) {
			throw new IllegalArgumentException("Cannot print grid of size: " + 
					new Vector3i(rows, cols, heights) + ", or " + numTilesToCheck + " total entries.");
		}
		
		int lastItemId = ItemData.generateNewItemId("Test Potato");
		int numDigits = (int) Math.ceil(Math.log10(lastItemId));
		DecimalFormat numFormat = new DecimalFormat("0".repeat(numDigits));
		
		String totalString = "";
		for (int h = 0; h < this.heights; h++) {
			for (int r = 0; r < this.rows; r++) {
				int numSpaces = (this.rows - 1 - r) * (numDigits + 1);
				String rowStr = " ".repeat(numSpaces);
				for (int c = 0; c < this.cols; c++) {
					Vector3i coords = new Vector3i(r,c,h);
					int tileId = 0;
					LocalTile tile = this.getTile(coords);
					if (tile != null) {
						tileId = tile.tileBlockId == -1 ? 0 : tile.tileBlockId;
					}
					rowStr += numFormat.format(tileId) + " ";
				}
				rowStr += "\n";
				totalString += rowStr;
			}
			totalString += "\n";
		}
		return totalString;
	}
	
	public static void main(String[] args) {
		WorldCsvParser.init();
		
		LocalGrid grid = new LocalGrid(new Vector3i(200,200,50));
		List<Vector3i> listOffsets = Arrays.asList(new Vector3i[] {
				new Vector3i(0),
				new Vector3i(1,0,10),
				new Vector3i(0,0,10)
		});
		List<Integer> listBlockIds = Arrays.asList(new Integer[] {
				ItemData.getIdFromName("Wooden Artisan"),
				ItemData.getIdFromName("Brick Wall"),
				ItemData.getIdFromName("Anvil")
				});
		LocalBuilding building = new LocalBuilding(10, "Test1", new Vector3i(25, 25, 35), listOffsets, listBlockIds);
		
		grid.addBuilding(building, new Vector3i(25,25,35), true, null);
		
		CustomLog.outPrintln(Arrays.toString(building.calculatedLocations.toArray())); //[25 25 25, 26 25 25, 25 28 25]
		CustomLog.outPrintln(grid.getTile(new Vector3i(26,25,45))); //This tile should have been created
		CustomLog.outPrintln(grid.getTile(new Vector3i(25,25,45)).building.name); //Test1
		
		grid.removeBuilding(building);
		
		CustomLog.outPrintln(grid.getTile(new Vector3i(26,25,45)).building); //null
		
		
		Vector3i smallVec = new Vector3i(6,6,6);
		LocalGridBiome biome = LocalGridBiome.determineAllBiomeBroad(0, 0, 0, 0, 0, 0, 0);
		
		ConstantData.ADVANCED_PATHING = false;
		LocalGrid smallGrid = new LocalGridInstantiate(smallVec, biome).setupGrid();
		
		int mods = (smallVec.x * smallVec.y * smallVec.z) / 3;
		for (int i = 0; i < mods; i++) {
			int r = (int) (Math.random() * smallVec.x);
			int c = (int) (Math.random() * smallVec.y);
			int h = (int) (Math.random() * smallVec.z);
			smallGrid.putBlockIntoTile(new Vector3i(r,c,h), ItemData.ITEM_EMPTY_ID);
		}
		smallGrid.initClustersData();
		
		CustomLog.errPrintln(smallGrid.smallGridToString());
		
		CustomLog.errPrintln("-----------------------");
		CustomLog.errPrintln(smallGrid.clustersList3d);
		CustomLog.errPrintln("-----------------------");
		CustomLog.errPrintln(smallGrid.connectedCompsMap);
		CustomLog.errPrintln("-----------------------");
		
		smallGrid.putBlockIntoTile(new Vector3i(2,2,2), ItemData.ITEM_EMPTY_ID);
		
		CustomLog.errPrintln(smallGrid.connectedCompsMap);
		
		smallGrid.putBlockIntoTile(new Vector3i(2,2,2), ItemData.getIdFromName("Quartz"));
		
		CustomLog.errPrintln(smallGrid.connectedCompsMap);
		
		smallGrid.putBlockIntoTile(new Vector3i(2,2,2), ItemData.ITEM_EMPTY_ID);
	}
	
}
