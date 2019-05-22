package io.github.dantetam.world.grid;

import java.text.DecimalFormat;
import java.util.List;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import io.github.dantetam.toolbox.StringUtil;
import io.github.dantetam.vector.Vector2i;
import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.civilization.Household;
import io.github.dantetam.world.civilization.Society;
import io.github.dantetam.world.dataparse.ItemData;
import io.github.dantetam.world.life.Ethos;
import io.github.dantetam.world.life.Human;
import io.github.dantetam.world.life.LivingEntity;
import io.github.dantetam.world.process.LocalProcess;
import io.github.dantetam.world.worldgen.DNAGridGeneration;
import io.github.dantetam.world.worldgen.DNAGridGeneration.DNATileData;
import io.github.dantetam.world.worldgen.LocalGridTerrainInstantiate;

public class WorldGrid {

	private Calendar currentWorldTime;
	public LocalGrid activeLocalGrid;
	public Society testSociety;
	
	private LocalGrid[][] localGridTiles;
	
	public WorldGrid() {
		currentWorldTime = Calendar.getInstance();
		Vector2i worldSize = new Vector2i(50, 50);
		localGridTiles = new LocalGrid[worldSize.x][worldSize.y];

		Vector3i sizes = new Vector3i(200,200,50);
		int biome = 3;
		localGridTiles[2][2] = new LocalGridTerrainInstantiate(sizes, biome).setupGrid();
		
		activeLocalGrid = localGridTiles[2][2];
		
		testSociety = new Society("TestSociety", activeLocalGrid);
		testSociety.societyCenter = new Vector3i(50,50,30);
		
		for (int i = 0; i < 15; i++) {
			int numPeopleHouse = (int)(Math.random() * 6) + 1;
			List<Human> people = new ArrayList<>();
			for (int j = 0; j < numPeopleHouse; j++) {
				int r = (int) (Math.random() * activeLocalGrid.rows);
				int c = (int) (Math.random() * activeLocalGrid.cols);
				int h = activeLocalGrid.findHighestGroundHeight(r,c);
				
				Human human = new Human(testSociety, "Human" + j + " of House " + i);
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
							human.brain.greatEthos.put("Culture", 
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
		
		/*
		Map<Integer, Double> calcUtility = testSociety.findCompleteUtilityAllItems();
		
		for (Entry<Integer, Double> entry: calcUtility.entrySet()) {
			System.out.println(ItemData.getNameFromId(entry.getKey()) + ": " + entry.getValue());
		}
	
		//Process process = testSociety.findBestProcess(calcUtility, ItemData.getIdFromName("Wheat"));
		//System.out.println(process.toString());
	
		Map<Process, Double> bestProcesses = testSociety.prioritizeProcesses(calcUtility, null, 20);
		
		for (Entry<Process, Double> entry: bestProcesses.entrySet()) {
			System.out.println("<########>");
			System.out.println(entry.getKey());
			System.out.println(entry.getValue());
		}
		*/
		
		Map<Integer, Double> calcUtility = testSociety.findCompleteUtilityAllItems(null);
		
		DecimalFormat df = new DecimalFormat("#.##");
		
		for (Entry<Integer, Double> entry: calcUtility.entrySet()) {
			if (entry.getValue() > 0)
				System.out.println(ItemData.getNameFromId(entry.getKey()) + ": " + df.format(entry.getValue()));
		}
		
		//tick();
	}
	
	public void tick() {
		LocalGridTimeExecution.tick(this, activeLocalGrid, testSociety);
		currentWorldTime.add(Calendar.SECOND, 1);
	}
	
	public Date getTime() {
		return currentWorldTime.getTime();
	}
	
}
