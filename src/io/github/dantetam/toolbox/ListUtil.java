package io.github.dantetam.toolbox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class ListUtil {

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
	
	public static <T> Collection<T> colnsIntersection(Collection<T>... collections) {
		if (collections == null || collections.length <= 1) {
			throw new IllegalArgumentException("Collection intersection not defined for empty/null amount of collections");
		}
			
		Collection<T> minSizeColn = null;
		for (Collection<T> coln: collections) {
			if (minSizeColn == null || coln.size() < minSizeColn.size()) {
				minSizeColn = coln;
			}
		}
		
		Collection<T> intersection = new ArrayList<>();
		for (T item: minSizeColn) {
			boolean itemFoundAll = true;
			for (Collection coln: collections) {
				if (coln.equals(minSizeColn)) continue;
				if (!minSizeColn.contains(item)) {
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
	
	public static <T> boolean colnsHasIntersect(Collection<T>... collections) {
		return colnsIntersection(collections).size() > 0;
	}
	
}
