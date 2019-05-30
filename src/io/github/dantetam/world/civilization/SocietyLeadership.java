package io.github.dantetam.world.civilization;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.dantetam.world.civhumanrelation.SocietySocietyRel;
import io.github.dantetam.world.life.Human;

/**
 * 
 */

public class SocietyLeadership {

	TODO; //Implement in Society
	
	public List<Human> currentLeaders;
	
	//Evaluate all factors of society, its people, and its method of determining/selecting rulers,
	//in order to figure out succession (or beginning, when societies emerge).
	public SocietyLeadershipMode sociPrefLeadership; 
	public SocSuccessionType sociSuccessionType;
	
	public SocietyLeadership(Society society) {
		
	}
	
	TODO //SocietalEthos, political, in the technical impl. like that of a human
		
	public enum SocietyLeadershipMode {
		DICTATORSHIP, GROUP, SENATE, FULL_DEMOCRACY 
	}
	
	public enum SocSuccessionType {
		STRENGTH, HEREDITARY, OLIGARCHIC, REP_DEMOCRACY, FULL_DEMOCRACY
	}
	
}
