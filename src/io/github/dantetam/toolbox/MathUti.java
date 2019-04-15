package io.github.dantetam.toolbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class MathUti {

	public static double roundToPower2(double n) {
		return Math.pow(2, Math.round(Math.log10(n) / Math.log10(2)));
	}
	
	public static void printTable(double[][] terrain) {
		for (int r = 0; r < terrain.length; r++) {
			for (int c = 0; c < terrain[0].length; c++) {
				String data = terrain[r][c] + "";
				//String data = String.format("%03d", (int) terrain[r][c]);
				/*
				if (terrain[r][c] <= 0) {
					data = "___";
				}
				*/
				System.out.print(data + " ");
			}
			System.out.println();
		}
	}
	
	//Return a number from a to b, inclusive and equal chance across the range.
	public static int discreteUniform(int a, int b) {
		if (a > b) {
			int temp = a;
			a = b;
			b = temp;
		}
		int range = b - a + 1;
		return (int) (Math.random() * range) + a;
	}
	
	public static <T, U extends Number> void 
			addNumMap(Map<T, U> map, T key, U value) {
		if (!map.containsKey(key)) {
			map.put(key, value);
		}
		else {
			map.put(key, (U) new Double(map.get(key).doubleValue() + value.doubleValue()));
		}
	}
	
	public static <T, U extends Number> void 
		insertKeepMaxMap(Map<T, U> map, T key, U value) {
		if (!map.containsKey(key)) {
			map.put(key, value);
		}
		else {
			map.put(key, (U) new Double(Math.max(map.get(key).doubleValue(), value.doubleValue())));
		}
	}
	
	public static <T, U extends Number> Map<T, Double> getNormalizedMap(Map<T, U> map) {
		double sum = 0;
		for (Entry<T, U> entry: map.entrySet()) {
			sum += entry.getValue().doubleValue();
		}
		Map<T, Double> normalized = new HashMap<>();
		for (Entry<T, U> entry: map.entrySet()) {
			normalized.put(entry.getKey(), entry.getValue().doubleValue() / sum);
		}
		return normalized;
	}
	
	public static <T, U extends Number> T randChoiceFromWeightMap(Map<T, U> map) {
		Map<T, Double> normalized = getNormalizedMap(map);
		double random = Math.random();
		double counter = 0;
		Set<T> keySet = normalized.keySet();
		for (T key: keySet) {
			counter += normalized.get(key);
			if (random <= counter) return key; 
		}
		return null;
	}

	//Ascending sort on a generic mapping
	public static <K, V extends Comparable<? super V>> Map<K, V> getSortedMapByValue(Map<K, V> map) {
        List<Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort(Entry.comparingByValue());

        Map<K, V> result = new LinkedHashMap<>();
        for (Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }
	
	//Descending sort on a generic mapping
	public static <K, V extends Comparable<? super V>> Map<K, V> getSortedMapByValueDesc(Map<K, V> map) {
        List<Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort(Entry.comparingByValue());

        Map<K, V> result = new LinkedHashMap<>();
        for (int i = list.size() - 1; i >= 0; i--) {
        	Entry<K, V> entry = list.get(i);
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }
	
	public static List<String> wrapString(String longString, int wrapWidth) {
		List<String> strings = new ArrayList<>();
		while (true) {
			if (longString.length() <= wrapWidth) {
				strings.add(longString);
				return strings;
			}
			else {
				strings.add(longString.substring(0, wrapWidth));
				longString = longString.substring(wrapWidth);
			}
		}
	}
	
	public static void main(String[] args) {
		Map<Integer, Integer> data = new HashMap<>();
		data.put(1, 2);
		data.put(3, 9);
		data.put(4, 7);
		data = getSortedMapByValueDesc(data);
		System.out.println(data);
		System.out.println(Arrays.toString(data.entrySet().toArray()));
	}
	
}
