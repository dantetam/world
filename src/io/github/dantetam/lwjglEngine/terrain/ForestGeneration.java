package io.github.dantetam.lwjglEngine.terrain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import java.util.Set;

import kn.uni.voronoitreemap.gui.JSite;
import kn.uni.voronoitreemap.gui.VoronoiLibrary;
import kn.uni.voronoitreemap.j2d.Point2D;
import kn.uni.voronoitreemap.j2d.PolygonSimple;
import io.github.dantetam.lwjglEngine.terrain.NeighborsGraphStructure.Edge;
import io.github.dantetam.lwjglEngine.toolbox.CustomMathUtil;

/**
 * TODO: General ideas:
 * 
 * use a Poisson-Voronoi tessellation, with many subdivisions (dense);
 * create a regular 2d perlin noise/projected 3d noise for climate, elevation, moisture;
 * use a random seeding process;
 * 		create individual spawning seeds (trees are different per biome, probability distributions);
 * 		trees age and spread seeds slowly;
 * 		trees die off in a vertically flipped, horizontally shifted logistic curve;
 * 		trees can burn down or be destroyed in methods similar to seeding and spreading;
 * 		same for other flora.
 * 
 * @author Dante
 *
 */

public class ForestGeneration {

	/*
	public static void main(String[] args) {
		Point2D topLeftBound = new Point2D(0, 0);
		Point2D bottomRightBound = new Point2D(1600, 1600);
		double averageDistance = 50;
		
		double cutoff = 50;
		
		double seed = 500;
				
		BaseTerrain map = new PerlinNoise(seed);
		double[][] terrain = map.generate(new double[] { 32, 32, 150, 8, 1, 0.8, 6, 64 });
		
		double[][] temperature = new PerlinNoise(seed).generate(new double[] { 32, 32, 3, 16, 3, 1, 3, 64 });
		
		Random rainRandom = new Random((long) seed);
		double[][] rain = new double[temperature.length][temperature[0].length];
		for (int i = 0; i < temperature.length; i++) {
			for (int j = 0; j < temperature[0].length; j++) {
				rain[i][j] = rainRandom.nextDouble() * temperature[i][j] + rainRandom.nextDouble();
			}
		}
		
		int[][] biomes = new int[32][32];
		for (int r = 0; r < biomes.length; r++) {
			for (int c = 0; c < biomes[0].length; c++) {
				if (terrain[r][c] >= cutoff) {
					biomes[r][c] = GameTerrainGeneration.returnBiome(temperature[r][c], rain[r][c]);
				} else
					biomes[r][c] = -1;
			}
		}
		
		
		Object[] forestData = generateForest(topLeftBound, bottomRightBound, averageDistance,
				terrain, biomes, temperature, rain);
		List<JSite> voronoi = (List<JSite>) forestData[0];
		Map<Integer, ProceduralTree> polygonForestData = (Map) forestData[1];
		Map<Integer, BiomeData> polygonBiomeData = (Map) forestData[2];
	}
	*/
	
	public static Object[] generateForest(Point2D topLeftBound, Point2D bottomRightBound, double averageDistance,
			double[][] terrain, int[][] biomes, double[][] temperature, double[][] rain) {
		int lloydRelaxationTimes = 1;
		List<JSite> voronoi = VoronoiLibrary.voronoiLib(topLeftBound, bottomRightBound, averageDistance,
				lloydRelaxationTimes);
		
		double[][] biomesD = toDoubleTable(biomes);
		
		Map<Integer, BiomeData> polygonBiomeData = new HashMap<>();
		Map<Integer, ProceduralTree> polygonForestData = new HashMap<>();
		
		for (int i = 0; i < voronoi.size(); i++) {
			PolygonSimple polygon = voronoi.get(i).getSite().getPolygon();
			Point2D centroid = polygon.getCentroid();
			double terrainValue = linearInterp2D(terrain, centroid, topLeftBound, bottomRightBound);
			double biomeValue = linearInterp2D(biomesD, centroid, topLeftBound, bottomRightBound);
			double temperatureValue = linearInterp2D(temperature, centroid, topLeftBound, bottomRightBound);
			double rainValue = linearInterp2D(rain, centroid, topLeftBound, bottomRightBound);
			BiomeData biomeData = new BiomeData(terrainValue, biomeValue, temperatureValue, rainValue);
			polygonBiomeData.put(i, biomeData);
		}
		
		Object[] results = NeighborsGraphStructure.computePolygonalNeighbors(voronoi);
		Map<Integer, Set<Integer>> polygonNeighborMap = (Map) results[0];
		//Map<Edge, List<Integer>> sharedEdgesMap = (Map) results[1]; 
		
		int initialRandomSeeds = (int) Math.round(Math.sqrt(voronoi.size()));
		while (initialRandomSeeds > 0) {
			int randomPolyIndex = (int) (Math.random() * voronoi.size());
			PolygonSimple polygon = voronoi.get(randomPolyIndex).getSite().getPolygon();
			BiomeData polygonData = polygonBiomeData.get(randomPolyIndex);
			if (!polygonForestData.containsKey(randomPolyIndex) && polygonData.biome != -1) {
				Point2D centroid = polygon.getCentroid();
				ProceduralTree tree = new ProceduralTree(centroid, polygonData.biome, 0, 0.125 * Math.sqrt(polygon.getArea()));
				polygonForestData.put(randomPolyIndex, tree);
				initialRandomSeeds--;
			}
		}
		
		int timeIterations = (int) Math.round(Math.sqrt(voronoi.size()));
		for (int i = 0; i < timeIterations; i++) {
			/*
			for (ProceduralTree tree: polygonForestData.values()) {
				tree.recentlyCreated = false;
			}
			*/
			
			//Store new trees by polygon location and biome
			Map<Integer, Double> newTreePolygonIndices = new HashMap<>();
			
			Set<Integer> spontaneousDeath = new HashSet<>();
			
			for (Entry<Integer, ProceduralTree> entry: polygonForestData.entrySet()) {
				int polygonIndex = entry.getKey();
				ProceduralTree tree = entry.getValue();
				PolygonSimple treePoly = voronoi.get(polygonIndex).getPolygon();
				
				//if (tree.recentlyCreated) continue;
				
				tree.age++;
				for (int j = 0; j < 7; j++) {
					if (tree.growChance()) {
						tree.size *= 1.02 + Math.random() * 0.13;
						double upperBoundArea = treePoly.getArea() * 0.9;
						if (tree.size >= upperBoundArea) {
							tree.size = upperBoundArea;
						}
					}
				}
				if (tree.age > 7 && tree.size >= tree.initialSize * 1.5) {
					if (tree.reproduceChance()) {
						Set<Integer> neighbors = polygonNeighborMap.get(polygonIndex);
						List<Integer> validSeedNeighbors = new ArrayList<>();
						for (int candidate: neighbors) {
							BiomeData biomeData = polygonBiomeData.get(candidate);
							if (!polygonForestData.containsKey(candidate) && biomeData.biome != -1) {
								validSeedNeighbors.add(candidate);
							}
						}
						int numSeeds = (int) Math.round(neighbors.size() * Math.random());
						numSeeds = Math.min(numSeeds, validSeedNeighbors.size());
						while (numSeeds > 0) {
							int chosenRandomIndex = (int) (Math.random() * validSeedNeighbors.size());
							int randomPolyIndex = validSeedNeighbors.get(chosenRandomIndex);
							newTreePolygonIndices.put(randomPolyIndex, tree.biome); //The new tree is the parent's biome
							numSeeds--;
						}
					}
				}
				if (tree.spantaneousDeathChance()) {
					spontaneousDeath.add(polygonIndex);
				}
			}
			
			int burnPolyIndex = (int) (Math.random() * voronoi.size());
			Set<Integer> burnOrRemove = randomDfsPolygon(polygonNeighborMap, burnPolyIndex);
			
			for (Integer damageTreePoly: burnOrRemove) {
				ProceduralTree tree = polygonForestData.get(damageTreePoly);
				if (tree != null) {
					double damageChance = tree.rawBurnChance();
					double rand = Math.random();
					if (rand < 0.25 * damageChance) {
						polygonForestData.remove(damageTreePoly);
					}
					else if (rand < damageChance) {
						tree.damage();
						if (tree.size < tree.initialSize / 2) {
							polygonForestData.remove(damageTreePoly);
						}
					}
				}
			}
			for (Integer removePoly: spontaneousDeath) {
				polygonForestData.remove(removePoly);
			}
			
			for (Entry<Integer, Double> entry: newTreePolygonIndices.entrySet()) {
				int randomPolyIndex = entry.getKey();
		
				PolygonSimple polygon = voronoi.get(randomPolyIndex).getSite().getPolygon();
				Point2D centroid = polygon.getCentroid();
				BiomeData polygonData = polygonBiomeData.get(randomPolyIndex);
		
				double newTreeBiome = entry.getValue();
				double mutateRand = Math.random();
				if (mutateRand < 0.05) {
					newTreeBiome += (Math.random() < 0.5 ? -1 : 1);
					newTreeBiome = Math.max(-1, Math.min(newTreeBiome, 6));
				}
				else if (mutateRand < 0.12) {
					newTreeBiome = polygonData.biome;
				}
				
				
				double factorBaby = 0.25 + Math.random()*0.25 - 0.125;
				ProceduralTree babyTree = new ProceduralTree(centroid, newTreeBiome, 0, factorBaby * Math.sqrt(polygon.getArea()));
				polygonForestData.put(randomPolyIndex, babyTree);
			}
		}
		
		return new Object[] {voronoi, polygonForestData, polygonBiomeData};
	}
	
	private static Set<Integer> randomDfsPolygon(Map<Integer, Set<Integer>> polygonEdgesMap, int startPolygonIndex) {
		List<Integer> fringe = new ArrayList<>();
		Set<Integer> visited = new HashSet<>();
		fringe.add(startPolygonIndex);
		
		int roundNumber = 0;
		while (fringe.size() > 0) {
			List<Integer> newFringe = new ArrayList<>();
			for (Integer explore: fringe) {
				if (visited.contains(explore)) continue;
				visited.add(explore);
				Set<Integer> neighbors = polygonEdgesMap.get(explore);
				for (Integer neighbor: neighbors) {
					if (Math.random() < 0.95 - roundNumber / 20) {
						newFringe.add(neighbor);
					}
				}
			}
			fringe = newFringe;
			roundNumber++;
		}
		return visited;
	}
	
	private static double[][] toDoubleTable(int[][] arr) {
		double[][] newArr = new double[arr.length][arr[0].length];
		for (int i = 0; i < arr.length; i++) {
			for (int j = 0; j < arr[0].length; j++) {
				newArr[i][j] = (int) arr[i][j];
			}
		}
		return newArr;
	}
	
	private static double linearInterp2D(double[][] data, Point2D pos, Point2D topLeftBound, Point2D bottomRightBound) {
		double width = bottomRightBound.x - topLeftBound.x;
		double height = bottomRightBound.y - topLeftBound.y;
		double normalizedX = (pos.x - topLeftBound.x) / width;
		double normalizedY = (pos.y - topLeftBound.y) / height;
		int gridX = (int) Math.floor(normalizedX * data[0].length);
		int gridY = (int) Math.floor(normalizedY * data.length);
		return data[gridY][gridX];
	}
	
	public static class BiomeData {
		public double terrainHeight, biome, temperature, rain;
		public BiomeData(double terrainHeight, double biome, double temperature, double rain) {
			this.terrainHeight = terrainHeight;
			this.biome = biome;
			this.temperature = temperature;
			this.rain = rain;
		}
	}
	
	public static class ProceduralTree {
		public Point2D location;
		public double biome;
		public int age;
		public double size, initialSize;
		
		public static final double REPRODUCE_MAX_CHANCE = 0.4, GROW_MAX_CHANCE = 0.4, BURN_MAX_CHANCE = 0.25, DEATH_MAX_CHANCE = 0.05,
				REPRODUCE_STEEPNESS = 0.2, GROW_STEEPNESS = 0.2, BURN_STEEPNESS = 0.05, DEATH_STEEPNESS = 0.02;
		
		//Ensure that newly created trees in turn x do not update a second time in turn x
		//public boolean recentlyCreated = true; 
		
		public ProceduralTree(Point2D location, double biome, int age, double size) {
			this.location = location;
			this.biome = biome;
			this.age = age;
			this.size = size;
			this.initialSize = initialSize;
		}
		
		public void damage() {
			double factor = 0.2 + Math.random()*0.24 - 0.12;
			this.size -= this.initialSize * factor;
		}
		
		//Negative logistic function for x >= 10
		public boolean reproduceChance() {
			double x = this.age;
			double chance = REPRODUCE_MAX_CHANCE - REPRODUCE_MAX_CHANCE / (1 + Math.pow(Math.E, -REPRODUCE_STEEPNESS * (x - 15) ));
			//System.out.println("Reproduce tree chance: " + chance);
			return Math.random() < chance;
		}
		
		//Negative logistic function for x >= 0
		public boolean growChance() {
			double x = this.age;
			double chance = GROW_MAX_CHANCE - GROW_MAX_CHANCE / (1 + Math.pow(Math.E, -GROW_STEEPNESS * (x) ));
			//System.out.println("Grow chance tree: " + chance);
			return Math.random() < chance;
		}
		
		public double rawBurnChance() {
			double x = this.age + (this.size / this.initialSize) * 2;
			double chance = BURN_MAX_CHANCE / (1 + Math.pow(Math.E, -BURN_STEEPNESS * (x) ));
			//System.out.println("Burn chance tree: " + chance);
			return chance;
		}
		
		public boolean spantaneousDeathChance() {
			double x = this.age;
			double chance = DEATH_MAX_CHANCE / (1 + Math.pow(Math.E, -DEATH_STEEPNESS * (x) ));
			//System.out.println("Death chance tree: " + chance);
			return Math.random() < chance;
		}
		
		public boolean equals(Object other) {
			if (!(other instanceof ProceduralTree)) {
				return false;
			}
			ProceduralTree tree = (ProceduralTree) other;
			return this.location.equals(tree.location);
		}
		
		public int hashCode() {
			return (int) (location.hashCode() + biome + age + size*100);
		}
	}
	
}
