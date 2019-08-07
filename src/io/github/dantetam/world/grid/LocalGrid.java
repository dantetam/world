package io.github.dantetam.world.grid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import io.github.dantetam.world.items.Inventory;
import io.github.dantetam.world.items.InventoryItem;
import io.github.dantetam.world.life.Human;
import io.github.dantetam.world.life.LivingEntity;
import io.github.dantetam.world.process.LocalProcess;
import kdtreegeo.KdTree;

/**
 * A grid representing one section of the whole world, i.e. a self contained world with its own people, tiles, and so on.
 * @author Dante
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
	private boolean[][][] accessibleTileRecord;
	
	private Map<Integer, KdTree<Vector3i>> buildIdQuickTileLookup;
	
	private KdTree<Vector3i> peopleLookup;
	
	public RSRPathfinder pathfinder;
	
	public List<LocalGridLandClaim> localLandClaims;
	
	//3d connected components
	public Map<Vector3i, Integer> connectedCompsMap; //Mapping of all available vectors to a unique numbered space (a 3d volume)
	public KdTree<ClusterVector3i> clustersList3d;
	
	//2d connected surfaces
	public KdTree<ClusterVector3i> clustersList2dSurfaces;
	
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
		accessibleTileRecord = new boolean[rows][cols][heights];
		
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
		if (tile.tileBlockId != ItemData.ITEM_EMPTY_ID) return true;
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
	public boolean tileIsAccessible(Vector3i coords) {
		if (!this.inBounds(coords))
			return false;
		return this.accessibleTileRecord[coords.x][coords.y][coords.z];
	}
	
	private boolean determineTileIsAccessible(Vector3i coords) {
		LocalTile tile = getTile(coords);
		
		if (!this.inBounds(coords))
			return false;
		
		Vector3i belowCoords = new Vector3i(coords.x, coords.y, coords.z - 1);
		
		if (tile == null) {
			if (inBounds(belowCoords)) {
				return tileIsOccupied(belowCoords);
			}
			else {
				return false;
			}
		}
		
		if (tile.tileBlockId != ItemData.ITEM_EMPTY_ID) return false;
		if (tile.building != null) {
			if (tile.building.calculatedLocations != null) {
				if (tile.building.calculatedLocations.contains(coords)) {
					return false;
				}
			}
		}
		if (tile.tileFloorId != ItemData.ITEM_EMPTY_ID) {
			return true;
		}
		
		if (inBounds(belowCoords)) {
			return tileIsOccupied(belowCoords);
		}
		else {
			return false;
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
	public Set<LocalTile> getAccessibleNeighbors(LocalTile tile) {
		Set<Vector3i> neighbors = this.getAllNeighbors14(tile.coords);
		Set<LocalTile> candidateTiles = new HashSet<>();
		for (Vector3i neighbor: neighbors) {
			if (this.tileIsAccessible(neighbor)) {
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
		int oldItemId = tile.tileBlockId;
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
		}
		
		//Do not create/modify RSR pathfinding nodes while world is being created
		if (this.pathfinder != null) {
			this.pathfinder.adjustRSR(coords, oldItemId != ItemData.ITEM_EMPTY_ID);
		}
		
		updateTileAccessibility(coords);
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
	
	public boolean buildingCanFitAt(LocalBuilding building, Vector3i newPrimaryCoords) {
		//Check to see if the building fits
		List<Vector3i> offsets = building.getLocationOffsets();
		for (Vector3i offset: offsets) {
			Vector3i candidate = newPrimaryCoords.getSum(offset);
			LocalTile candidateTile = this.getTile(candidate);
			if (!inBounds(candidate) || (candidateTile != null && candidateTile.building != null)) {
				return false;
			}
		}
		return true;
	}
	
	public void addBuilding(LocalBuilding building, Vector3i newPrimaryCoords, boolean overrideGrid,
			LivingEntity owner) {
		if (buildingCanFitAt(building, newPrimaryCoords) || overrideGrid) {
			removeBuilding(building);
			building.setPrimaryLocation(newPrimaryCoords);
			Collection<Vector3i> newAbsLocations = building.calculatedLocations;

			for (Vector3i newAbsLocation: newAbsLocations) {
				LocalTile absTile = getTile(newAbsLocation);
				if (absTile == null && this.inBounds(newAbsLocation)) {
					absTile = createTile(newAbsLocation);
					
					absTile.building = building;
					//absTile.tileBlockId = building.buildingBlockIds.get(buildingTileIndex);
					
					//TODO Update pathfinder correctly, use neighboring tiles
					if (this.pathfinder != null)
						this.pathfinder.adjustRSR(newAbsLocation, true);
				}
				
				if (this.inBounds(newAbsLocation))
					updateTileAccessibility(newAbsLocation);
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
	
	/**
	 * 
	 * @param coords The desired coordinates to look for nearest available rooms
	 * @param requiredSpace
	 * @return An array of two vectors, representing the 3d coords of the top left corner (min r and min c)
	 * of the bounds, and then 2d rectangular dimensions of the bounds.
	 */
	/*
	public Object[] getNearestViableRoom(Set<Human> validLandOwners, Vector3i coords, Vector2i requiredSpace) {
		TODO;
		
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
				society, validLandOwners, false, false, new LocalTileCond.IsBuildingTileCond());
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
			if (this.tileIsAccessible(location)) {
				emptySpaces.add(location);
			}
		}
		return emptySpaces;
	}
	
	public void addHuman(LivingEntity human, Vector3i coords) {
		LocalTile tile = getTile(coords);
		
		if (human.location != null) {
			human.location.removePerson(human);
			human.location = null;
			if (human.location.getPeople().size() == 0) {
				peopleLookup.remove(human.location.coords);
				allLivingBeings.remove(human);
			}
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
			if (this.tileIsAccessible(coords)) {
				return coords;
			}
		}
		return null;
	}
	
	//See LocalGrid::tileIsAccessible(...)
	public Vector3i findHighestAccessibleHeight(int r, int c) {
		for (int h = heights - 1; h >= 0; h--) {
			Vector3i coords = new Vector3i(r,c,h);
			if (this.tileIsAccessible(coords)) {
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
			if (this.tileIsAccessible(new Vector3i(r, c, h))) {
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
					if (this.tileIsAccessible(new Vector3i(r,c,h))) {
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
		
		Set<Vector3i> range = Vector3i.getRange(newLandClaim.boundary);
		for (Vector3i vec: range) {
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
		
		Set<Vector3i> range = Vector3i.getRange(claim.boundary);
		for (Vector3i vec: range) {
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
	}
	
}
