package io.github.dantetam.world.combat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.dantetam.world.civilization.LocalExperience;
import io.github.dantetam.world.civilization.Society;

public class War {

	public Society warLeaderAttacker, warLeaderDefender;
	public Set<Society> attackerAllies;
	public Set<Society> defenderAllies;
	
	public List<LocalExperience> battleHistory;
	
	public double warscoreAttacker;
	
	public War(Society warLeaderAttacker, Society warLeaderDefender, 
			Set<Society> attackerAllies, Set<Society> defenderAllies) {
		this.warLeaderAttacker = warLeaderAttacker;
		this.warLeaderDefender = warLeaderDefender;
		this.attackerAllies = attackerAllies;
		this.defenderAllies = defenderAllies;
		
		battleHistory = new ArrayList<>();
		warscoreAttacker = 0;
	}
	
	public boolean isInvolvedInWar(Society society) {
		if (warLeaderAttacker.equals(society) || attackerAllies.contains(society) ||
				warLeaderDefender.equals(society) || defenderAllies.contains(society)) {
			return true;
		}
		return false;
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
	
}
