package io.github.dantetam.world.civilization;

public class Skill {
	
	public String name;
	public int level, allExp;
	public boolean isCoreSkill;
	
	public Skill(String name, int level, boolean isCoreSkill) {
		this.name = name;
		this.level = level;
		this.allExp = (int) (0.5 * (SkillBook.experienceNeeded[level] + SkillBook.experienceNeeded[level + 1]));
		this.isCoreSkill = isCoreSkill;
	}
	
	public String toString() {
		return "Skill: " + name + " (L: " + level + ", exp: " + allExp + ")";
	}
	
	public Skill clone() {
		Skill clone = new Skill(name, level, isCoreSkill);
		clone.allExp = allExp;
		return clone;
	}
	
}