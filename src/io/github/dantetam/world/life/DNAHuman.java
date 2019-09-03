package io.github.dantetam.world.life;

import java.util.HashMap;
import java.util.Map;

import io.github.dantetam.toolbox.StringUtil;

public class DNAHuman extends DNALivingEntity {

	private static final double RACE_MUTATE_FREQ = 0.02;
	
	public DNAHuman(String speciesName) {
		super(speciesName);
		initDnaMap();
	}

	@Override
	public void initDnaMap() {
		this.dnaMap = new HashMap<>();
		//this.dnaMap.put("race", StringUtil.genAlphaNumericStr(20));
		String sex = randomSex();
		String gender = randomGenderFromSex(sex);
		String orient = randomSexualOri(gender);
		
		this.dnaMap.put("sex", sex);
		this.dnaMap.put("gender", gender);
		this.dnaMap.put("sexualOri", orient);
	}
	
	@Override
	public Body getBodyFromDNA() {
		return null;
	}

	public boolean canReproduce(DNALivingEntity otherDNA) {
		return this.speciesName.equals(otherDNA.speciesName);
	}
	
	@Override
	public DNAHuman recombineDNA(DNALivingEntity otherDNA) {
		if (!canReproduce(otherDNA)) {
			throw new IllegalArgumentException("Cannot naturally reproduce together dna of two different species");
		}
		
		DNAHuman newDna = new DNAHuman(this.speciesName);
		Map<String, String> dnaMap = new HashMap<>();
		
		String[] keysFairlyMerged = {"race", "culture"};
		for (String key: keysFairlyMerged) {
			String race = this.dnaMap.get(key);
			String otherRace = otherDNA.dnaMap.get(key);
			String newRace = StringUtil.randMergeStrs(race, otherRace);
			if (Math.random() < RACE_MUTATE_FREQ) {
				newRace = StringUtil.mutateAlphaNumStr(newRace);
			}
			dnaMap.put(key, newRace);
		}
		
		newDna.dnaMap = dnaMap;
		
		return newDna;
	}
	
	public String randomSex() {
		/*
		if (Math.random() < 0.005) {
			return "nonbinary";
		}
		*/
		return Math.random() < 0.5 ? "XX" : "XY";
	}
	
	public String randomGenderFromSex(String sexChromo) {
		double rand = Math.random();
		if (rand < 0.020) {
			return "nonbinary";
		}
		if (rand < 0.026) {
			return sexChromo == "XX" ? "male" : "female";
		}
		return sexChromo == "XX" ? "female" : "male";
	}
	
	public String randomSexualOri(String gender) {
		double rand = Math.random();
		if (rand < 0.05) {
			return "all";
		}
		if (rand < 0.13) {
			return gender;
		}
		return gender == "male" ? "female" : "male";
	}
	
}
