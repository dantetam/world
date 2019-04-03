package io.github.dantetam.world.grid;

import java.util.Map;
import java.util.Map.Entry;

import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.civilization.Human;
import io.github.dantetam.world.civilization.Society;
import io.github.dantetam.world.dataparse.ItemData;
import io.github.dantetam.world.worldgen.LocalGridTerrainInstantiate;
import io.github.dantetam.world.dataparse.Process;

public class WorldGrid {

	public LocalGrid activeLocalGrid;
	public Society testSociety;
	
	public WorldGrid() {
		Vector3i sizes = new Vector3i(200,200,50);
		int biome = 3;
		activeLocalGrid = new LocalGridTerrainInstantiate(sizes, biome).setupGrid();
		
		testSociety = new Society(activeLocalGrid);
		Human human = new Human("");
		testSociety.addPerson(human);
		activeLocalGrid.addHuman(human, new Vector3i(25,25,30));
		human.inventory.addItem(ItemData.item("Wheat Seeds", 50));
		human.inventory.addItem(ItemData.item("Pine Wood", 50));
		
		Map<Integer, Double> calcUtility = testSociety.findCompleteUtilityAllItems();
		
		for (Entry<Integer, Double> entry: calcUtility.entrySet()) {
			System.out.println(ItemData.getNameFromId(entry.getKey()) + ": " + entry.getValue());
		}
	
		//Process process = testSociety.findBestProcess(calcUtility, ItemData.getIdFromName("Wheat"));
		//System.out.println(process.toString());
		
		
		Map<Process, Double> bestProcesses = testSociety.prioritizeProcesses(calcUtility, 20);
		
		for (Entry<Process, Double> entry: bestProcesses.entrySet()) {
			System.out.println("<########>");
			System.out.println(entry.getKey());
			System.out.println(entry.getValue());
		}
	}
	
}
