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

public class StringDoubleGamut {

	private Map<String, Double> stringDoubleGamut;
	
	protected StringDoubleGamut() {
		this.stringDoubleGamut = new HashMap<>();
	}
	
	protected StringDoubleGamut(StringDoubleGamut gamut) {
		this.stringDoubleGamut = new HashMap<>(gamut.stringDoubleGamut);
	}
	
	public void addEmotion(String emotion, double value) {
		if (getFields().contains(emotion)) {
			MapUtil.addNumMap(stringDoubleGamut, emotion, value);
		}
		else {
			throw new IllegalArgumentException("Could not find given emotion in gamut (emotion range): " + emotion);
		}
	}
	
	public boolean hasEmotion(String emotion) {
		return getFields().contains(emotion);
	}
	
	public double getEmotion(String emotion) {
		if (hasEmotion(emotion)) {
			if (stringDoubleGamut.containsKey(emotion)) {
				return stringDoubleGamut.get(emotion);
			}
			return 0;
		}
		else {
			throw new IllegalArgumentException("Could not find given emotion in gamut (emotion range): " + emotion);
		}
	}
	
	public <U extends Number> double dotProduct(Map<String, U> weights) {
		double sum = 0;
		for (Entry<String, Double> emotion: stringDoubleGamut.entrySet()) {
			if (weights.containsKey(emotion.getKey())) {
				sum += weights.get(emotion.getKey()).doubleValue() * emotion.getValue();
			}
		}
		return sum;
	}
	public double dotProduct(StringDoubleGamut gamut) {
		return this.dotProduct(gamut.stringDoubleGamut);
	}
	
	public Map<String, Double> productWeights(Map<String, Double> weights) {
		Map<String, Double> newWeights = new HashMap<>();
		for (Entry<String, Double> emotion: stringDoubleGamut.entrySet()) {
			if (weights.containsKey(emotion.getKey())) {
				newWeights.put(emotion.getKey(), weights.get(emotion.getKey()) * emotion.getValue());
			}
		}
		return newWeights;
	}
	
	public StringDoubleGamut addKeyGamut(StringDoubleGamut gamut) {
		StringDoubleGamut newGamut = new StringDoubleGamut(this);
		MapUtil.addMapToMap(newGamut.stringDoubleGamut, gamut.stringDoubleGamut);
		return newGamut;
	}
	
	public Set<String> getFields() {
		return null;
	}
	
}
