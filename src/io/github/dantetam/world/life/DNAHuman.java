package io.github.dantetam.world.life;

import java.util.Map;

import io.github.dantetam.world.dataparse.AnatomyData.Body;

public class DNAHuman extends DNALivingEntity {

	public DNAHuman(String speciesName) {
		super(speciesName);
	}

	@Override
	public void initDnaMap() {
		
	}

	@Override
	public Body getBodyFromDNA() {
		return null;
	}

	@Override
	public Map<String, String> recombineDNA(DNALivingEntity otherDNA) {
		return null;
	}

}
