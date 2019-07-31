package io.github.dantetam.world.grid;

import java.text.DecimalFormat;
import java.util.List;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import io.github.dantetam.toolbox.MapUtil;
import io.github.dantetam.toolbox.StringUtil;
import io.github.dantetam.toolbox.log.CustomLog;
import io.github.dantetam.vector.Vector2i;
import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.civhumanai.Ethos;
import io.github.dantetam.world.civhumansocietyai.FreeActionsHousehold;
import io.github.dantetam.world.civhumansocietyai.FreeActionsHumans;
import io.github.dantetam.world.civhumansocietyai.FreeActionsSociety;
import io.github.dantetam.world.civilization.Household;
import io.github.dantetam.world.civilization.Society;
import io.github.dantetam.world.civilization.SocietyDiplomacy;
import io.github.dantetam.world.dataparse.ItemData;
import io.github.dantetam.world.life.Human;
import io.github.dantetam.world.life.LivingEntity;
import io.github.dantetam.world.process.LocalProcess;
import io.github.dantetam.world.worldgen.DNAGridGeneration;
import io.github.dantetam.world.worldgen.DNAGridGeneration.DNATileData;
import io.github.dantetam.world.worldgen.LocalGridInstantiate;

public class WorldGrid {

	public Date worldStartTime;
	private Calendar currentWorldTime;
	
	public Vector2i worldSize;
	private LocalGrid[][] localGridTiles;
	
	//Manage all societies, relations, wars, and societal relationships in general
	public SocietyDiplomacy societalDiplomacy;
	
	//Store/find all free households in the world
	private Map<Vector2i, List<Household>> worldAllHouseholds = new HashMap<>();
	
	public boolean currentlyTicking = false;
	
	public WorldGrid(Vector2i worldSize) {
		this.worldSize = worldSize.clone();
		currentWorldTime = Calendar.getInstance();
		worldStartTime = getTime();
		
		localGridTiles = new LocalGrid[worldSize.x][worldSize.y];

		for (int r = 0; r < worldSize.x; r++) {
			for (int c = 0; c < worldSize.y; c++) {
				worldAllHouseholds.put(new Vector2i(r,c), new ArrayList<>());
			}
		}
		
		societalDiplomacy = new SocietyDiplomacy(this);
		
		/*
		Map<Integer, Double> calcUtility = testSociety.findCompleteUtilityAllItems(null);
		
		DecimalFormat df = new DecimalFormat("#.##");
		
		CustomLog.outPrintln("-----Calculated adjusted full economic utility-----");
		for (Entry<Integer, Double> entry: calcUtility.entrySet()) {
			if (entry.getValue() > 0)
				CustomLog.outPrintln(ItemData.getNameFromId(entry.getKey()) + ": " + df.format(entry.getValue()));
		}
		
		Map<Integer, Double> rawUtility = testSociety.findRawResourcesRarity(null);
		rawUtility = MapUtil.getSortedMapByValueDesc(rawUtility);
		
		CustomLog.outPrintln("-----Commonness of Resources (higher means more common)-----");
		for (Entry<Integer, Double> entry: rawUtility.entrySet()) {
			if (entry.getValue() > 0)
				CustomLog.outPrintln(ItemData.getNameFromId(entry.getKey()) + ": " + df.format(entry.getValue()));
		}
		*/
	}
	
	public void addSociety(Society society) {
		this.societalDiplomacy.addSociety(society);
	}
	
	//TODO: Fix this so societies can live across multiple grids
	public void tick() {
		for (int r = 0; r < worldSize.x; r++) {
			for (int c = 0; c < worldSize.y; c++) {
				LocalGrid grid = localGridTiles[r][c];
				if (grid != null) {
					List<Household> households = getFreeHouseholds(new Vector2i(r,c));
					FreeActionsHousehold.considerAllFreeActionsHouseholds(
							this, grid, households, getTime());
				}
			}
		}
		
		for (Society society: this.societalDiplomacy.getAllSocieties()) {
			//TODO //Tick for every society involved in this grid
			LocalGridTimeExecution.tick(this, society.primaryGrid, society);
			
			FreeActionsHumans.considerAllFreeActionsHumans(this, society.primaryGrid,
					society.getAllPeople(), getTime());
			
			for (Household house: society.getAllHouseholds()) {
				FreeActionsHousehold.considerAllFreeActionsHouse(this, society, house, getTime());
			}
			FreeActionsSociety.considerAllFreeActions(this, society, getTime());
		}
		
		currentWorldTime.add(Calendar.SECOND, 1);
	}
	
	public Date getTime() {
		return currentWorldTime.getTime();
	}

	public List<Household> getFreeHouseholds(Vector2i coord) {
		if (inBounds(coord)) {
			List<Household> freeHouses = new ArrayList<>();
			List<Household> allHousesAtCoord = this.worldAllHouseholds.get(coord);
			for (Household house: allHousesAtCoord) {
				if (house.society == null) {
					freeHouses.add(house);
				}
			}
			return freeHouses;
		}
		return null;
	}
	
	public boolean inBounds(Vector2i coords) {
		return !(coords.x < 0 || coords.y < 0 || coords.x >= worldSize.x || coords.y >= worldSize.y);
	}
	
	public LocalGrid getLocalGrid(Vector2i coords) {
		if (!inBounds(coords)) {
			throw new IllegalArgumentException("In world grid, cannot find indexed local grid: " + coords);
		}
		return this.localGridTiles[coords.x][coords.y];
	}
	
	public void initLocalGrid(Vector2i coords, LocalGrid grid) {
		if (!inBounds(coords)) {
			throw new IllegalArgumentException("In world grid, cannot init indexed local grid: " + coords);
		}
		this.localGridTiles[coords.x][coords.y] = grid;
	}
	
}
