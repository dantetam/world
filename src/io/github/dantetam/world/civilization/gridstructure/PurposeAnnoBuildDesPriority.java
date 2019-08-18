package io.github.dantetam.world.civilization.gridstructure;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.AbstractMap.SimpleEntry;

import io.github.dantetam.world.civhumanai.NeedsGamut;
import io.github.dantetam.world.life.Human;
import io.github.dantetam.world.life.LivingEntity;

/**
 * Use the rooms in PurposeAnnoBuildDesign to figure out what a human needs to do with space,
 * factoring in certain needs of the human and the space and type of space available.
 * 
 * For example, a household works primarily in textiles. This household needs to designate three rooms
 * with these properties and priorities:
 * 
 * Highest priority: bedroom for living, 4x5, requires beds, furniture optional;
 * 2) looming/spinning room for textiles (making cloth and clothes), 4x4, requires loom;
 * 3) storage room, 3x3, requires empty space for storage.
 *
 * 
 * @author Dante
 *
 */

public class PurposeAnnoBuildDesPriority {
	
	public Entry<PurposeAnnotatedBuild, AnnotatedRoom> futureRoomNeedByScore(LivingEntity being, 
			NeedsGamut humanNeedsCalc) {
		//Look all the complexes, and find the best room expansion candidate of each.
		//Then compare these for the final best result.
		Entry<PurposeAnnotatedBuild, AnnotatedRoom> bestEntry = null;
		double bestScore = 0;
		for (List<PurposeAnnotatedBuild> purposeAnnoBuilds: being.designatedBuildsByPurpose.values()) {
			for (PurposeAnnotatedBuild purposeAnnoBuild: purposeAnnoBuilds) {
				LinkedHashMap<AnnotatedRoom, Double> scoredRooms = futureRoomNeedByScore(being, purposeAnnoBuild,
						humanNeedsCalc);
				if (scoredRooms != null && scoredRooms.size() > 0) {
					Entry<AnnotatedRoom, Double> thisComplexBestChoice = scoredRooms.entrySet().iterator().next();
					if (bestEntry == null || thisComplexBestChoice.getValue() > bestScore) {
						bestScore = thisComplexBestChoice.getValue();
						bestEntry = new SimpleEntry<PurposeAnnotatedBuild, AnnotatedRoom>(
								purposeAnnoBuild, thisComplexBestChoice.getKey()
						);
					}
				}
			}
		}
		return bestEntry;
	}
	
	//TODO: Implement Maslow's needs hierarchy object (different scoring of different needs)
	public LinkedHashMap<AnnotatedRoom, Double> futureRoomNeedByScore(LivingEntity being, 
			PurposeAnnotatedBuild complex, NeedsGamut humanNeedsCalc) {
		 
	}
	
}
