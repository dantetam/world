package io.github.dantetam.world.combat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.dantetam.toolbox.Pair;
import io.github.dantetam.world.civilization.LocalExperience;
import io.github.dantetam.world.civilization.Society;

public class War {

	public Society warLeaderAttacker, warLeaderDefender;
	public Set<Society> attackerAllies;
	public Set<Society> defenderAllies;
	
	public List<Battle> warBattleHistory;
	public List<LocalExperience> warMemories;
	
	public double warscoreAttacker;
	
	public Map<Society, Double> startingSocietalWealth;
	public Map<Society, Integer> startingSocietalPeople;
	public double startingAtkStrength, startingDefStrength;
	
	public War(Society warLeaderAttacker, Society warLeaderDefender, 
			Set<Society> attackerAllies, Set<Society> defenderAllies) {
		this.warLeaderAttacker = warLeaderAttacker;
		this.warLeaderDefender = warLeaderDefender;
		this.attackerAllies = attackerAllies;
		this.defenderAllies = defenderAllies;
		
		this.startingSocietalWealth = new HashMap<>();
		this.startingSocietalPeople = new HashMap<>();
		
		Set<Society> allSocieties = this.getAllBelligerentsBothSides();
		for (Society allSociety: allSocieties) {
			double wealth = allSociety.getTotalWealth();
			int peopleScore = allSociety.getAllPeople().size() * 50;
			startingSocietalWealth.put(allSociety, wealth);
			startingSocietalPeople.put(allSociety, peopleScore);
			if (attackerAllies.contains(allSociety) || warLeaderAttacker.equals(allSociety)) {
				startingAtkStrength += wealth + peopleScore;
			}
			else {
				startingDefStrength += wealth + peopleScore;
			}
		}
		
		warBattleHistory = new ArrayList<>();
		warMemories = new ArrayList<>();
		warscoreAttacker = 0;
	}
	
	public boolean isInvolvedInWar(Society society) {
		if (warLeaderAttacker.equals(society) || attackerAllies.contains(society) ||
				warLeaderDefender.equals(society) || defenderAllies.contains(society)) {
			return true;
		}
		return false;
	}
	
	public Set<Society> getSameSide(Society society) {
		if (warLeaderAttacker.equals(society) || attackerAllies.contains(society)) {
			Set<Society> opposite = new HashSet<>(attackerAllies);
			opposite.add(warLeaderAttacker);
			return opposite;
		}
		else if (warLeaderDefender.equals(society) || defenderAllies.contains(society)) {
			Set<Society> opposite = new HashSet<>(defenderAllies);
			opposite.add(warLeaderDefender);
			return opposite;
		}
		throw new IllegalArgumentException("Invalid war calculation, " + society + " not involved in war: " + this.toString());
	}
	
	public Set<Society> getOppositeSide(Society society) {
		if (warLeaderAttacker.equals(society) || attackerAllies.contains(society)) {
			Set<Society> opposite = new HashSet<>(defenderAllies);
			opposite.add(warLeaderDefender);
			return opposite;
		}
		else if (warLeaderDefender.equals(society) || defenderAllies.contains(society)) {
			Set<Society> opposite = new HashSet<>(attackerAllies);
			opposite.add(warLeaderAttacker);
			return opposite;
		}
		throw new IllegalArgumentException("Invalid war calculation, " + society + " not involved in war: " + this.toString());
	}
	
	public boolean hostileInThisWar(Society society, Society otherSociety) {
		return getOppositeSide(society).contains(otherSociety);
	}
	
	public Pair<Set<Society>> getBelligerents() {
		return new Pair<Set<Society>>(
				new HashSet<Society>() {{addAll(attackerAllies); add(warLeaderAttacker);}},
				new HashSet<Society>() {{addAll(defenderAllies); add(warLeaderDefender);}}
				);
	}
	
	public Set<Society> getAllBelligerentsBothSides() {
		return new HashSet<Society>() {{
					addAll(attackerAllies); add(warLeaderAttacker);
					addAll(defenderAllies); add(warLeaderDefender);
					}};
	}
	
}
