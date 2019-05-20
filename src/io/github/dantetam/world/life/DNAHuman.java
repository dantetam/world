package io.github.dantetam.world.life;

import java.util.HashMap;
import java.util.Map;

import io.github.dantetam.toolbox.StringUtil;
import io.github.dantetam.world.dataparse.AnatomyData.Body;

public class DNAHuman extends DNALivingEntity {

	private static final double RACE_MUTATE_FREQ = 0.02;
	
	public DNAHuman(String speciesName) {
		super(speciesName);
		initDnaMap();
	}

	@Override
	public void initDnaMap() {
		this.dnaMap = new HashMap<>();
		this.dnaMap.put("race", StringUtil.genAlphaNumericStr(20));
	}

	@Override
	public Body getBodyFromDNA() {
		return null;
	}

	@Override
	public Map<String, String> recombineDNA(DNALivingEntity otherDNA) {
		Map<String, String> newDna = new HashMap<>();
		
		String newRace = "";
		String race = this.dnaMap.get("race");
		String otherRace = otherDNA.dnaMap.get("race");
		for (int index = 0; index < race.length(); index++) {
			if (Math.random() < 0.5) {
				newRace += race.charAt(index);
			}
			else {
				newRace += otherRace.charAt(index);
			}
		}
		if (Math.random() < RACE_MUTATE_FREQ) {
			newRace = StringUtil.mutateAlphaNumStr(newRace);
		}
		
		newDna.put("race", newRace);
		
		return null;
	}
	
}
