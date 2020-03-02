package io.github.dantetam.world.process.priority;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.grid.LocalTile;
import io.github.dantetam.world.life.Human;
import io.github.dantetam.world.life.LivingEntity;

/*
 * When two people meet, they must go towards one location.
 * This is represented by a composite map of MovePriority.
 * 
 * This condition is done when all people have met near the designated area.
 */

public class PeopleMeetPriority extends Priority {
	
	public Collection<Human> allPeople;
	public Map<Human, MovePriority> differentMovePriors;
	
	public PeopleMeetPriority(Vector3i coords) {
		super(coords);
	}

	public void initAllPeoplePaths(Map<Human, List<LocalTile>> allCalculatedPaths) {
		differentMovePriors = new HashMap<>();
		for (Entry<Human, List<LocalTile>> entry: allCalculatedPaths.entrySet()) {
			MovePriority movePrior = new MovePriority(this.coords, true); //Tolerate a distance of one
			movePrior.initPath(entry.getValue());
			differentMovePriors.put(entry.getKey(), movePrior);
		}
		allPeople = allCalculatedPaths.keySet();
	}
	
	//TODO: Make this a standard part of the interface? For priorities involving more than one person
	public void removeThisMeetPrior() {
		for (LivingEntity personMeetingUp: differentMovePriors.keySet()) {
			if (personMeetingUp.activePriority.equals(this)) {
				personMeetingUp.activePriority = null;
			}
		}
	}

}
