package io.github.dantetam.world.life;

import java.util.List;
import java.util.Map.Entry;

import io.github.dantetam.toolbox.MapUtil;
import io.github.dantetam.world.civilization.Society;

public class HumanBrainInitialize {

	public static void influenceHumanBrain(HumanBrain newBrain, List<HumanBrain> otherBrains, 
			Society society) {
		int n = otherBrains.size();
		for (HumanBrain brain: otherBrains) {
			for (Entry<String, Double> entry: brain.languageCodesStrength.entrySet()) {
				MapUtil.addNumMap(newBrain.languageCodesStrength, entry.getKey(), 
						entry.getValue() / n);
			}
		}
	}
	
}
