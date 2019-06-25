package io.github.dantetam.world.civilization;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.dantetam.toolbox.MapUtil;
import io.github.dantetam.world.life.LivingEntity;

/**
 * A representation of a memory, a set of LocalEvent objects which come together to form
 * a coherent theme in the memory of a sentient being.
 * 
 * @author Dante
 *
 * TODO Implement usage into storing really big memories (extreme or important, like created art, 
 * or married someone)
 *
 */

public class LocalExperience {

	public List<LocalEvent> events;
	public double opinion;
	public String type;
	public Map<LivingEntity, Set<String>> beingRoles;
	
	public LocalExperience(String type) {
		this(type, new ArrayList<LocalEvent>());
	}
	
	public LocalExperience(String type, List<LocalEvent> events) {
		this.type = type;
		
		this.events = events;
		opinion = 0;
		for (LocalEvent event: events) {
			opinion += event.opinion;
		}
	}
	
	public void addRole(LivingEntity being, String role) {
		MapUtil.insertNestedSetMap(beingRoles, being, role);
	}
	
}
