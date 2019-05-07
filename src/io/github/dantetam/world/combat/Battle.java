package io.github.dantetam.world.combat;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.civilization.LivingEntity;
import io.github.dantetam.world.grid.LocalGrid;

public class Battle {

	public LocalGrid grid;
	public Vector3i battleCenter;
	
	public List<Set<LivingEntity>> combatantTeams;
	
	public BattleMode battlePhase;
	public int battlePhaseTicksLeft;
	
	public static enum BattleMode {
		PREPARE, SHOCK, DANCE
	}
	
	public Battle(List<Set<LivingEntity>> combatantTeams) {
		
	}
	
	public List<LivingEntity[]> adjacentCombatEntities() {
		for (int groupIndex = 0; groupIndex < combatantTeams.size(); groupIndex++) {
			for (int otherGroupIndex = 0; otherGroupIndex < combatantTeams.size(); otherGroupIndex++) {
				if (groupIndex <= otherGroupIndex) continue; //Return unique pairs of combatants, without repeating reversed pairs
				Set<LivingEntity> groupCopy = new HashSet<>(combatantTeams.get(groupIndex));
				Set<LivingEntity> otherGroupCopy = new HashSet<>(combatantTeams.get(groupIndex));
					//Collection<LivingEntity> people = grid.getNearestPeopleList(groupMem.location.coords);
					
				for 
			}
		}
	}
	
	
}
