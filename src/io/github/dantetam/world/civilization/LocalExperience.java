package io.github.dantetam.world.civilization;

import java.util.List;

/**
 * A representation of a memory, a set of LocalEvent objects which come together to form
 * a coherent theme in the memory of a sentient being.
 * 
 * @author Dante
 *
 */

public class LocalExperience {

	public List<LocalEvent> events;
	public double opinion;
	
	public LocalExperience(List<LocalEvent> events) {
		this.events = events;
		opinion = 0;
		for (LocalEvent event: events) {
			opinion += event.opinion;
		}
	}
	
}
