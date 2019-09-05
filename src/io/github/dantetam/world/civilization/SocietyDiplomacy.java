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

import io.github.dantetam.toolbox.CollectionUtil;
import io.github.dantetam.toolbox.MapUtil;
import io.github.dantetam.world.civhumanrelation.SocietySocietyRel;
import io.github.dantetam.world.civhumanrelation.SocietySocietyRel.SocietalRelationMode;
import io.github.dantetam.world.civhumansocietyai.FreeActionsSociety;
import io.github.dantetam.world.combat.War;
import io.github.dantetam.world.grid.WorldGrid;
import io.github.dantetam.world.process.LocalSocietyJob;

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
		Map<Integer, Integer> resourceExchangeResult = new HashMap<>();
		
		Map<Integer, Double> hostUtilMap = MapUtil.getSortedMapByValueDesc(host.calcUtility);
		Map<Integer, Double> tarUtilMap = MapUtil.getSortedMapByValueDesc(target.calcUtility);
		
		Map<Integer, Double> hostAccessMap = MapUtil.getSortedMapByValueDesc(host.accessibleResUtil);
		Map<Integer, Double> tarAccessMap = MapUtil.getSortedMapByValueDesc(target.accessibleResUtil);
		
		Collection<Integer> relevantResIds = CollectionUtil.colnsUnionUnique(
				hostAccessMap.keySet(), tarAccessMap.keySet());
		
		double tradeScore = 0;
		List<Integer> potentialImportHost = new ArrayList<>(), potentialImportTar = new ArrayList<>();
		int tradableHostUtil = 0, tradableTarUtil = 0; 
		
		//Look at all resources that both societies possess, and determine which side desires a resource more
		//The host, who initiates the trade, has a slight advantage in their choice of goods,
		//while the target has advantage in pricing.
		//This is not founded in any real world research regarding trade, barter, or society.
		for (Integer resId: relevantResIds) {
			double hostUtil = hostUtilMap.get(resId), tarUtil = tarUtilMap.get(resId);
			double hostAccess = hostUtilMap.get(resId), tarAccess = tarUtilMap.get(resId);
			
			double finalHost = hostUtil / hostAccess;
			double finalTar = tarUtil / tarAccess;
			
			double unitPrice = (hostUtil + tarUtil) / 2;
			
			if (hostUtil > tarUtil * 1.2 || finalHost > finalTar) {
				potentialImportHost.add(resId);
				tradableTarUtil += tarAccess * unitPrice;
			}
			else if (tarUtil > hostUtil * 1.2 || finalTar > finalHost) {
				potentialImportTar.add(resId);
				tradableHostUtil += hostAccess * unitPrice;
			}
		}
		
		//Balance out the trade utility by pulling items out that can be traded
		//The map resourceExchangeResult contains positive entries for items that the host must import
		while (potentialImportHost.size() > 0 && potentialImportTar.size() > 0) {
			boolean hostMode = tradeScore <= 0;
			int resId;
			if (hostMode) {
				resId = potentialImportHost.remove(0);
			}
			else {
				resId = potentialImportTar.remove(0);
			}
			
			double hostUtil = hostUtilMap.get(resId), tarUtil = tarUtilMap.get(resId);
			double hostAccess = hostUtilMap.get(resId), tarAccess = tarUtilMap.get(resId);
			double unitPrice = (hostUtil + tarUtil) / 2;
			
			if (hostMode) {
				double scoreToAdd = Math.min(tradableHostUtil, tarAccess * unitPrice);
				tradeScore += scoreToAdd;
				int numUnits = (int) (scoreToAdd / unitPrice);
				MapUtil.addNumMap(resourceExchangeResult, resId, numUnits);
			}
			else {
				double scoreToAdd = Math.min(tradableTarUtil, hostAccess * unitPrice);
				tradeScore -= scoreToAdd;
				int numUnits = (int) (scoreToAdd / unitPrice);
				MapUtil.addNumMap(resourceExchangeResult, resId, -numUnits);
			}
		}
		
		CaravanTradeProcess tradeProcess = new CaravanTradeProcess();
		//Create a trade job that allows people to take the priority
		//to ship goods between societies
		LocalSocietyJob societyJob = new LocalSocietyJob();
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
