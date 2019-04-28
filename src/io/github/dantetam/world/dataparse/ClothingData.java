package io.github.dantetam.world.dataparse;

import java.util.HashSet;
import java.util.Set;

public class ClothingData {

	public static Set<String> initBeingName(String name) {
		if (name.equals("Human")) {
			return new HashSet<String>() {{
				add("Hat");
				add("Shirt");
				add("Pants");
			}};
		}
		throw new IllegalArgumentException("Could not instantiate clothes slots for missing being type: " + name);
	}
	
}
