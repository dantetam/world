package io.github.dantetam.world.civilization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import io.github.dantetam.toolbox.MapUtil;
import io.github.dantetam.world.civhumanrelation.HumanHumanRel;
import io.github.dantetam.world.civilization.SocietyLeadership.SocSuccessionType;
import io.github.dantetam.world.civilization.SocietyLeadership.SocietyLeadershipMode;
import io.github.dantetam.world.life.Human;

/**
 * 
 */

public class SocietyLeadershipSuccession {

	public static List<Human> determineSuccessors(Society society, 
			SocietyLeadershipMode mode, SocSuccessionType successType) {
		Map<Human, Double> candidates = findCandidates(society, mode, successType);
		Iterator<Human> humanIter = candidates.keySet().iterator();
		//Succession process, by the criteria and willingness of people to compete for power
		
		List<Human> finalLeaders = new ArrayList<>();
		if (mode == SocietyLeadershipMode.GROUP || mode == SocietyLeadershipMode.SENATE ||
				mode == SocietyLeadershipMode.FULL_DEMOCRACY) {
			int numPeople = Math.min(candidates.size(), Math.min((int) (candidates.size() * 0.2), 5));
			for (int i = 0; i < numPeople; i++) {
				finalLeaders.add(humanIter.next());
			}
		}
		else {
			Human firstHuman = humanIter.next();
			int numPeople = Math.min(candidates.size(), Math.min((int) (candidates.size() * 0.2), 5));
			while (humanIter.hasNext() && finalLeaders.size() < numPeople) {
				finalLeaders.add(firstHuman);
				
				double firstScore = candidates.get(firstHuman);
				Human nextCompetitor = humanIter.next();
				double nextScore = candidates.get(nextCompetitor);
				
				double relCompete = nextCompetitor.brain.getHumanRel(firstHuman).opinion;
				
				//TODO: Design custom multivariate functions for this use in mathematical util calculations
				//Design a generic class that can be overridden with a multivariate function using
				//string keys as variables
				double competeUtil = 10 * (1 - nextScore / firstScore) + Math.max(-relCompete / 20, 0);
				
				//Repeat the cycle, guaranteeing an addition and bringing in another competitor
				if (competeUtil > 3) {
					firstHuman = nextCompetitor;
				}
			}
		}
		return finalLeaders;
	}
	
	public static Map<Human, Double> findCandidates(Society society, 
			SocietyLeadershipMode mode, SocSuccessionType successType) {
		//Pick people based on the societal criteria for new people to rule
		//Use people traits as well
		int numPeopleChoose = Math.min((int) (Math.random() * 5) + 5, society.getAllPeople().size());
		
		Map<Human, Double> scoring = new HashMap<>();
		for (Human human: society.getAllPeople()) {
			double score = 0;
			if (successType == SocSuccessionType.STRENGTH || successType == SocSuccessionType.PRESTIGE ||
					successType == SocSuccessionType.OLIGARCHIC) {
				score = human.getTotalPowerPrestige();
			}
			else if (successType == SocSuccessionType.LANDED_DEMOCRACY) {
				score = Math.log10(human.getTotalPowerPrestige()) + human.skillBook.getSkillLevel("Rhetoric");
			}
			else {
				score = human.skillBook.getSkillLevel("Rhetoric");
				double averageRel = 0;
				for (Human otherHuman: society.getAllPeople()) {
					if (human.equals(otherHuman)) continue;
					HumanHumanRel rel = human.brain.getHumanRel(otherHuman);
					averageRel += rel.emotionGamut.getEmotion("Admiration") + rel.opinion * 0.35;
				}
				score += averageRel * 0.3;
			}
			scoring.put(human, score);
		}
		scoring = MapUtil.getSortedMapByValueDesc(scoring);
		Iterator<Entry<Human, Double>> bestCandidates = scoring.entrySet().iterator();
		
		Map<Human, Double> results = new HashMap<>();
		while (results.size() < numPeopleChoose && bestCandidates.hasNext()) {
			Entry<Human, Double> entry = bestCandidates.next();
			results.put(entry.getKey(), entry.getValue());
		}
		return results;
	}
	
}
