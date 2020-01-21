package io.github.dantetam.world.combat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.civhumanrelation.HumanRelationship;
import io.github.dantetam.world.grid.LocalGrid;
import io.github.dantetam.world.life.LivingEntity;

public class Battle {

	public LocalGrid grid;
	public Vector3i battleCenter;
	
	public List<LivingEntity> allPeople;
	public List<Set<LivingEntity>> combatantTeams;
	
	public BattleMode battlePhase;
	public int battlePhaseTicksLeft;
	
	public static enum BattleMode {
		PREPARE, SHOCK, DANCE
	}
	
	public Battle(List<Set<LivingEntity>> combatantTeams) {
		this.combatantTeams = combatantTeams;
		this.allPeople = new ArrayList<>();
		for (Set<LivingEntity> team: combatantTeams) {
			for (LivingEntity entity: team) {
				this.allPeople.add(entity);
			} 
		}
	}
	
	public List<LivingEntity> getCombatantsWithNoNeighbors() {
		int withinDist = 1;
		List<LivingEntity> noNeighborCombatant = new ArrayList<>();
		for (LivingEntity people: allPeople) {
			boolean foundNear = false;
			Collection<LivingEntity> closestPeople = grid.getNearestPeopleList(people.location.coords);
			for (LivingEntity nearPerson: closestPeople) {
				if (people.equals(nearPerson)) continue;
				if (people.location.coords.squareDist(nearPerson.location.coords) <= withinDist) {
					break;
				}
				else {
					
				}
			}
			if (!foundNear) {
				noNeighborCombatant.add(people);
			}
		}
		return noNeighborCombatant;
	}
	
	/**
	 * @return A list of ordered combat partners, organized as attacker and defender,
	 * for use in a battle. A pair is two distinct people that are hostile to each other,
	 * and within range.
	 */
	public List<LivingEntity[]> adjacentCombatEntities() {
		int withinDist = 1;
		
		Map<LivingEntity, List<LivingEntity>> neighborCombatants = new HashMap<>();
		for (LivingEntity people: allPeople) {
			Collection<LivingEntity> closestPeople = grid.getNearestPeopleList(people.location.coords);
			for (LivingEntity nearPerson: closestPeople) {
				if (people.equals(nearPerson)) continue;
				if (people.location.coords.squareDist(nearPerson.location.coords) <= withinDist &&
						HumanRelationship.isHostileTowards(people, nearPerson)) {
					if (!neighborCombatants.containsKey(people)) {
						neighborCombatants.put(people, new ArrayList<>());
					}
					neighborCombatants.get(people).add(nearPerson);
				}
				else {
					break;
				}
			}
		}
		
		List<LivingEntity[]> combatAtkDefPairs = new ArrayList<>();
		for (Entry<LivingEntity, List<LivingEntity>> entry: neighborCombatants.entrySet()) {
			List<LivingEntity> neighbors = entry.getValue();
			int randIndex = (int) (Math.random() * neighbors.size());
			LivingEntity randNeighbor = neighbors.get(randIndex);
			combatAtkDefPairs.add(new LivingEntity[] {entry.getKey(), randNeighbor});
		}
		return combatAtkDefPairs;
	}
	
	
}
