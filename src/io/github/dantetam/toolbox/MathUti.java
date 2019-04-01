package io.github.dantetam.toolbox;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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

	public static <K, V extends Comparable<? super V>> Map<K, V> sortMapByValue(Map<K, V> map) {
        List<Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort(Entry.comparingByValue());

        Map<K, V> result = new LinkedHashMap<>();
        for (Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }
	
}