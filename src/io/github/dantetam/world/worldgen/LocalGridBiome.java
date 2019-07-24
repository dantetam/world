package io.github.dantetam.world.worldgen;

import java.util.List;
import java.util.Map;

public class LocalGridBiome {

	//TODO
	//Use this architecture;
	
	public BiomeType biomeType;
	
	public List<String> flora;
	public Map<String, Double> stoneBands;
	public double surfaceLevelAmp;
	public double surfaceLevelPerturbAmp;
	
	public Map<Integer, Double> surfaceResourceUbiquity;
	public Map<Integer, Double> surfaceResourceSizes;
	
	public Map<Integer, Double> subSurfaceResourceUbiquity;
	public Map<Integer, Double> subSurfaceResourceSizes;
	public Map<Integer, Integer> subSurfaceResourceHeight;
	
	public LocalGridBiome(BiomeType biomeType) {
		this.biomeType = biomeType;
	}
	
	public enum BiomeType {
		TEST_DESERT, TEST_FOREST
	}
	
}
