package io.github.dantetam.world.life;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.dantetam.world.dataparse.AnatomyData.Body;

public abstract class DNALivingEntity {

	public String speciesName;
	protected Map<String, String> dnaMap;
	
	//Not really broad haplogroups in the sense of world populations, but 
	public List<String> haploGroupMutations; 
	
	public DNALivingEntity(String speciesName) {
		this.speciesName = speciesName;
		dnaMap = new HashMap<>();
		haploGroupMutations = new ArrayList<>();
		initDnaMap();
	}
	
	//Ideally, this is instantiated in DNA subclasses, 
	//which define the possible DNA 'genotypes' to maintain per species
	protected abstract void initDnaMap();
	
	//Phenotype simulator, implemented per species or groups of species
	//This is where genes can be converted into actual traits and features
	public abstract Body getBodyFromDNA();
	
	//How two organisms (usually but not always the same species) give new DNA to their offspring
	public abstract Map<String, String> recombineDNA(DNALivingEntity otherDNA);
	
}
