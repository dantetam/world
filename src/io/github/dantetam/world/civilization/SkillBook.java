package io.github.dantetam.world.civilization;

import java.util.HashMap;
import java.util.Map;

public class SkillBook {
	
	private static final int MAX_LEVEL = 30;
	private static int[] experienceNeeded = new int[MAX_LEVEL+2]; 
	private static String[] allSkills = {"build", "cook", "cloth", "craft", "farm", 
			"fight", "process", "smelt", "stone", "woodcut"};
	
	private Map<String, Skill> skillMapping;
	
	public static void init() {
		double startingExperience = 100;
		double levelingFactor = 1.2;
		experienceNeeded[0] = (int) startingExperience;
		for (int i = 1; i < MAX_LEVEL + 1; i++) {
			experienceNeeded[i] = (int) (experienceNeeded[i-1] * levelingFactor);
		}
		experienceNeeded[MAX_LEVEL + 1] = Integer.MAX_VALUE / 2;
	}
	
	//TODO: Use
	
	public void addExperienceToSkill(String skillName, int experience) {
		Skill skill = skillMapping.get(skillName);
		skill.allExp += experience;
		if (experience > 0) {
			if (skill.allExp >= experienceNeeded[skill.level + 1]) {
				skill.level++;
			}
		}
	}
	
	public SkillBook() {
		skillMapping = new HashMap<>();
	}
	
	private class Skill {
		public String name;
		public int level, allExp;
		
		public Skill(String name, int level) {
			this.name = name;
			this.level = level;
			this.allExp = (int) (0.5 * (experienceNeeded[level] + experienceNeeded[level + 1]));
		}
	}
	
}
