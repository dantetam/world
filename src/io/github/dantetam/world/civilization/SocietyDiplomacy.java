package io.github.dantetam.world.civilization;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import io.github.dantetam.world.civhumanrelation.SocietySocietyRel;
import io.github.dantetam.world.combat.War;

public class SocietyDiplomacy {

	private Map<String, Society> societiesByName;
	private Map<Society, Map<Society, SocietySocietyRel>> relationships;
	
	public SocietyDiplomacy() {
		societiesByName = new HashMap<>();
	}
	
	public void addSociety(Society society) {
		this.societiesByName.put(society.name, society);
		relationships.put(society, new HashMap<>());
	}
	
	public void declareWar(Society attacker, Society defender) {
		 TODO
	}
	
	public void declarePeace(War warToEnd) {
		
	}
	
	public Collection<Society> getAllSocieties() {
		return societiesByName.values();
	}
	
}
