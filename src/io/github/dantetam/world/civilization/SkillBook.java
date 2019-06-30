package io.github.dantetam.world.civilization;

import java.util.HashMap;
import java.util.Map;

public class SkillBook {
	
	public static final int MAX_LEVEL = 30;
	private static int[] experienceNeeded = new int[MAX_LEVEL+2]; 
	public static String[] allSkills = {"build", "cook", "cloth", "craft", "farm", 
			"fight", "process", "smelt", "stone", "woodcut"};
	
	private Map<String, Skill> skillMapping;
	
	//Use skill in implementing process efficiency/competency/quality,
	//i.e. ability to do so, quality, time, wasted resources, etc.
	
	public static void init() {
		double startingExperience = 100;
		double levelingFactor = 1.2;
		experienceNeeded[0] = (int) startingExperience;
		for (int i = 1; i < MAX_LEVEL + 1; i++) {
			experienceNeeded[i] = (int) (experienceNeeded[i-1] * levelingFactor);
		}
		experienceNeeded[MAX_LEVEL + 1] = Integer.MAX_VALUE / 2;
	}
	
	public SkillBook() {
		skillMapping = new HashMap<>();
		for (String skillName: allSkills) {
			Skill skill = new Skill(skillName, (int) (Math.random() * 8));
			skillMapping.put(skillName, skill);
		}
	}
	
	public int getSkillLevel(String skillName) {
		if (!skillMapping.containsKey(skillName)) {
			throw new IllegalArgumentException("Could not find skill name: " + skillName);
		}
		return skillMapping.get(skillName).level;
	}
	
	public void addExperienceToSkill(String skillName, int experience) {
		Skill skill = skillMapping.get(skillName);
		skill.allExp += experience;
		if (experience > 0) {
			if (skill.allExp >= experienceNeeded[skill.level + 1]) {
				skill.level++;
			}
		}
	}
	
	public void setSkillLevel(String skillName, int level) {
		Skill skill = skillMapping.get(skillName);
		skill.level = level;
		skill.allExp = experienceNeeded[level + 1];
	}
	
	private static class Skill {
		public String name;
		public int level, allExp;
		
		public Skill(String name, int level) {
			this.name = name;
			this.level = level;
			this.allExp = (int) (0.5 * (experienceNeeded[level] + experienceNeeded[level + 1]));
		}
		
		public String toString() {
			return "Skill: " + name + " (L: " + level + ", exp: " + allExp + ")";
		}
	}
	
}
