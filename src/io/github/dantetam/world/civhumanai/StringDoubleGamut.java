package io.github.dantetam.world.civhumanai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import io.github.dantetam.toolbox.MapUtil;

/**
 * 
 *  
 * @author Dante
 *
 */

public abstract class StringDoubleGamut {

	private Map<String, Double> stringDoubleGamut;
	
	public static final Set<String> EMOTIONS = new HashSet<String>() {{
		add("Kindness"); add("Honor"); add("Attraction"); add("Admiration");                                                                                                                                                                          
		add("Rationality"); 
		add("Hate"); add("Indifference");
	}};
	
	protected StringDoubleGamut() {
		this.stringDoubleGamut = new HashMap<>();
	}
	
	public void addEmotion(String emotion, double value) {
		if (EMOTIONS.contains(emotion)) {
			MapUtil.addNumMap(stringDoubleGamut, emotion, value);
		}
		else {
			throw new IllegalArgumentException("Could not find given emotion in gamut (emotion range): " + emotion);
		}
	}
	
	public double getEmotion(String emotion) {
		if (EMOTIONS.contains(emotion)) {
			if (stringDoubleGamut.containsKey(emotion)) {
				return stringDoubleGamut.get(emotion);
			}
			return 0;
		}
		else {
			throw new IllegalArgumentException("Could not find given emotion in gamut (emotion range): " + emotion);
		}
	}
	
	public double dotProductWeights(Map<String, Double> weights) {
		double sum = 0;
		for (Entry<String, Double> emotion: stringDoubleGamut.entrySet()) {
			if (weights.containsKey(emotion.getKey())) {
				sum += weights.get(emotion.getKey()) * emotion.getValue();
			}
		}
		return sum;
	}
	
}
