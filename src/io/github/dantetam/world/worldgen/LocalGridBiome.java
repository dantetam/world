package io.github.dantetam.world.worldgen;

import java.util.List;
import java.util.Map;

import io.github.dantetam.localdata.ConstantData;

public class LocalGridBiome {

	//TODO
	//Use this architecture;
	
	//These five factors are assigned to this local grid biome as rough high level parameters
	public BiomeType biomeType;
	public BiomeLocalizedClimate biomeClimate;
	public List<BiomeLocalizedFeatures> features;
	public double elevation;
	public BiomeTerrain terrain;
	
	//The rest of the parameters below determine the actual substances and life that make up the land
	public double[][] soilLevels;
	public int[][] soilCompositions;
	
	public List<String> flora;
	public Map<String, Double> stoneBands;
	public double surfaceLevelAmp;
	public double surfaceLevelPerturbAmp;
	
	public Map<Integer, Double> surfaceResourceUbiquity;
	public Map<Integer, Double> surfaceResourceSizes;
	
	public Map<Integer, Double> subSurfaceResourceUbiquity;
	public Map<Integer, Double> subSurfaceResourceSizes;
	public Map<Integer, Integer> subSurfaceResourceHeight;
	
	public LocalGridBiome(BiomeType biomeType, BiomeLocalizedClimate biomeClimate,
			List<BiomeLocalizedFeatures> features, double elevation, BiomeTerrain terrain) {
		this.biomeType = biomeType;
		this.biomeClimate = biomeClimate;
		this.features = features;
		this.elevation = elevation;
		this.terrain = terrain;
	}
	
	//Use the data within this LocalGridBiome object to fill these data fields
	private void initBiomeSettings() {
		
	}
	
	public enum BiomeType { //Main indicator of biome profile/high level summary
		TUNDRA, TAIGA, WINTER_FOREST,
		CONTINENTAL_FOREST, COOL_TEMP_FOREST, TEMPERATE_FOREST, SUBTROPICAL_FOREST,
		TROPICA_RAINFOREST, DESERT
	}
	
	public enum BiomeTerrain {
		PLAINS, HILLS, MOUNTAINS, EXTREME_MOUNTAINS, MESA_PLATEAU
	}
	
	//The climate specific to one local grid region
	public static class BiomeLocalizedClimate {
		public BroadClimateGroup group;
		public Precipitation rain;
		public TemperatureModifier temperature;
		
		public BiomeLocalizedClimate(BroadClimateGroup group, Precipitation rain, TemperatureModifier temperature) {
			this.group = group;
			this.rain = rain;
			this.temperature = temperature;
		}
	}
	
	//Affects temperature, heat/cold resistant plants, biodiversity and existence of special plant types
	public enum BroadClimateGroup { 
		TROPICAL, ARID_HOT, TEMPERATE, CONTINENTAL, COLD
	}
	
	//Rain and temperature affect amount of life, soil, and water-tolerant/water-needing plants, biodiversity
	public enum Precipitation {
		DRY, SEMI_DRY, DRY_SUMMER, DRY_WINTER, HUMID, MONSOON
	}
	public enum TemperatureModifier {
		HOT, COLD, HOT_SUMMER, WARM_SUMMER, COLD_SUMMER
	}
	
	//Relating to special modifications or features on this area, such as high mountains, 
	//or exceedingly dry conditions
	//Intended also for special distinctive features like a magical glade meadow.
	public enum BiomeLocalizedFeatures {
		MAGICAL_GLADE,
		DEMONIC_CORRUPTION
	}
	
	/**
	 * Store all the collection of biome calculations and analysis of climate groups, precipitation, temperature,
	 * and other special modifiers to determine the general feel of a local grid biome.
	 */
	
	public static LocalGridBiome defaultBiomeTest() {
		return LocalGridBiome.determineAllBiomeBroad(0, 0, 0, 0, 0, 0, 0);
	}
	
	public static LocalGridBiome determineAllBiomeBroad(double elevation, double temperature, 
			double yearRoundVarTemp, double rain, double yearRoundVarRain, 
			double terrainType, double terrainPerturb) {
		Precipitation precip;
		TemperatureModifier tempMod;
		
		if (rain < 1.5) precip = Precipitation.DRY;
		else if (rain < 3.5) precip = Precipitation.SEMI_DRY;
		else if (rain > 7) precip = yearRoundVarRain < 7 ? Precipitation.HUMID : Precipitation.MONSOON;
		else precip = yearRoundVarTemp < 8 ? Precipitation.DRY_SUMMER : Precipitation.DRY_WINTER;
		
		if (temperature < 2.5) tempMod = TemperatureModifier.COLD;
		else if (temperature > 7.5) tempMod = TemperatureModifier.HOT;
		else {
			if (yearRoundVarTemp > 6) tempMod = TemperatureModifier.HOT_SUMMER;
			else if (yearRoundVarTemp < 4) tempMod = TemperatureModifier.COLD_SUMMER;
			else tempMod = TemperatureModifier.WARM_SUMMER;
		}
		
		BroadClimateGroup climateGroup;
		if (rain < 2) {
			if (temperature < 3.5) {
				climateGroup = BroadClimateGroup.COLD;
			}
			else {
				climateGroup = BroadClimateGroup.ARID_HOT;
			}
		}
		else {
			if (temperature > 7.5) {
				climateGroup = BroadClimateGroup.TROPICAL;
			}
			else if (temperature > 4.5) {
				climateGroup = BroadClimateGroup.TEMPERATE;
			}
			else {
				climateGroup = BroadClimateGroup.CONTINENTAL;
			}
		}
		
		BiomeTerrain terrainEnum;
		if (terrainType < 4) {
			terrainEnum = BiomeTerrain.PLAINS;
		}
		else if (terrainType > 9) {
			terrainEnum = BiomeTerrain.EXTREME_MOUNTAINS;
		}
		else if (terrainType > 7.5) {
			terrainEnum = BiomeTerrain.MOUNTAINS;
		}
		else {
			if (terrainPerturb > 5.5) {
				terrainEnum = BiomeTerrain.HILLS;
			}
			else {
				terrainEnum = BiomeTerrain.MESA_PLATEAU;
			}
		}
		
		BiomeLocalizedClimate climate = new BiomeLocalizedClimate(climateGroup, precip, tempMod);
		LocalGridBiome biome = new LocalGridBiome(
				BiomeType.CONTINENTAL_FOREST, climate, null, elevation, terrainEnum); 
		
		
		//TODO
		//Use special clusters parsed from file, WorldCSVParser::parseMap? 
		biome.surfaceResourceSizes = ConstantData.clusterSizesMap;
		biome.surfaceResourceUbiquity = ConstantData.clusterUbiquityMap;
		
		return biome;
	}
	
}
