package io.github.dantetam.world.worldgen;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.lwjgl.util.vector.Vector2f;

import io.github.dantetam.localdata.ConstantData;
import io.github.dantetam.lwjglEngine.terrain.ForestGeneration;
import io.github.dantetam.lwjglEngine.terrain.ForestGeneration.BiomeData;
import io.github.dantetam.lwjglEngine.terrain.ForestGeneration.ProceduralTree;
import io.github.dantetam.lwjglEngine.terrain.RasterizeVoronoi;
import io.github.dantetam.toolbox.MapUtil;
import io.github.dantetam.toolbox.MathUti;
import io.github.dantetam.toolbox.StringUtil;
import io.github.dantetam.toolbox.VecGridUtil;
import io.github.dantetam.toolbox.log.CustomLog;
import io.github.dantetam.vector.Vector2i;
import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.ai.RSRPathfinder;
import io.github.dantetam.world.civhumanai.Ethos;
import io.github.dantetam.world.civilization.Household;
import io.github.dantetam.world.civilization.Society;
import io.github.dantetam.world.dataparse.ItemData;
import io.github.dantetam.world.grid.LocalGrid;
import io.github.dantetam.world.grid.LocalTile;
import io.github.dantetam.world.grid.SpaceFillingAlg;
import io.github.dantetam.world.grid.WorldGrid;
import io.github.dantetam.world.life.Human;
import io.github.dantetam.world.life.LivingEntity;
import io.github.dantetam.world.worldgen.DNAGridGeneration.DNATileData;
import io.github.dantetam.world.worldgen.oldnoiselib.BaseTerrain2D;
import io.github.dantetam.world.worldgen.oldnoiselib.DiamondSquare2D;
import kdtreegeo.KdTree;
import kn.uni.voronoitreemap.convexHull.JVertex;
import kn.uni.voronoitreemap.gui.JSite;
import kn.uni.voronoitreemap.j2d.Point2D;
import kn.uni.voronoitreemap.j2d.PolygonSimple;

/**
 * For generating mostly everything in a local grid, except the 3D terrain (like Dwarf Fortress).
 * This includes weather, local variations in 'biome', and multi-tile fixtures like trees and rocks.
 * @author Dante
 */

public class WorldGridInstantiate {

	private Vector2i worldSize;
	private int generatedTerrainLen;
	
	public WorldGridInstantiate(Vector2i worldSize) {
		this.worldSize = worldSize;
		generatedTerrainLen = (int) MathUti.roundToPower2(Math.max(worldSize.x, worldSize.y));
	}
	
	public WorldGrid setupGrid() {
		WorldGrid world = new WorldGrid(this.worldSize);
		
		double[][] elevation = generateTerrain(10, 0.4);
		double[][] temperature = generateTerrain(10, 0.4);
		double[][] yearRoundTempVariation = generateTerrain(10, 0.75);
		double[][] rain = generateTerrain(10, 0.6);
		
		//TODO Create all grids instantiated with
		//new societies, DNA/races/cultures, biomes/flora/fauna, and intersocietal interactions across grids;
				
		Vector3i gridSizes = new Vector3i(200,200,60);
		
		
		for (int r = 0; r < worldSize.x; r++) {
			for (int c = 0; c < worldSize.y; c++) {
				LocalGridBiome assignedBiome = null;
				
				LocalGrid grid = createNewLocalGridRoutine(gridSizes, assignedBiome);
			}
		}
		
		DNATileData[][] worldData = DNAGridGeneration.createGrid(worldSize);
		for (int r = 0; r < worldSize.x; r++) {
			for (int c = 0; c < worldSize.y; c++) {
				LocalGrid grid = world.getLocalGrid(new Vector2i(r,c));
				if (grid != null) {
					DNATileData tileData = worldData[r][c];
					for (LivingEntity being: grid.getAllLivingBeings()) {
						if (being instanceof Human) {
							Human human = (Human) being;
							human.dna.overrideDnaMapping("race", tileData.race);
							human.dna.overrideDnaMapping("culture", tileData.culture);
							String apparentCul = tileData.culture.repeat(1);
							for (int i = 0; i < (int) (Math.random() * 8); i++) {
								apparentCul = StringUtil.mutateAlphaNumStr(apparentCul);
							}
							human.brain.ethosSet.greatEthos.put("Culture", 
									new Ethos("Culture", 1.0, "MOD:" + apparentCul, ""));
							for (int i = 0; i < tileData.languages.size(); i++) {
								String language = tileData.languages.get(i);
								human.brain.languageCodesStrength.put(language, 1.0 / i);
							}
						}
					}
				}
			}
		}
		
		return world;
	}
	
	private LocalGrid createNewLocalGridRoutine(Vector3i gridSizes, LocalGridBiome biome) {
		LocalGrid grid = new LocalGridInstantiate(gridSizes, biome).setupGrid(true);
		
		Society testSociety = new Society("TestSociety", grid);
		testSociety.societyCenter = new Vector3i(50,50,30);
		
		int numHouses = 20;
		
		for (int i = 0; i < numHouses; i++) {
			int numPeopleHouse = (int)(Math.random() * 8) + 1;
			List<Human> people = new ArrayList<>();
			for (int j = 0; j < numPeopleHouse; j++) {
				int r, c;
				Vector3i availVec;
				do {
					r = (int) (Math.random() * grid.rows);
					c = (int) (Math.random() * grid.cols);
					availVec = grid.findHighestAccessibleHeight(r,c);
				} while (availVec == null);
				
				Human human = new Human(testSociety, "Human" + j + " of House " + i);
				people.add(human);
				grid.addHuman(human, availVec);
				
				/*
				human.inventory.addItem(ItemData.randomBaseItem());
				human.inventory.addItem(ItemData.randomBaseItem());
				human.inventory.addItem(ItemData.randomBaseItem());
				human.inventory.addItem(ItemData.randomBaseItem());
				human.inventory.addItem(ItemData.randomItem());
				*/
				human.inventory.addItem(ItemData.item("Wheat Seeds", 50));
				human.inventory.addItem(ItemData.item("Pine Wood", 50));
			}
			testSociety.addHousehold(new Household(people));
		}
		
		return grid;
	}
	
	public double[][] generateTerrain(double amp, double fract) {
		double[][] temp = DiamondSquare2D.makeTable(30, 30, 30, 30, generatedTerrainLen + 1);
		BaseTerrain2D map = new DiamondSquare2D(temp);
		double[][] terrain = map.generate(new double[] { 0, 0, generatedTerrainLen, amp, fract });
		return terrain;
	}
	
	public int[][] generateSoilCompositions() {
		double[][] temp = DiamondSquare2D.makeTable(20, 20, 20, 20, generatedTerrainLen + 1);
		BaseTerrain2D map = new DiamondSquare2D(temp);
		double[][] clayLevels = map.generate(new double[] { 0, 0, generatedTerrainLen, 5, 0.55 });
		
		temp = DiamondSquare2D.makeTable(30, 30, 30, 30, generatedTerrainLen + 1);
		map = new DiamondSquare2D(temp);
		double[][] sandLevels = map.generate(new double[] { 0, 0, generatedTerrainLen, 25, 0.45 });
		
		temp = DiamondSquare2D.makeTable(20, 20, 20, 20, generatedTerrainLen + 1);
		map = new DiamondSquare2D(temp);
		double[][] siltLevels = map.generate(new double[] { 0, 0, generatedTerrainLen, 10, 0.45 });

		int[][] soilIdsForTiles = new int[generatedTerrainLen + 1][generatedTerrainLen + 1];
		for (int r = 0; r < generatedTerrainLen + 1; r++) {
			for (int c = 0; c < generatedTerrainLen + 1; c++) {
				double[] data = {clayLevels[r][c], sandLevels[r][c], siltLevels[r][c]};
				double sum = Arrays.stream(data).sum();
				double[] normalizedSoilData = Arrays.stream(data).map(amount -> amount / sum).toArray();
				int soilId = getSoilItemIdFromData(normalizedSoilData);
				soilIdsForTiles[r][c] = soilId;
			}
		}
		return soilIdsForTiles;
	}
	
	//Return item id associated with soil composition
	public int getSoilItemIdFromData(double[] data) {
		double rand = Math.random();
		double clay = data[0], sand = data[1], silt = data[2];
		String soilName;
		if (clay > 0.4) {
			if (clay > 0.6) soilName = "Clay";
			else if (sand < 0.3 && silt < 0.3) soilName = "Soil";
			else if (sand > silt) {
				soilName = rand < sand ? "Soil (Sand)" : "Soil (Clay)";
			}
			else {
				soilName = rand < silt ? "Soil (Silt)" : "Soil (Clay)";
			}
		}
		else if (silt > 0.5) {
			if (silt > 0.8) soilName = "Silt";
			if (clay > sand) {
				soilName = rand < clay ? "Soil (Clay)" : "Soil (Silt)";
			}
			else soilName = "Soil (Silt)";
		}
		else if (sand > 0.45) {
			if (sand > 0.9) soilName = "Sand";
			if (clay < 0.2) {
				soilName = rand < (clay + silt) ? "Soil (Sand)" : "Sand";
			}
			else {
				soilName = rand < clay ? "Soil (Clay)" : "Soil (Sand)";
			}
		}
		else {
			soilName = "Soil";
		}
		return ItemData.getIdFromName(soilName);
	}
	
	public double[][] generateFlatTableDouble(int rows, int cols, double level) {
		double[][] newTable = new double[rows][cols];
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				newTable[r][c] = level;
			}
		}
		return newTable;
	}
	
	public int[][] generateFlatTableInt(int rows, int cols, int level) {
		int[][] newTable = new int[rows][cols];
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				newTable[r][c] = level;
			}
		}
		return newTable;
	}
	
}
