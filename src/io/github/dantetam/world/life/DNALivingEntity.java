package io.github.dantetam.world.life;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.dantetam.toolbox.StringUtil;
import io.github.dantetam.world.dataparse.AnatomyData.Body;

public abstract class DNALivingEntity {

	public String speciesName;
	protected Map<String, String> dnaMap; //Representation of genes ordered by name
	
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
	
	public double compareGenesDist(DNALivingEntity otherDna, String geneName) {
		if (!this.dnaMap.containsKey(geneName) || !this.dnaMap.containsKey(geneName)) {
			throw new IllegalArgumentException("This DNA and/or other DNA do not contain the gene named: " + geneName);
		}
		String gene = dnaMap.get(geneName), otherGene = otherDna.dnaMap.get(geneName);
		return dnaStringDist(gene, otherGene);
	}
	
	protected double dnaStringDist(String stringA, String stringB) {
		double avgDist = 0;
		int minLen = Math.min(stringA.length(), stringB.length());
		for (int i = 0; i < minLen; i++) {
			char charA = stringA.charAt(i), charB = stringB.charAt(i);
			int indexA = StringUtil.getIndexOfChar(charA), indexB = StringUtil.getIndexOfChar(charB);
			if (indexA < indexB) {
				int temp = indexA;
				indexA = indexB;
				indexB = temp;
			}
			int wrapIndexB = indexB - StringUtil.alphanum.length();
			int charDist = Math.min(indexB - indexA, indexA - wrapIndexB);
			avgDist += (double) charDist / minLen;
		}
		return avgDist;
	}
	
	public void overrideDnaMapping(String key, String value) {
		dnaMap.put(key, value);
	}
	
}
