package io.github.dantetam.world.civilization;

import java.util.Date;

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
	
	public LocalEvent(Date timeEvent, String type, double opinion) {
		this.timeEvent = timeEvent;
		this.type = type;
		this.opinion = opinion;
	}
	
}
