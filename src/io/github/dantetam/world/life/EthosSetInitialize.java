package io.github.dantetam.world.life;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import io.github.dantetam.world.civhumanai.Ethos;
import io.github.dantetam.world.civhumanai.EthosSet;
import io.github.dantetam.world.civhumansocietyai.FreeAction;
import io.github.dantetam.world.civhumansocietyai.FreeActionsHousehold;
import io.github.dantetam.world.civhumansocietyai.FreeActionsHumans;
import io.github.dantetam.world.civhumansocietyai.FreeActionsSociety;
import io.github.dantetam.world.civilization.Society;
import io.github.dantetam.world.dataparse.EthosData;
import io.github.dantetam.world.dataparse.ItemData;
import io.github.dantetam.world.dataparse.ProcessData;
import io.github.dantetam.world.items.InventoryItem;
import io.github.dantetam.world.process.LocalProcess;

public class EthosSetInitialize {

	/**
	 * Fill a human brain with most of its starting opinions and base ethos
	 * 
	 */
	public static void initHumanBrain(EthosSet ethosSet) {
		Map<String, Ethos> greatEthos = new HashMap<>();
		for (Ethos ethos: EthosData.getMajorEthos()) {
			greatEthos.put(ethos.name, ethos.clone());
		}
		ethosSet.greatEthos = greatEthos;
		
		Map<String, Ethos> personalTraits = new HashMap<>();
		for (Ethos ethos: EthosData.getPersonalityTraits()) {
			personalTraits.put(ethos.name, ethos.clone());
		}
		ethosSet.ethosPersonalityTraits = personalTraits;
		
		Map<LocalProcess, Ethos> mapProcesses = new HashMap<>();
		for (LocalProcess process: ProcessData.getAllProcesses()) {
			mapProcesses.put(process, new Ethos("Process " + process.name, 0, "", ""));
		}
		ethosSet.ethosTowardsProcesses = mapProcesses;
		
		Map<Integer, Ethos> mapItems = new HashMap<>();
		for (InventoryItem item: ItemData.getAllItems()) {
			mapItems.put(item.itemId, new Ethos("Item " + item.name, 0, "", ""));
		}
		ethosSet.ethosTowardsItems = mapItems;
		
		Map<String, Ethos> mapFreeActions = new HashMap<>();
		for (Entry<String, FreeAction> entry: FreeActionsHumans.freeActionsListHuman.entrySet()) {
			String name = "FreeActionsHumans " + entry.getKey();
			Ethos newEthos = new Ethos(name, 0, "", "");
			mapFreeActions.put(name, newEthos);
		}
		for (Entry<String, FreeAction> entry: FreeActionsHousehold.freeActionsListHuman.entrySet()) {
			String name = "FreeActionsHousehold " + entry.getKey();
			Ethos newEthos = new Ethos(name, 0, "", "");
			mapFreeActions.put(name, newEthos);
		}
		for (Entry<String, FreeAction> entry: FreeActionsHousehold.freeActionsListHousehold.entrySet()) {
			String name = "FreeActionsHousehold " + entry.getKey();
			Ethos newEthos = new Ethos(name, 0, "", "");
			mapFreeActions.put(name, newEthos);
		}
		for (Entry<String, FreeAction> entry: FreeActionsSociety.freeActionsInterSociety.entrySet()) {
			String name = "FreeActionsSociety " + entry.getKey();
			Ethos newEthos = new Ethos(name, 0, "", "");
			mapFreeActions.put(name, newEthos);
		}
		ethosSet.ethosTowardsFreeActions = mapFreeActions;
		
		ethosSet.initAllEthosSaved();
		
		//Determine ethos towards other ethos, i.e. judgments  
		for (Ethos ethos: ethosSet.getAllEthos()) {
			String name = ethos.name;
			Ethos newEthos = new Ethos(name, 0, "", "");
			ethosSet.ethosTowardsOtherEthos.put(ethos, newEthos);
		}
		
		initValuesToHumanEthos(ethosSet);
	}

	//Assign random, sensible values to ethos
	private static void initValuesToHumanEthos(EthosSet ethosSet) {
		for (Entry<String, Ethos> entry: ethosSet.greatEthos.entrySet()) {
			Ethos ethos = entry.getValue();
			ethos.severity = Math.random() * 60 - 30;
			
			Ethos ethosOpinion = ethosSet.ethosTowardsOtherEthos.get(ethos);
			if (ethosOpinion != null) {
				double offset = Math.random() * 40 - 20;
				ethosOpinion.severity = ethos.severity + offset;
			}
		}
		for (Entry<String, Ethos> entry: ethosSet.ethosPersonalityTraits.entrySet()) {
			Ethos ethos = entry.getValue();
			ethos.severity = Math.random() * 100 - 50;
			
			Ethos ethosOpinion = ethosSet.ethosTowardsOtherEthos.get(ethos);
			if (ethosOpinion != null) {
				double offset = Math.random() * 40 - 20;
				ethosOpinion.severity = ethos.severity + offset;
			}
		}
	}
	
	/**
	 * Evaluate a person's thoughts based on their 'base' (ingrained opinions since birth),
	 * and their experiences, to gradually evolve systems of thought over time.
	 * @param human The human in question
	 * 
	 * TODO
	 * Experiences can individually affect one's perception of world, in the short and long term.
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
	public static void influenceHumanBrain(HumanBrain newBrain, List<EthosSet> otherInfluencingEthos, 
			Society society) {
		double n = otherInfluencingEthos.size();
		for (EthosSet otherEthosSet: otherInfluencingEthos) {
			for (Entry<String, Ethos> entry: otherEthosSet.greatEthos.entrySet()) {
				newBrain.ethosSet.greatEthos.get(entry.getKey()).severity += 
						entry.getValue().severity / n;
			}
			for (Entry<String, Ethos> entry: otherEthosSet.ethosPersonalityTraits.entrySet()) {
				newBrain.ethosSet.ethosPersonalityTraits.get(entry.getKey()).severity += 
						entry.getValue().severity / n;
			}
			for (Entry<String, Ethos> entry: otherEthosSet.ethosEconomics.entrySet()) {
				newBrain.ethosSet.ethosEconomics.get(entry.getKey()).severity += 
						entry.getValue().severity / n;
			}
			for (Entry<LocalProcess, Ethos> entry: otherEthosSet.ethosTowardsProcesses.entrySet()) {
				newBrain.ethosSet.ethosTowardsProcesses.get(entry.getKey()).severity += 
						entry.getValue().severity / n;
			}
			for (Entry<Integer, Ethos> entry: otherEthosSet.ethosTowardsItems.entrySet()) {
				newBrain.ethosSet.ethosTowardsItems.get(entry.getKey()).severity += 
						entry.getValue().severity / n;
			}
			for (Entry<Ethos, Ethos> entry: otherEthosSet.ethosTowardsOtherEthos.entrySet()) {
				newBrain.ethosSet.ethosTowardsOtherEthos.get(entry.getKey()).severity += 
						entry.getValue().severity / n;
			}
		}
	}
	
	
	
}
