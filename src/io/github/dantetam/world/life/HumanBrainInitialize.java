package io.github.dantetam.world.life;

import java.util.HashMap;
import java.util.Map;

import io.github.dantetam.world.dataparse.EthosData;
import io.github.dantetam.world.dataparse.ItemData;
import io.github.dantetam.world.dataparse.ProcessData;
import io.github.dantetam.world.items.InventoryItem;
import io.github.dantetam.world.life.HumanBrain.Ethos;
import io.github.dantetam.world.process.LocalProcess;

public class HumanBrainInitialize {

	/**
	 * Fill a human brain with most of its starting opinions and base ethos
	 * 
	 */
	public static void initHumanBrain(HumanBrain brain) {
		Map<String, Ethos> greatEthos = new HashMap<>();
		for (Ethos ethos: EthosData.getMajorEthos()) {
			greatEthos.put(ethos.name, ethos.clone());
		}
		brain.greatEthos = greatEthos;
		
		Map<LocalProcess, Ethos> mapProcesses = new HashMap<>();
		for (LocalProcess process: ProcessData.getAllProcesses()) {
			mapProcesses.put(process, new Ethos("Process " + process.name, 0, "", ""));
		}
		brain.ethosTowardsProcesses = mapProcesses;
		
		Map<Integer, Ethos> mapItems = new HashMap<>();
		for (InventoryItem item: ItemData.getAllItems()) {
			mapItems.put(item.itemId, new Ethos("Item " + item.name, 0, "", ""));
		}
		brain.ethosTowardsItems = mapItems;
	}
	
}
