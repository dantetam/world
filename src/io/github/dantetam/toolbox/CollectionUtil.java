package io.github.dantetam.toolbox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

public class CollectionUtil {

	public static <T> Collection<T> getColns(Collection<T>... collections) {
	    List<T> items = new ArrayList<>();
	    for (Collection<T> collection: collections) {
	    	for (T item: collection) {
	    		items.add(item);
	    	}
	    }
	    return items;
	}
	
	public static <T> List<T> getShuffledList(List<T> list) {
		List<T> data = new ArrayList<>();
		for (T item: list) {
			data.add(item);
		}
		Collections.shuffle(data);
		return data;
	}
	
	/**
	 * 
	 * @param collections
	 * @return A collection containing elements found at least once in all collections
	 * 		   If a collection is null or empty, the intersection is empty.
	 * 		e.g. (1, 2, 3) intersect (2, 3) intersect (1, 3, 4) = (3)
	 * 			 () intersect ... = ()
 	 */
	public static <T> Collection<T> colnsIntersection(Collection<? extends T>... collections) {
		if (collections == null || collections.length <= 1) {
			throw new IllegalArgumentException("Collection intersection not defined for empty/null amount of collections");
		}
			
		Collection<? extends T> minSizeColn = null;
		for (Collection<? extends T> coln: collections) {
			if (coln == null) { //Strictly define intersection with an empty/null set as none
				return new ArrayList<>();
			}
			if (minSizeColn == null || coln.size() < minSizeColn.size()) {
				minSizeColn = coln;
			}
		}
		
		Collection<T> intersection = new ArrayList<>();
		for (T item: minSizeColn) {
			boolean itemFoundAll = true;
			for (Collection<? extends T> coln: collections) {
				if (coln.equals(minSizeColn)) continue;
				if (!coln.contains(item)) {
					itemFoundAll = false; 
					break;
				}
			}
			if (itemFoundAll) {
				intersection.add(item);
			}
		}
		return intersection;
	}
	
	public static <T> boolean colnsHasIntersect(Collection<? extends T>... collections) {
		return colnsIntersection(collections).size() > 0;
	}
	
	public static <T> T searchItemCond(Collection<T> data, Function<T, Boolean> condition) {
		for (T item: data) {
			if (condition.apply(item)) {
				return item;
			}
		}
		return null;
	}
	
	public static <K, V> Map<K, V> searchMapCondByKey(Map<K, V> map, 
			Function<K, Boolean> condition) {
		Map<K, V> results = new HashMap<>();
		for (Entry<K, V> entry: map.entrySet()) {
			if (condition.apply(entry.getKey())) {
				results.put(entry.getKey(), entry.getValue());
			}
		}
		return results;
	}
	public static <K, V> Map<K, V> searchMapCondByValue(Map<K, V> map, 
			Function<V, Boolean> condition) {
		Map<K, V> results = new HashMap<>();
		for (Entry<K, V> entry: map.entrySet()) {
			if (condition.apply(entry.getValue())) {
				results.put(entry.getKey(), entry.getValue());
			}
		}
		return results;
	}
	
	/**
	 * 
	 * @param collections
	 * @return A collection containing elements found at least once in _any_ collection
	 * 		e.g. (1, 2, 3) intersect (2, 3) intersect (1, 3, 4) = (1, 2, 3, 4)
	 * 			 () union ... = ...
 	 */
	public static <T> Set<T> colnsUnionUnique(Collection<T>... collections) {
		if (collections == null || collections.length <= 1) {
			throw new IllegalArgumentException("Collection intersection not defined for empty/null amount of collections");
		}
		
		Set<T> intersection = null;
		for (Collection<T> coln: collections) {
			if (coln == null) {
				continue;
			}
			if (intersection == null) {
				intersection = new HashSet<>(coln);
			}
			else {
				for (T item: coln) {
					intersection.add(item);
				}
			}
		}
		return intersection;
	}
	
	public static <T> Set<T> newSet(T... elements) {
		Set<T> set = new HashSet<T>();
		for (T element: elements) {
			set.add(element);
		}
		return set;
	}
	
	public static <T> List<T> newList(T... elements) {
		List<T> set = new ArrayList<T>();
		for (T element: elements) {
			set.add(element);
		}
		return set;
	}
	
	
	public static void main(String[] args) {
		Set<Integer> setA = new HashSet<Integer>() {{add(1); add(2); add(3);}};
		Set<Integer> setB = new HashSet<Integer>() {{add(2); add(3);}};
		Set<Integer> setC = new HashSet<Integer>() {{add(1); add(3); add(4);}};
		Collection<Integer> intersect = colnsIntersection(setA, setB, setC); 
		System.err.println(intersect);
	}
	
}
