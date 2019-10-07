package io.github.dantetam.world.civilization;

import java.util.HashMap;
import java.util.Map;

import io.github.dantetam.world.dataparse.SkillData;

public class SkillBook {
	
	public static final int MIN_LEVEL = 0;
	public static final int MAX_LEVEL = 30;
	static int[] experienceNeeded = new int[MAX_LEVEL+2]; 
	
	private Map<String, Skill> skillMapping;
	public static Map<String, String> skillAbbrev = new HashMap<>(); //Map shorter name -> full name
	
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
		for (Skill skill: SkillData.getAllSkills().values()) {
			skill = skill.clone();
			skillMapping.put(skill.name, skill);
		}
	}
	
	public Skill getSkill(String skillKey) {
		if (!skillMapping.containsKey(skillKey)) {
			if (skillAbbrev.containsKey(skillKey)) {
				skillKey = skillAbbrev.get(skillKey);
			}
		}
		if (!skillMapping.containsKey(skillKey)) {
			System.err.println(skillMapping);
			throw new IllegalArgumentException("Could not find skill name: " + skillKey);
		}
		return skillMapping.get(skillKey);
	}
	
	public int getSkillLevel(String skillKey) {
		return this.getSkill(skillKey).level;
	}
	
	public void addExperienceToSkill(String skillName, int experience) {
		Skill skill = this.getSkill(skillName);
		skill.allExp += experience;
		if (experience > 0) {
			if (skill.allExp >= experienceNeeded[skill.level + 1]) {
				skill.level++;
			}
		}
	}
	
	public void setSkillLevel(String skillName, int level) {
		Skill skill = this.getSkill(skillName);
		skill.level = level;
		skill.allExp = experienceNeeded[level + 1];
	}
	
}
