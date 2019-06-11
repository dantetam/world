package io.github.dantetam.world.civilization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.dantetam.world.civhumanai.EthosSet;
import io.github.dantetam.world.civhumanrelation.SocietySocietyRel;
import io.github.dantetam.world.life.Human;

/**
 * 
 */

public class SocietyLeadership {
	
	private Society society;
	public List<Human> currentLeaders;
	
	public EthosSet societalEthosSet;
	
	//Evaluate all factors of society, its people, and its method of determining/selecting rulers,
	//in order to figure out succession (or beginning, when societies emerge).
	public SocietyLeadershipMode sociPrefLeadership; 
	public SocSuccessionType sociSuccessionType;
	public SocKnowledgeEthosChange sociKnowledgeType;
	
	public SocietyLeadership(Society society) {
		this.society = society;
		this.currentLeaders = new ArrayList<>();
		societalEthosSet = new EthosSet();
	}
	
	public void succeed() {
		List<Human> newLeaders = SocietyLeadershipSuccession.determineSuccessors(society, 
				this.sociPrefLeadership, this.sociSuccessionType);
		this.currentLeaders = newLeaders;
	}
	
	/**
	 * Pick new leaders, affirm the society's knowledge base and ethos, 
	 * assign societal task utility, and so on.
	 */
	public void workThroughSocietalPolitics() {
		if (currentLeaders == null || currentLeaders.size() == 0) {
			succeed();
		}
	}
	
	//TODO: SocietalEthos, political, in the technical impl. like that of a human
		
	//TODO: Note that various groups of people can seek to change the mode of societal leadership and succession,
	//e.g. a group of strong warlords believes there is high utility in
	
	public enum SocietyLeadershipMode {
		DICTATORSHIP,  //An absolute unchecked dictator rules
		GROUP, 		   //A council rules, mirroring classical Roman consuls, or elder councils
		MONARCH,	   //Either a hereditary absolute/constitutional monarch rules, with advisors
		PRESIDENT,     //A democratic president rules, with advisors
		SENATE,        //A democratically elected group rules
		FULL_DEMOCRACY //The people rule
	}
	
	public enum SocSuccessionType {
		STRENGTH, //Might is right, in a barbaric sort of way 
		PRESTIGE, //Society elevates those with the highest util. (i.e. wealth, army, heroic deeds, etc.)
		HEREDITARY, //Ruling families and dynasties are favored, also with pretenders and new families
		OLIGARCHIC, //Ruling and wealthy classes are favored, and fight with each other
		LANDED_DEMOCRACY, //Much like early US democracy, an oligarchic state where landed classes vote
		REP_DEMOCRACY, //Citizens get votes for representatives, who ultimately make decisions
		FULL_DEMOCRACY  //Decentralized democracy; for the most local, undeveloped, or anarchistic societies
	}
	
	//Representing the way that this society perpetuates new knowledge, especially ethos
	//See the different modes below for the various methods
	public enum SocKnowledgeEthosChange {
		//This society slowly adds on knowledge, with each ruler contributing gradually,
		//in a way that favors elders, ancestors, and older knowledge
		TRADITION,
		
		//The current rulers have absolute authority over the current knowledge
		//Only for the most primitive and most advanced fascist societies
		ABSOLUTE,
		
		//The current rulers and the ruling classes in general, have absolute authority
		OLIGARCHIC_ABSOLUTE,
		
		//Current rulers may debate ideas with the past, past rulers, and current rulers.
		//This does not bias towards older ideas.
		OLIGARCHIC_DEBATE,
		
		//Still debate, but now citizens have input which can go into the knowledge base
		//Bias new ideas, because anyone can start debate and bring new ideas in.
		//Mostly for rational democracies
		DEMOCRATIC_DEBATE
	}
	
}
