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

import io.github.dantetam.toolbox.StringUtil;
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

	private Calendar currentWorldTime;
	public LocalGrid activeLocalGrid;
	public Society testSociety;
	
	public int worldRows, worldCols;
	private LocalGrid[][] localGridTiles;
	
	//Manage all societies, relations, wars, and societal relationships in general
	public SocietyDiplomacy societalDiplomacy;
	
	//Store/find all free households in the world
	private Map<Vector2i, List<Household>> worldAllHouseholds = new HashMap<>();
	
	public boolean currentlyTicking = false;
	
	public WorldGrid() {
		currentWorldTime = Calendar.getInstance();
		worldRows = 50;
		worldCols = 50;
		Vector2i worldSize = new Vector2i(worldRows, worldCols);
		localGridTiles = new LocalGrid[worldSize.x][worldSize.y];

		for (int r = 0; r < worldSize.x; r++) {
			for (int c = 0; c < worldSize.y; c++) {
				worldAllHouseholds.put(new Vector2i(r,c), new ArrayList<>());
			}
		}
		
		societalDiplomacy = new SocietyDiplomacy(this);
		
		Vector3i sizes = new Vector3i(200,200,50);
		int biome = 3;
		localGridTiles[2][2] = new LocalGridInstantiate(sizes, biome).setupGrid(true);
		
		activeLocalGrid = localGridTiles[2][2];
		
		testSociety = new Society("TestSociety", activeLocalGrid);
		testSociety.societyCenter = new Vector3i(50,50,30);
		
		int numHouses = 20;
		
		for (int i = 0; i < numHouses; i++) {
			int numPeopleHouse = (int)(Math.random() * 8) + 1;
			List<Human> people = new ArrayList<>();
			for (int j = 0; j < numPeopleHouse; j++) {
				int r = (int) (Math.random() * activeLocalGrid.rows);
				int c = (int) (Math.random() * activeLocalGrid.cols);
				int h = activeLocalGrid.findHighestGroundHeight(r,c);
				
				Human human = new Human(testSociety, "Human" + j + " of House " + i);
				people.add(human);
				activeLocalGrid.addHuman(human, new Vector3i(r,c,h));
				
				human.inventory.addItem(ItemData.randomBaseItem());
				human.inventory.addItem(ItemData.randomBaseItem());
				human.inventory.addItem(ItemData.randomBaseItem());
				human.inventory.addItem(ItemData.randomBaseItem());
				human.inventory.addItem(ItemData.randomItem());
				human.inventory.addItem(ItemData.item("Wheat Seeds", 50));
				human.inventory.addItem(ItemData.item("Pine Wood", 50));
			}
			testSociety.addHousehold(new Household(people));
		}
		
		DNATileData[][] worldData = DNAGridGeneration.createGrid(worldSize);
		for (int r = 0; r < worldSize.x; r++) {
			for (int c = 0; c < worldSize.y; c++) {
				LocalGrid grid = localGridTiles[r][c];
				if (grid != null) {
					DNATileData tileData = worldData[r][c];
					for (LivingEntity being: grid.getAllLivingBeings()) {
						if (being instanceof Human) {
							Human human = (Human) being;
							human.dna.overrideDnaMapping("race", tileData.race);
							human.dna.overrideDnaMapping("culture", tileData.culture);
							String apparentCul = tileData.culture.repeat(1);
							for (int i = 0; i < (int) (Math.random() * 8); i++) {
								apparentCul = StringUtil.mutateAlphaNumStr(apparentCul);
							}
							human.brain.ethosSet.greatEthos.put("Culture", 
									new Ethos("Culture", 1.0, "MOD:" + apparentCul, ""));
							for (int i = 0; i < tileData.languages.size(); i++) {
								String language = tileData.languages.get(i);
								human.brain.languageCodesStrength.put(language, 1.0 / i);
							}
						}
					}
				}
			}
		}
		
		Map<Integer, Double> calcUtility = testSociety.findCompleteUtilityAllItems(null);
		
		DecimalFormat df = new DecimalFormat("#.##");
		
		for (Entry<Integer, Double> entry: calcUtility.entrySet()) {
			if (entry.getValue() > 0)
				System.out.println(ItemData.getNameFromId(entry.getKey()) + ": " + df.format(entry.getValue()));
		}
	}
	
	public void addSociety(Society society) {
		this.societalDiplomacy.addSociety(society);
	}
	
	public void tick() {
		for (int r = 0; r < worldRows; r++) {
			for (int c = 0; c < worldCols; c++) {
				LocalGrid grid = localGridTiles[r][c];
				if (grid != null) {
					List<Household> households = getFreeHouseholds(new Vector2i(r,c));
					FreeActionsHousehold.considerAllFreeActionsHouseholds(
							this, grid, households, getTime());
					
					FreeActionsHumans.considerAllFreeActionsHumans(this, grid,
							testSociety.getAllPeople(), getTime());
					
					//TODO //Tick for every society involved in this grid
					LocalGridTimeExecution.tick(this, grid, testSociety);
				}
			}
		}
		
		for (Society society: this.societalDiplomacy.getAllSocieties()) {
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
	
	public boolean inBounds(Vector2i coord) {
		if (coord.x < 0 || coord.x >= worldRows || coord.y < 0 || coord.y >= worldCols) {
			return false;
		}
		return true;
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
	
}
