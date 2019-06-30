package io.github.dantetam.world.life;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.GeometricDistribution;

import io.github.dantetam.toolbox.ListUtil;
import io.github.dantetam.toolbox.MapUtil;
import io.github.dantetam.world.civhumanai.Ethos;
import io.github.dantetam.world.civhumanai.EthosSet;
import io.github.dantetam.world.civilization.SkillBook;
import io.github.dantetam.world.civilization.Society;
import io.github.dantetam.world.dataparse.EthosData;
import io.github.dantetam.world.dataparse.ItemData;
import io.github.dantetam.world.dataparse.ProcessData;
import io.github.dantetam.world.dataparse.SkillData;
import io.github.dantetam.world.items.InventoryItem;
import io.github.dantetam.world.process.LocalProcess;

public class SkillSetInitialize {

	private static final int NUM_START_SKILL_GOOD = 1;
	private static final int NUM_START_SKILL_AVG = 3;
	private static final int NUM_START_SKILL_BASIC = 3;
	
	/**
	 * Fill a human brain with most of its starting opinions and base ethos
	 * Assign random, sensible values to skills
	 * 
	 */
	public static void initHumanSkills(SkillBook skillBook) {
		List<String> allSkills = SkillData.getAllSkills();
		for (int i = 0; i < NUM_START_SKILL_GOOD; i++) {
			String randSkill = allSkills.remove((int) (Math.random() * allSkills.size()));
			int baseSkillVal = (int) (SkillBook.MAX_LEVEL / 3.0);
			GeometricDistribution distr = new GeometricDistribution(0.8);
			int level = baseSkillVal + distr.sample();
			skillBook.setSkillLevel(randSkill, level);
		}
		
		for (int i = 0; i < NUM_START_SKILL_AVG; i++) {
			String randSkill = allSkills.remove((int) (Math.random() * allSkills.size()));
			int baseSkillVal = (int) (SkillBook.MAX_LEVEL / 5.0);
			GeometricDistribution distr = new GeometricDistribution(0.7);
			int level = baseSkillVal + distr.sample();
			skillBook.setSkillLevel(randSkill, level);
		}
		
		for (int i = 0; i < NUM_START_SKILL_BASIC; i++) {
			String randSkill = allSkills.remove((int) (Math.random() * allSkills.size()));
			int baseSkillVal = (int) (SkillBook.MAX_LEVEL / 6.0);
			GeometricDistribution distr = new GeometricDistribution(0.5);
			int level = baseSkillVal + distr.sample();
			skillBook.setSkillLevel(randSkill, level);
		}
	}
	
}
