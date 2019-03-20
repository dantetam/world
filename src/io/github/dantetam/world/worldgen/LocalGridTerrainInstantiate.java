package io.github.dantetam.world.worldgen;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import io.github.dantetam.lwjglEngine.terrain.ForestGeneration;
import io.github.dantetam.lwjglEngine.terrain.ForestGeneration.BiomeData;
import io.github.dantetam.lwjglEngine.terrain.ForestGeneration.ProceduralTree;
import io.github.dantetam.toolbox.CustomMathUtil;
import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.dataparse.ItemData;
import io.github.dantetam.world.grid.LocalGrid;
import io.github.dantetam.world.grid.LocalTile;
import kn.uni.voronoitreemap.gui.JSite;
import kn.uni.voronoitreemap.j2d.Point2D;
import kn.uni.voronoitreemap.j2d.PolygonSimple;
import terrain.BaseTerrain;
import terrain.DiamondSquare;

public class LocalGridTerrainInstantiate {

	private LocalGrid localGrid;
	private int generatedTerrainLen;
	private int localGridBiome;
	
	public LocalGridTerrainInstantiate(Vector3i sizes, int biome) {
		localGrid = new LocalGrid(sizes);
		generatedTerrainLen = (int) CustomMathUtil.roundToPower2(Math.max(Math.max(sizes.x, sizes.y), sizes.z));
		localGridBiome = biome;
	}
	
	public LocalGrid setupGrid() {
		double[][] terrain = generateTerrain();
		double[][] soilLevels = generateSoilLevels();
		int[][] soilCompositions = generateSoilCompositions();
		for (int r = 0; r < localGrid.rows; r++) {
			for (int c = 0; c < localGrid.cols; c++) {
				for (double h = terrain[r][c]; h >= terrain[r][c] - soilLevels[r][c]; h--) {
					if (h <= 0) break;
					int height = (int) h;
					Vector3i coords = new Vector3i(r,c,height);
					LocalTile newTile = new LocalTile(coords);
					newTile.tileBlockId = soilCompositions[r][c];
					newTile.tileFloorId = soilCompositions[r][c];
					localGrid.setTileInstantiate(coords, newTile);
				}
			}
		}
		
		int[][] biomes = generateFlatTableInt(localGrid.rows, localGrid.cols, localGridBiome);
		double[][] temperature = generateFlatTableDouble(localGrid.rows, localGrid.cols, 0);
		double[][] rain = generateFlatTableDouble(localGrid.rows, localGrid.cols, 0);
		Map<int[], ProceduralTree> gridTrees = generateTrees(terrain, biomes, temperature, rain);
		for (Entry<int[], ProceduralTree> entry: gridTrees.entrySet()) {
			System.out.println(Arrays.toString(entry.getKey()));
		}
		System.out.println(gridTrees.size());
		
		return localGrid;
	}
	
	public double[][] generateTerrain() {
		double[][] temp = DiamondSquare.makeTable(30, 30, 30, 30, generatedTerrainLen + 1);
		BaseTerrain map = new DiamondSquare(temp);
		double[][] terrain = map.generate(new double[] { 0, 0, generatedTerrainLen, 8, 0.5 });
		return terrain;
	}
	
	public double[][] generateSoilLevels() {
		double[][] temp = DiamondSquare.makeTable(0, 0, 0, 0, generatedTerrainLen + 1);
		BaseTerrain map = new DiamondSquare(temp);
		double[][] soilLevels = map.generate(new double[] { 0, 0, generatedTerrainLen, 5, 0.65 });
		return soilLevels;
	}
	
	public int[][] generateSoilCompositions() {
		double[][] temp = DiamondSquare.makeTable(20, 20, 20, 20, generatedTerrainLen + 1);
		BaseTerrain map = new DiamondSquare(temp);
		double[][] clayLevels = map.generate(new double[] { 0, 0, generatedTerrainLen, 5, 0.55 });
		
		temp = DiamondSquare.makeTable(30, 30, 30, 30, generatedTerrainLen + 1);
		map = new DiamondSquare(temp);
		double[][] sandLevels = map.generate(new double[] { 0, 0, generatedTerrainLen, 25, 0.45 });
		
		temp = DiamondSquare.makeTable(20, 20, 20, 20, generatedTerrainLen + 1);
		map = new DiamondSquare(temp);
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
	
	/**
	 * @param terrain
	 * @param biomes
	 * @param temperature
	 * @param rain
	 * @return A map of 2D tile coordinates to tree objects containing type, size, etc.
	 */
	public Map<int[], ProceduralTree> generateTrees(double[][] terrain, int[][] biomes, double[][] temperature, double[][] rain) {
		int rasterExpandFactor = 6;
		Point2D topLeftBound = new Point2D(0,0),
				bottomRightBound = new Point2D(localGrid.rows * rasterExpandFactor, localGrid.cols * rasterExpandFactor);
		int averageDistance = 3 * rasterExpandFactor;
		
		Object[] forestData = ForestGeneration.generateForest(topLeftBound, bottomRightBound, averageDistance, 
				terrain, biomes, temperature, rain); 
		List<JSite> voronoi = (List) forestData[0]; 
		Map<Integer, ProceduralTree> polygonForestData = (Map) forestData[1]; 
		Map<Integer, BiomeData> polygonBiomeData = (Map) forestData[2];
		
		Map<int[], ProceduralTree> treesByTileLocations = new HashMap<>();
		for (Entry<Integer, ProceduralTree> entry: polygonForestData.entrySet()) {
			JSite polygon = voronoi.get(entry.getKey());
			Point2D centroid = polygon.getSite().getPolygon().getCentroid();
			int[] tileLocation = {(int) (centroid.x / rasterExpandFactor), (int) (centroid.y / rasterExpandFactor)};
			treesByTileLocations.put(tileLocation, entry.getValue());
		}
		return treesByTileLocations;
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
	
	public double[] generateSandstoneIgneousHeights() {
		return null;
	}
	
	public double[] generateIgneousMetaHeights() {
		return null;
	}
	
}
