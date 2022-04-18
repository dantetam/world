package io.github.dantetam.toolbox;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.sun.tools.javac.util.Assert;

import io.github.dantetam.toolbox.log.CustomLog;
import io.github.dantetam.world.ai.Pathfinder.ScoredPath;
import io.github.dantetam.world.grid.LocalTile;

public class MapUtil {

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
			CustomLog.outPrintln();
		}
	}

	/**
	 * Safe addition into a map (null check). 
	 * Add value to key k, defaulting to k -> 0 when k is not a key.
	 */
	public static <T, U extends Number> void 
			addNumMap(Map<T, U> map, T key, Number value) {
		if (!map.containsKey(key)) {
			map.put(key, (U) new Double(value.doubleValue()));
		}
		else {
			map.put(key, (U) new Double(map.get(key).doubleValue() + value.doubleValue()));
		}
	}
	
	public static <T, U extends Number> double 
			dotProductOfMaps(Map<T, U> mapA, Map<T, U> mapB) {
		double product = 0;
		for (Entry<T, U> entry: mapA.entrySet()) {
			T key = entry.getKey();
			if (mapB.containsKey(key)) {
				product += mapA.get(key).doubleValue() * mapB.get(key).doubleValue();
			}
		}
		return product;
	}
	
	public static <T, U extends Number> void 
			addMapToMap(Map<T, U> baseMap, Map<T, U> additions) {
		for (Entry<T, U> addition: additions.entrySet()) {
			addNumMap(baseMap, addition.getKey(), addition.getValue());
		}
	}
	
	public static <T, U extends Number> Map<T, U> 
			getMapSubtractByMap(Map<T, U> baseMap, Map<T, U> subtractions) {
		Map<T, U> results = new HashMap<>();
		for (Entry<T, U> base: baseMap.entrySet()) {
			T key = base.getKey();
			if (subtractions.containsKey(base)) {
				results.put(key, (U) new Double(base.getValue().doubleValue() - subtractions.get(key).doubleValue()));
			}
			else {
				results.put(key, (U) new Double(base.getValue().doubleValue()));
			}
		}
		for (Entry<T, U> subtraction: subtractions.entrySet()) {
			T key = subtraction.getKey();
			if (!baseMap.containsKey(key)) {
				results.put(key, (U) new Double(-subtractions.get(key).doubleValue()));
			}
		}
		return results;
	}
	
	public static <T, U> boolean checkKeyValue(Map<T, U> map, T key, U value) {
		if (!map.containsKey(key)) {
			return false;
		}
		else {
			return map.get(key).equals(value);
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
	
	public static <K, V> void insertNestedListMap(Map<K, List<V>> map, K key, V value) {
		if (!map.containsKey(key)) {
			map.put(key, new ArrayList<V>());
		}
		map.get(key).add(value);
	}
	public static <K, V> void insertNestedSetMap(Map<K, Set<V>> map, K key, V value) {
		if (!map.containsKey(key)) {
			map.put(key, new HashSet<V>());
		}
		map.get(key).add(value);
	}
	
	public static <K, V> void removeSafeNestListMap(Map<K, List<V>> map, K key, V value) {
		if (map.containsKey(key)) {
			map.get(key).remove(value);
		}
	}
	public static <K, V> void removeSafeNestSetMap(Map<K, Set<V>> map, K key, V value) {
		if (map.containsKey(key)) {
			map.get(key).remove(value);
		}
	}
	
	public static <K, V> List<V> getAllItemsNestMap(Map<K, Collection<V>> map) {
		List<V> items = new ArrayList<>();
		for (Entry<K,Collection<V>> entry: map.entrySet()) {
			for (V value: entry.getValue()) {
				items.add(value);
			}
		}
		return items;
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
	
	/**
	 * @param map A mapping of items to probabilities, not necessarily normalized
	 * @return A random choice from the normalized weight map. Null iff the map is empty
	 */
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
	
	/**
	 * @param map A mapping of items to probabilities, not necessarily normalized
	 * @return A random choice with uniform probability across the keys. Null iff the map is empty
	 */
	public static <T, U> T randChoiceFromMapUniform(Map<T, U> map) {
		if (map.size() == 0)
			return null;
 		int index = (int) (Math.random() * map.size());
 		Iterator<T> keys = map.keySet().iterator();
 		while (keys.hasNext()) { //Return the key in the numbered index
 			T key = keys.next();
 			if (index == 0) {
 				return key;
 			}
 		}
 		return null;
	}
	
	public static <U extends Number> Object randChoiceFromMaps(Map<?, U>... maps) {
		return randChoiceFromMapsHelper(false, maps);
	}
	
	/*
	 * Return a random Object from one of the maps, after removing that item from the corresponding map.
	 */
	public static <U extends Number> Object randChoiceFromMapsRemove(Map<?, U>... maps) {
		return randChoiceFromMapsHelper(true, maps);
	}
	
	private static <U extends Number> Object randChoiceFromMapsHelper(boolean remove, Map<?, U>... maps) {
		Map<Object, U> allItemsMap = new HashMap<>();
		for (Map<?, U> map: maps) {
			for (Entry<?, U> entry: map.entrySet()) {
				allItemsMap.put(entry.getKey(), entry.getValue());
			}
		}
		Object result = randChoiceFromWeightMap(allItemsMap);
		if (remove) {
			for (Map<?, U> map : maps) {
				if (map.containsKey(result)) {
					map.remove(result);
				}
			}
		}
		return result;
	}

	//Ascending sort on a generic mapping
	public static <K, V extends Comparable<? super V>> LinkedHashMap<K, V> getSortedMapByValueAsc(Map<K, V> map) {
        List<Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort(Entry.comparingByValue());
        LinkedHashMap<K, V> result = new LinkedHashMap<>();
        for (Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }
	
	//Descending sort on a generic mapping
	public static <K, V extends Comparable<? super V>> LinkedHashMap<K, V> getSortedMapByValueDesc(Map<K, V> map) {
        List<Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort(Entry.comparingByValue());
        LinkedHashMap<K, V> result = new LinkedHashMap<>();
        for (int i = list.size() - 1; i >= 0; i--) {
        	Entry<K, V> entry = list.get(i);
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }
	
	//Ascending sort using a custom entry comparator
	public static <K, V extends Comparable<? super V>> LinkedHashMap<K, V> getMapByValueAscComp(Map<K, V> map,
			Comparator<Entry<K, V>> comparator) {
        List<Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort(comparator);
        LinkedHashMap<K, V> result = new LinkedHashMap<>();
        for (Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }
	
	//Descending sort using a custom entry comparator
	public static <K, V extends Comparable<? super V>> LinkedHashMap<K, V> getMapByValueDescComp(Map<K, V> map,
			Comparator<Entry<K, V>> comparator) {
        List<Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort(comparator);
        LinkedHashMap<K, V> result = new LinkedHashMap<>();
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
		data.put(6, 19);
		data.put(7, 17);
		data = getSortedMapByValueDesc(data);
		CustomLog.outPrintln(data);
		CustomLog.outPrintln(Arrays.toString(data.entrySet().toArray()));
		
		CustomLog.outPrintln();
		Iterator<Integer> keys = data.keySet().iterator();
		while (keys.hasNext()) {
			CustomLog.outPrintSameLine(keys.next() + " ");
		}
		
		Map<String, Integer> data2 = new HashMap<>();
		data2.put("String", 2);
		data2.put("String2", 9);
		data2.put("String3", 7);
		
		int totalSize = data.size() + data2.size(); //.size() method changes result on map removal
		for (int i = 0; i < totalSize; i++) {
			MapUtil.randChoiceFromMapsRemove(data, data2);
		}
		
		CustomLog.outPrintln(data);
		CustomLog.outPrintln(data2);
		
		Assert.check(data.size() == 0 && data2.size() == 0, 
				"randChoiceFromMapsRemove should all elements of both maps");
	}
	
}
