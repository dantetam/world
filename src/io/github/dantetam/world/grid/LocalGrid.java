package io.github.dantetam.world.grid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.dantetam.toolbox.VecGridUtil;
import io.github.dantetam.vector.Vector2i;
import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.dataparse.ItemData;
import io.github.dantetam.world.dataparse.WorldCsvParser;
import io.github.dantetam.world.items.Inventory;
import io.github.dantetam.world.items.InventoryItem;
import io.github.dantetam.world.life.Human;
import io.github.dantetam.world.life.LivingEntity;
import kdtreegeo.KdTree;

/**
 * A grid representing one section of the whole world, i.e. a self contained world with its own people, tiles, and so on.
 * @author Dante
 *
 */

//TODO: Different societies claim possibly overlapping tiles in a grid, use these claims
//as a basis for societal wealth, as well as part of larger border disputes.

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
	
	private KdTree<Vector3i> peopleLookup;
	
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
		
		peopleLookup = new KdTree<>();
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
	
	public boolean tileIsAccessible(Vector3i coords) {
		LocalTile tile = getTile(coords);
		if (tile == null) return false;
		Vector3i belowCoords = new Vector3i(coords.x, coords.y, coords.z - 1);
		if (inBounds(belowCoords)) {
			if (!tileIsOccupied(coords)) {
				return false;
			}
		}
		//if (tile.tileBlockId != ItemData.ITEM_EMPTY_ID) return false;
		if (tile.building != null) {
			if (tile.building.calculatedLocations != null) {
				return !tile.building.calculatedLocations.contains(coords);
			}
		}
		return true;
	}
	
	public static final Set<Vector3i> directAdjOffsets6 = new HashSet<Vector3i>() {
		{add(new Vector3i(1,0,0)); add(new Vector3i(-1,0,0)); add(new Vector3i(0,1,0));
			add(new Vector3i(0,-1,0)); add(new Vector3i(0,0,1)); add(new Vector3i(0,0,-1));}};
	public Set<Vector3i> getAllNeighbors(Vector3i coords) {
		Set<Vector3i> candidates = new HashSet<>();
		for (Vector3i adjOffset: directAdjOffsets6) {
			Vector3i neighbor = coords.getSum(adjOffset);
			if (inBounds(neighbor)) {
				candidates.add(neighbor);
			}
		}
		return candidates;
	}
	public Set<LocalTile> getAccessibleNeighbors(LocalTile tile) {
		Set<Vector3i> neighbors = getAllNeighbors(tile.coords);
		Set<LocalTile> candidateTiles = new HashSet<>();
		for (Vector3i neighbor: neighbors) {
			if (inBounds(neighbor)) {
				LocalTile neighborTile = getTile(neighbor);
				if (neighborTile == null) {
					neighborTile = createTile(neighbor);
				}
				candidateTiles.add(neighborTile);
			}
		}
		return candidateTiles;
	}
	//TODO: Conditional access to local tiles based on actor biology
	//public Set<LocalTile> getAccessibleNeighbors(LocalTile tile, LivingEntity being)
	
	public static final Set<Vector3i> allAdjOffsets8 = new HashSet<Vector3i>() {
		{add(new Vector3i(1,0,0)); add(new Vector3i(-1,0,0)); add(new Vector3i(0,1,0)); add(new Vector3i(0,-1,0)); 
		add(new Vector3i(1,1,0)); add(new Vector3i(1,-1,0)); add(new Vector3i(-1,-1,0)); add(new Vector3i(-1,1,0));}};
	public Set<Vector3i> getAllFlatAdjAndDiag(Vector3i coords) {
		Set<Vector3i> candidates = new HashSet<>();
		for (Vector3i adjOffset: allAdjOffsets8) {
			Vector3i neighbor = coords.getSum(adjOffset);
			if (inBounds(neighbor)) {
				candidates.add(neighbor);
			}
		}
		return candidates;
	}
	
	public static final Set<Vector3i> allVertAdjOffsets26 = new HashSet<Vector3i>() {{
		add(new Vector3i(1,0,0)); add(new Vector3i(-1,0,0)); add(new Vector3i(0,1,0)); add(new Vector3i(0,-1,0)); 
		add(new Vector3i(1,1,0)); add(new Vector3i(1,-1,0)); add(new Vector3i(-1,-1,0)); add(new Vector3i(-1,1,0));
		add(new Vector3i(1,0,1)); add(new Vector3i(-1,0,1)); add(new Vector3i(0,1,1)); add(new Vector3i(0,-1,1)); 
		add(new Vector3i(1,1,1)); add(new Vector3i(1,-1,1)); add(new Vector3i(-1,-1,1)); add(new Vector3i(-1,1,1));
		add(new Vector3i(1,0,-1)); add(new Vector3i(-1,0,-1)); add(new Vector3i(0,1,-1)); add(new Vector3i(0,-1,-1)); 
		add(new Vector3i(1,1,-1)); add(new Vector3i(1,-1,-1)); add(new Vector3i(-1,-1,-1)); add(new Vector3i(-1,1,-1));
		add(new Vector3i(0,0,1)); add(new Vector3i(0,0,-1));
		}};
	public Set<Vector3i> getEveryNeighborUpDown(Vector3i coords) {
		Set<Vector3i> candidates = new HashSet<>();
		for (Vector3i adjOffset: allVertAdjOffsets26) {
			Vector3i neighbor = coords.getSum(adjOffset);
			if (inBounds(neighbor)) {
				candidates.add(neighbor);
			}
		}
		return candidates;
	}
	public void markAllAdjAsExposed(Vector3i coords) {
		Set<Vector3i> neighbors = this.getEveryNeighborUpDown(coords);
		neighbors.add(coords);
		for (Vector3i neighbor: neighbors) {
			LocalTile tile = getTile(neighbor);
			if (tile != null) {
				tile.exposedToAir = true;
			}
		}
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
	}
	
	public LocalTile createTile(Vector3i coords) {
		if (inBounds(coords)) {
			LocalTile tile = new LocalTile(coords);
			grid[coords.x][coords.y][coords.z] = tile;
			return tile;
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
		for (String itemGroup: itemGroups) {
			if (!(itemGroupTileLookup.containsKey(itemGroup))) {
				itemGroupTileLookup.put(itemGroup, new KdTree<Vector3i>());
			}
			itemGroupTileLookup.get(itemGroup).add(coords);
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
	
	/*
	public void dropItemAtTile(Vector3i coords, InventoryItem item) {
		LocalTile tile = getTile(coords);
		if (tile == null) {
			tile = createTile(coords);
		}
		if (tile != null) {
			tile.itemsOnFloor.addItem(item);
			if (!(itemIdQuickTileLookup.containsKey(item.itemId))) {
				itemIdQuickTileLookup.put(item.itemId, new KdTree<Vector3i>());
			}
			itemIdQuickTileLookup.get(item.itemId).add(coords);
			globalItemsLookup.add(coords);
		}
	}
	*/
	
	public void putBlockIntoTile(Vector3i coords, int blockId) {
		//globalTileBlockLookup
		LocalTile tile = getTile(coords);
		int oldItemId = tile.tileBlockId;
		if (tile != null) {
			tile.tileBlockId = blockId;
			if (tile.tileBlockId != ItemData.ITEM_EMPTY_ID) {
				if (!(globalTileBlockLookup.containsKey(blockId))) {
					globalTileBlockLookup.put(blockId, new KdTree<Vector3i>());
				}
				globalTileBlockLookup.get(blockId).add(coords);
			}
			else {
				if (oldItemId != ItemData.ITEM_EMPTY_ID) {
					globalTileBlockLookup.get(oldItemId).remove(coords);
				}
				markAllAdjAsExposed(coords);
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
		return this.itemGroupTileLookup.get(groupName);
	}
	
	public boolean buildingCanFitAt(LocalBuilding building, Vector3i newPrimaryCoords, boolean overrideGrid) {
		//Check to see if the building fits
		List<Vector3i> offsets = building.getLocationOffsets();
		for (Vector3i offset: offsets) {
			Vector3i candidate = newPrimaryCoords.getSum(offset);
			LocalTile candidateTile = this.getTile(candidate);
			if (!inBounds(candidate) || (candidateTile != null && candidateTile.building != null && !overrideGrid)) {
				return false;
			}
		}
		return true;
	}
	
	public void addBuilding(LocalBuilding building, Vector3i newPrimaryCoords, boolean overrideGrid) {
		if (buildingCanFitAt(building, newPrimaryCoords, overrideGrid)) {
			removeBuilding(building);
			building.setPrimaryLocation(newPrimaryCoords);
			Set<Vector3i> newAbsLocations = building.calculatedLocations;
			int buildingTileIndex = 0;
			for (Vector3i newAbsLocation: newAbsLocations) {
				LocalTile absTile = getTile(newAbsLocation);
				if (absTile == null) {
					absTile = createTile(newAbsLocation);
				}
				absTile.building = building;
				//absTile.tileBlockId = building.buildingBlockIds.get(buildingTileIndex);
				buildingTileIndex++;
			} 
			allBuildings.add(building);
			buildingLookup.add(newPrimaryCoords);
			markAllAdjAsExposed(newPrimaryCoords);
		}
	}
	
	public void removeBuilding(LocalBuilding building) {
		Set<Vector3i> oldAbsLocations = building.calculatedLocations;
		for (Vector3i oldAbsLocation: oldAbsLocations) {
			LocalTile oldTile = getTile(oldAbsLocation);
			if (oldTile != null) {
				oldTile.building = null;
				oldTile.tileBlockId = ItemData.ITEM_EMPTY_ID;
			}
		}
		buildingLookup.remove(building.getPrimaryLocation());
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
	public Object[] getNearestViableRoom(Vector3i coords, Vector2i requiredSpace) {
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
			LocalTile buildingTile = getTile(location);
			if (buildingTile.tileBlockId == ItemData.ITEM_EMPTY_ID) {
				emptySpaces.add(buildingTile.coords);
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
		//TODO
		return true;
	}
	
	public int findLowestEmptyHeight(int r, int c) {
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
	
	//Find an empty height at lowest possible level, i.e. an empty tile with ground right below it,
	//so that a Human can be placed.
	public int findHighestGroundHeight(int r, int c) {
		Set<Integer> groundGroup = ItemData.getGroupIds("Ground");
		for (int h = heights - 1; h >= 0; h--) {
			LocalTile tile = getTile(new Vector3i(r, c, h));
			if (tile != null) {
				if (groundGroup.contains(tile.tileBlockId) || groundGroup.contains(tile.tileFloorId)) {
					return h+1;
				}
			}
		}
		return 0;
	}
	
	public double averageBeauty(Vector3i coords) {
		Set<Vector3i> spiralCoords = rangeLargeSquareTiles(coords, 0, 8);
		double sumBeauty = 0;
		int numWeights = 0;
		final double itemWeighting = 0.25;
		
		for (Vector3i spiralCoord: spiralCoords) {
			int groundHeight = this.findHighestGroundHeight(spiralCoord.x, spiralCoord.y);
			int emptyHeight = this.findLowestEmptyHeight(spiralCoord.x, spiralCoord.y);
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
					if (this.tileIsOccupied(new Vector3i(r,c,h))) {
						return new Vector3i(r,c,h);
					}
				}
			}
		}
		return null;
	}
	
	//Claim is equivalent to actual human ownership and full rights, as opposed to being in use
	public void claimTile(Human human, Vector3i coords, boolean override) {
		LocalTile tile = getTile(coords);
		if (tile != null) {
			if (tile.humanClaim == null || override) {
				unclaimTile(coords);
				tile.humanClaim = human;
				human.allClaims.add(coords);
			}
		}
	}
	
	public void unclaimTile(Vector3i coords) {
		LocalTile tile = getTile(coords);
		if (tile != null) {
			if (tile.humanClaim != null) {
				tile.humanClaim.allClaims.remove(coords);
				tile.humanClaim = null;
			}
		}
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
		
		grid.addBuilding(building, new Vector3i(25,25,35), true);
		
		System.out.println(Arrays.toString(building.calculatedLocations.toArray())); //[25 25 25, 26 25 25, 25 28 25]
		System.out.println(grid.getTile(new Vector3i(26,25,45))); //This tile should have been created
		System.out.println(grid.getTile(new Vector3i(25,25,45)).building.name); //Test1
		
		grid.removeBuilding(building);
		
		System.out.println(grid.getTile(new Vector3i(26,25,45)).building); //null
	}
	
}
