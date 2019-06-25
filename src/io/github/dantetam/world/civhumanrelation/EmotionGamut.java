package io.github.dantetam.world.civhumanrelation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import io.github.dantetam.toolbox.MapUtil;

/**
 * Represents a range of different emotions with distinct values. This is needed
 * to capture nuances in emotion beyond good and bad. For example, being friends with someone,
 * does not mean getting married to that person.
 * 
 * @author Dante
 *
 */

public class EmotionGamut {

	private Map<String, Double> emotionGamut;
	
	public static final Set<String> EMOTIONS = new HashSet<String>() {{
		add("Kindness"); add("Honor"); add("Attraction"); add("Admiration");
		add("Rationality"); 
		add("Hate"); add("Indifference");
	}};
	
	public EmotionGamut() {
		this.emotionGamut = new HashMap<>();
	}
	
	public void addEmotion(String emotion, double value) {
		if (EMOTIONS.contains(emotion)) {
			MapUtil.addNumMap(emotionGamut, emotion, value);
		}
		else {
			throw new IllegalArgumentException("Could not find given emotion in gamut (emotion range): " + emotion);
		}
	}
	
	public double getEmotion(String emotion) {
		if (EMOTIONS.contains(emotion)) {
			if (emotionGamut.containsKey(emotion)) {
				return emotionGamut.get(emotion);
			}
			return 0;
		}
		else {
			throw new IllegalArgumentException("Could not find given emotion in gamut (emotion range): " + emotion);
		}
	}
	
	public double dotProductWeights(Map<String, Double> weights) {
		double sum = 0;
		for (Entry<String, Double> emotion: emotionGamut.entrySet()) {
			if (weights.containsKey(emotion.getKey())) {
				sum += weights.get(emotion.getKey()) * emotion.getValue();
			}
		}
		return sum;
	}
	
}
