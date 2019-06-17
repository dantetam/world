package io.github.dantetam.toolbox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class ListUtil {

	public static <T> Collection<T> stream(Collection<T>... collections) {
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
	
}
