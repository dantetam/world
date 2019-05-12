package io.github.dantetam.world.grid;

import java.text.DecimalFormat;
import java.util.List;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.civilization.Household;
import io.github.dantetam.world.civilization.Human;
import io.github.dantetam.world.civilization.Society;
import io.github.dantetam.world.dataparse.ItemData;
import io.github.dantetam.world.process.LocalProcess;
import io.github.dantetam.world.worldgen.LocalGridTerrainInstantiate;

public class WorldGrid {

	private Calendar currentWorldTime;
	public LocalGrid activeLocalGrid;
	public Society testSociety;
	
	public WorldGrid() {
		currentWorldTime = Calendar.getInstance();
		
		Vector3i sizes = new Vector3i(200,200,50);
		int biome = 3;
		activeLocalGrid = new LocalGridTerrainInstantiate(sizes, biome).setupGrid();
		
		testSociety = new Society("TestSociety", activeLocalGrid);
		testSociety.societyCenter = new Vector3i(50,50,30);
		
		for (int i = 0; i < 15; i++) {
			int numPeopleHouse = (int)(Math.random() * 6) + 1;
			List<Human> people = new ArrayList<>();
			for (int j = 0; j < numPeopleHouse; j++) {
				int r = (int) (Math.random() * activeLocalGrid.rows);
				int c = (int) (Math.random() * activeLocalGrid.cols);
				int h = activeLocalGrid.findHighestGroundHeight(r,c);
				
				Human human = new Human(testSociety, "Human" + i);
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
