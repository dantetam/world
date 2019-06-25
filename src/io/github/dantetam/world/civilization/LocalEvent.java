package io.github.dantetam.world.civilization;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import io.github.dantetam.toolbox.MapUtil;
import io.github.dantetam.world.life.LivingEntity;

/**
 * Represents a single event that can form a much larger part of one's memory and experience.
 * It represents a single action like falling to the ground or eating a sandwich.
 * @author Dante
 *
 */

public class LocalEvent {

	public Date timeEvent;
	public String type;
	public double opinion;
	public Map<LivingEntity, Set<String>> beingRoles;
	
	public LocalEvent(Date timeEvent, String type, double opinion) {
		this.timeEvent = timeEvent;
		this.type = type;
		this.opinion = opinion;
		beingRoles = new HashMap<>();
	}
	
	public void addRole(LivingEntity being, String role) {
		MapUtil.insertNestedSetMap(beingRoles, being, role);
	}
	
}
