package io.github.dantetam.world.civilization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.dantetam.world.civhumanrelation.SocietySocietyRel;
import io.github.dantetam.world.civilization.SocietyLeadership.SocSuccessionType;
import io.github.dantetam.world.civilization.SocietyLeadership.SocietyLeadershipMode;
import io.github.dantetam.world.life.Human;

/**
 * 
 */

public class SocietyLeadershipSuccession {

	public static List<Human> determineSuccessors(Society society, 
			SocietyLeadershipMode mode, SocSuccessionType successType) {
		//TODO;
		List<Human> candidates = findCandidates(society, mode, successType);
		Human randHuman = candidates.get((int) (Math.random() * candidates.size()));
		return new ArrayList<Human>() {{add(randHuman);}};
	}
	
	public static List<Human> findCandidates(Society society, 
			SocietyLeadershipMode mode, SocSuccessionType successType) {
		//TODO;
		Set<Integer> indices = new HashSet<>();
		int numPeople = Math.min(10, society.getAllPeople().size());
		while (indices.size() < numPeople) {
			int randIndex = (int) (Math.random() * society.getAllPeople().size());
			if (!indices.contains(randIndex)) {
				indices.add(randIndex);
			}
		}
		List<Human> candidates = new ArrayList<>();
		for (Integer indice: indices) {
			candidates.add(society.getAllPeople().get(indice));
		}
		return candidates;
	}
	
}
