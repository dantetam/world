package io.github.dantetam.world.life;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import io.github.dantetam.toolbox.ListUtil;
import io.github.dantetam.world.civilization.Society;
import io.github.dantetam.world.dataparse.EthosData;
import io.github.dantetam.world.dataparse.ItemData;
import io.github.dantetam.world.dataparse.ProcessData;
import io.github.dantetam.world.items.InventoryItem;
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
		
		Map<String, Ethos> personalTraits = new HashMap<>();
		for (Ethos ethos: EthosData.getPersonalityTraits()) {
			personalTraits.put(ethos.name, ethos.clone());
		}
		brain.ethosPersonalityTraits = personalTraits;
		
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
		
		//Determine ethos towards other ethos, i.e. judgments  
		for (Ethos ethos: getAllHumanEthos(brain)) {
			String name = ethos.name;
			Ethos newEthos = new Ethos(name, 0, "", "");
			brain.ethosTowardsOtherEthos.put(ethos, newEthos);
		}
	}

	//Assign random, sensible values to ethos, 
	public static void initValuesToHumanEthos(HumanBrain brain) {
		for (Entry<String, Ethos> entry: brain.greatEthos.entrySet()) {
			Ethos ethos = entry.getValue();
			ethos.severity = Math.random() * 60 - 30;
			
			Ethos ethosOpinion = brain.ethosTowardsOtherEthos.get(ethos);
			if (ethosOpinion != null) {
				double offset = Math.random() * 40 - 20;
				ethosOpinion.severity = ethos.severity + offset;
			}
		}
		for (Entry<String, Ethos> entry: brain.ethosPersonalityTraits.entrySet()) {
			Ethos ethos = entry.getValue();
			ethos.severity = Math.random() * 100 - 50;
			
			Ethos ethosOpinion = brain.ethosTowardsOtherEthos.get(ethos);
			if (ethosOpinion != null) {
				double offset = Math.random() * 40 - 20;
				ethosOpinion.severity = ethos.severity + offset;
			}
		}
		TODO
	}
	
	/**
	 * Evaluate a person's thoughts based on their 'base' (ingrained opinions since birth),
	 * and their experiences, to gradually evolve systems of thought over time.
	 * @param human The human in question
	 */
	public static void calculateHumanEthosExp(Human human) {
		
	}

	/**
	 * Ideally used for reproduction, create a new human brain based on the prevailing thoughts
	 * of other people around, and societal ethos.
	 * 
	 * TODO: Use a childhood guardian system like that of CK2, 
	 * 
	 * @param newBrain    The new brain, usually belonging to a new baby
	 * @param otherBrains Other people that have an influence on this baby
	 * @param society     The society this person is born in
	 */
	public static void influenceHumanBrain(HumanBrain newBrain, List<HumanBrain> otherBrains, 
			Society society) {
		
	}
	
	private static Collection<Ethos> getAllHumanEthos(HumanBrain brain) {
		return ListUtil.stream(
				brain.greatEthos.values(),
				brain.ethosPersonalityTraits.values(),
				brain.ethosTowardsItems.values(),
				brain.ethosTowardsProcesses.values()
				);
	}
	
}
