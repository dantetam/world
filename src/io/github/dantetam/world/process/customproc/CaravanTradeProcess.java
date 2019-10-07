package io.github.dantetam.world.process.customproc;

import java.util.List;

import io.github.dantetam.world.dataparse.ItemTotalDrops;
import io.github.dantetam.world.items.InventoryItem;
import io.github.dantetam.world.process.LocalProcess;

public class CaravanTradeProcess extends LocalProcess {

	public CaravanTradeProcess(String name, List<InventoryItem> input, String buildingName,
			boolean site, List<ProcessStep> steps) {
		super(name, input, null, buildingName, site, null, steps, null, 1, null);
		// Auto-generated constructor stub
		//TODO; Add special properties to the caravan trade process 
		//As well as a skill process distribution for social/barter trading skills for trading between societies/people
	}
	
}
