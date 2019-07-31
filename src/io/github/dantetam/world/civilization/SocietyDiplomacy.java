package io.github.dantetam.world.civilization;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import io.github.dantetam.toolbox.MapUtil;
import io.github.dantetam.world.civhumanrelation.SocietySocietyRel;
import io.github.dantetam.world.civhumanrelation.SocietySocietyRel.SocietalRelationMode;
import io.github.dantetam.world.civhumansocietyai.FreeActionsSociety;
import io.github.dantetam.world.combat.War;
import io.github.dantetam.world.grid.WorldGrid;

public class SocietyDiplomacy {

	private WorldGrid world;
	private Map<String, Society> societiesByName;
	private Map<Society, Map<Society, SocietySocietyRel>> relationships;
	private List<War> activeWars;
	
	public SocietyDiplomacy(WorldGrid world) {
		this.world = world;
		societiesByName = new HashMap<>();
		relationships = new HashMap<>();
		activeWars = new ArrayList<>();
	}
	
	public void addSociety(Society society) {
		this.societiesByName.put(society.name, society);
		relationships.put(society, new HashMap<>());
	}
	
	public void declareWar(Society attacker, Society defender) {
		Date date = world.getTime();
		
		Set<Society> attackerAllies = new HashSet<>(), defenderAllies = new HashSet<>();
		Map<Society, SocietySocietyRel> attackerRels = world.societalDiplomacy.getInterSocietalRel(attacker);
		Map<Society, SocietySocietyRel> defenderRels = world.societalDiplomacy.getInterSocietalRel(defender);
		
		//Call attacker allies in offensive war
		for (Entry<Society, SocietySocietyRel> entry: attackerRels.entrySet()) {			
			Society possibleAlly = entry.getKey();
			if (isObligatedInAtkCall(attacker, possibleAlly, defender)) {
				boolean isDoubleAllied = isObligatedInDefCall(defender, possibleAlly, attacker); 
				boolean warStatus = FreeActionsSociety.callAllyToArms(world, 
						attacker, possibleAlly, defender, isDoubleAllied, date);
				if (warStatus) {
					attackerAllies.add(possibleAlly);
				}
			}
		}
		
		//Call defender allies in offensive war
		for (Entry<Society, SocietySocietyRel> entry: defenderRels.entrySet()) {			
			Society possibleAlly = entry.getKey();
			if (isObligatedInDefCall(defender, possibleAlly, attacker)) {
				boolean isDoubleAllied = isObligatedInAtkCall(attacker, possibleAlly, defender); 
				boolean warStatus = FreeActionsSociety.callAllyToArms(world, 
						defender, possibleAlly, attacker, isDoubleAllied, date);
				if (warStatus) {
					defenderAllies.add(possibleAlly);
				}
			}
		}
		
		War war = new War(attacker, defender, attackerAllies, defenderAllies);
		activeWars.add(war);
		
		List<Society> allSocieties = new ArrayList<>();
		allSocieties.add(attacker); allSocieties.add(defender);
		allSocieties.addAll(attackerAllies); allSocieties.addAll(defenderAllies);
		
		for (Society society: allSocieties) {
			society.warsInvolved.add(war);
		}
		
		List<Society> offensiveSide = new ArrayList<>(attackerAllies);
		offensiveSide.add(attacker);
		Set<Society> defensiveSide = war.getOppositeSide(attacker);
		for (Society society: offensiveSide) {
			for (Society otherSociety: defensiveSide) {
				SocietySocietyRel rel = getInterSocietalRel(society, otherSociety);
				rel.societyRelMode = SocietalRelationMode.WAR;
				
				rel = getInterSocietalRel(otherSociety, society);
				rel.societyRelMode = SocietalRelationMode.WAR;
			}
		}
	}
	
	public void declareTotalPeace(War warToEnd) {
		Set<Society> offensiveSide = warToEnd.getSameSide(warToEnd.warLeaderAttacker);
		Set<Society> defensiveSide = warToEnd.getSameSide(warToEnd.warLeaderDefender);
		for (Society society: offensiveSide) {
			for (Society otherSociety: defensiveSide) {
				if (!areSocietiesAtWar(society, otherSociety)) {
					SocietySocietyRel rel = getInterSocietalRel(society, otherSociety);
					rel.societyRelMode = SocietalRelationMode.NEUTRAL;
					
					rel = getInterSocietalRel(otherSociety, society);
					rel.societyRelMode = SocietalRelationMode.NEUTRAL;  
				}
			}
		}
	}
	
	//See FreeActionsSociety::considerAllFreeActions()
	public void initiateTrade(Society host, Society target) {
		//TODO
		Map<Integer, Double> hostSocUtil = MapUtil.getSortedMapByValueDesc(host.calcUtility);
		Map<Integer, Double> hostTarUtil = MapUtil.getSortedMapByValueDesc(target.calcUtility);
		
		Map<Integer, Double> hostSocCommon = MapUtil.getSortedMapByValueDesc(host.allEconomicUtil);
		Map<Integer, Double> hostTarCommon = MapUtil.getSortedMapByValueDesc(target.allEconomicUtil);
		
		
	}
	
	public boolean isObligatedInDefCall(Society caller, Society ally, Society target) {
		SocietalRelationMode defRelType = getInterSocietalRel(ally, caller).societyRelMode;
		return defRelType == SocietalRelationMode.DEFENSIVE_ALLIES ||
				defRelType == SocietalRelationMode.ALLIES ||
				defRelType == SocietalRelationMode.OVERLORD ||
				defRelType == SocietalRelationMode.VASSAL;
	}
	
	public boolean isObligatedInAtkCall(Society caller, Society ally, Society target) {
		SocietalRelationMode atkRelType = getInterSocietalRel(ally, caller).societyRelMode;
		return atkRelType == SocietalRelationMode.ALLIES ||
				atkRelType == SocietalRelationMode.VASSAL;
	}
	
	public Collection<Society> getAllSocieties() {
		return societiesByName.values();
	}
	
	public Map<Society, SocietySocietyRel> getInterSocietalRel(Society host) {
		if (relationships.containsKey(host)) {
			return relationships.get(host);
		}
		return null;
	}
	
	public SocietySocietyRel getInterSocietalRel(Society host, Society target) {
		if (relationships.containsKey(host)) {
			Map<Society, SocietySocietyRel> hostRelMap = relationships.get(host);
			if (hostRelMap.containsKey(target)) {
				return hostRelMap.get(target);
			}
		}
		return null;
	}
	
	public static boolean areSocietiesAtWar(Society society, Society otherSociety) {
		for (War war: society.warsInvolved) {
			if (war.hostileInThisWar(society, otherSociety)) {
				return true;
			}
		}
		return false;
	}
	
}
