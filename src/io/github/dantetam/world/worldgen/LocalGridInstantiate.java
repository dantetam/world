package io.github.dantetam.world.worldgen;

import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.math3.distribution.NormalDistribution;

import io.github.dantetam.localdata.ConstantData;
import io.github.dantetam.lwjglEngine.terrain.ForestGeneration;
import io.github.dantetam.lwjglEngine.terrain.ForestGeneration.BiomeData;
import io.github.dantetam.lwjglEngine.terrain.ForestGeneration.ProceduralTree;
import io.github.dantetam.lwjglEngine.terrain.RasterizeVoronoi;
import io.github.dantetam.toolbox.MathUti;
import io.github.dantetam.toolbox.VecGridUtil;
import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.ai.HierarchicalPathfinder;
import io.github.dantetam.world.ai.RSRPathfinder;
import io.github.dantetam.world.dataparse.ItemData;
import io.github.dantetam.world.grid.LocalGrid;
import io.github.dantetam.world.grid.LocalTile;
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

public class LocalGridInstantiate {

	private LocalGrid localGrid;
	private int generatedTerrainLen;
	private int localGridBiome;
	
	public LocalGridInstantiate(Vector3i sizes, int biome) {
		localGrid = new LocalGrid(sizes);
		generatedTerrainLen = (int) MathUti.roundToPower2(Math.max(Math.max(sizes.x, sizes.y), sizes.z));
		localGridBiome = biome;
	}
	
	public LocalGrid setupGrid(boolean setupAdvancedPathfinding) {
		int[][][] advTerrainData = LocalGridTerrainGenerate.genTerrain(new Vector3i(localGrid.rows, localGrid.cols, localGrid.heights));
		
		double[][] terrain = generateTerrain();
		int[][] biomes = generateFlatTableInt(localGrid.rows, localGrid.cols, localGridBiome);
		double[][] temperature = generateFlatTableDouble(localGrid.rows, localGrid.cols, 0);
		double[][] rain = generateFlatTableDouble(localGrid.rows, localGrid.cols, 0);
		
		int grassId = ItemData.getIdFromName("Grass");
		double[][] gridGrasses = generateGrass(terrain, biomes, temperature, rain);
		
		int quartzId = ItemData.getIdFromName("Quartz");
		
		int airId = ItemData.ITEM_EMPTY_ID;
		
	    int[][] surfaceClusters = generateSurfaceClusters(
	    		ConstantData.clusterUbiquityMap, 
	    		ConstantData.clusterSizesMap, 
	    		terrain);
		
		double[][] soilLevels = generateSoilLevels();
		int[][] soilCompositions = generateSoilCompositions();	
		
		for (int r = 0; r < localGrid.rows; r++) {
			for (int c = 0; c < localGrid.cols; c++) {
				for (int h = 0; h < localGrid.heights; h++) {
					Vector3i coords = new Vector3i(r,c,h);
					int advTerrain = advTerrainData[r][c][h];

					LocalTile tile = localGrid.getTile(coords);
					//if (blockId != airId) { //tile.tileBlockId == airId
						if (tile == null) {
							tile = new LocalTile(coords);
							tile.tileBlockId = advTerrain;
							tile.tileFloorId = advTerrain;
							localGrid.setTileInstantiate(coords, tile);
						}
					//}
				}
			}
		}
		
		for (int r = 0; r < localGrid.rows; r++) {
			for (int c = 0; c < localGrid.cols; c++) {
				int height = localGrid.findHighestGroundHeight(r, c);
				int numTilesPassed = 0;
				
				Vector3i topCoord = new Vector3i(r, c, height + 1);
				if (localGrid.inBounds(topCoord)) {
					LocalTile topTile = localGrid.getTile(topCoord);
					int surfaceClusterItemId = surfaceClusters[r][c];
					if (surfaceClusterItemId != ItemData.ITEM_EMPTY_ID) {
						topTile.tileBlockId = surfaceClusterItemId;
					}
					localGrid.setTileInstantiate(topCoord, topTile);
				}
				
				while (height > 0 && numTilesPassed < (int) soilLevels[r][c]) {
					Vector3i coords = new Vector3i(r,c,height);
					
					height--;
					
					LocalTile tile = localGrid.getTile(coords);
					if (tile == null) continue;
					
					tile.exposedToAir = true;
					
					if (numTilesPassed == 0 && gridGrasses[r][c] != 0) {
						tile.tileBlockId = grassId;
						tile.tileFloorId = grassId;
					}
					else {
						tile.tileBlockId = soilCompositions[r][c];
						tile.tileFloorId = soilCompositions[r][c];
					}
					numTilesPassed++;
				}
			}
		}
		
		Map<int[], ProceduralTree> gridTrees = generateTrees(terrain, biomes, temperature, rain);
		for (Entry<int[], ProceduralTree> entry : gridTrees.entrySet()) {
			TreeVoxelGeneration.generateSingle3dTree(localGrid, entry.getKey(), entry.getValue());
		}
		
		if (setupAdvancedPathfinding)
			localGrid.pathfinder = new RSRPathfinder(localGrid);
		
		System.out.println(localGrid.rows + " " + localGrid.cols + " " + localGrid.heights);
		System.out.println(gridTrees.size());
		
		localGrid.connectedCompsMap = VecGridUtil.contComponent3dSolids(null, null, localGrid);
		localGrid.clustersList = new KdTree(VecGridUtil.contComp3dSolidsClustersSpec(null, null, localGrid));
		
		return localGrid;
	}
	
	public double[][] generateTerrain() {
		double[][] temp = DiamondSquare2D.makeTable(30, 30, 30, 30, generatedTerrainLen + 1);
		BaseTerrain2D map = new DiamondSquare2D(temp);
		double[][] terrain = map.generate(new double[] { 0, 0, generatedTerrainLen, 8, 0.5 });
		return terrain;
	}
	
	public double[][] generateSoilLevels() {
		double[][] temp = DiamondSquare2D.makeTable(4, 4, 4, 4, generatedTerrainLen + 1);
		BaseTerrain2D map = new DiamondSquare2D(temp);
		double[][] soilLevels = map.generate(new double[] { 0, 0, generatedTerrainLen, 8, 0.65 });
		return soilLevels;
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
		int averageDistance = 25;
		
		Object[] forestData = ForestGeneration.generateForest(topLeftBound, bottomRightBound, averageDistance, 
				terrain, biomes, temperature, rain, 1); 
		List<JSite> voronoi = (List) forestData[0]; 
		Map<Integer, ProceduralTree> polygonForestData = (Map) forestData[1]; 
		Map<Integer, BiomeData> polygonBiomeData = (Map) forestData[2];
		
		//Directly convert the centroids of the Voronoi polygons into tree tile locations
		Map<int[], ProceduralTree> treesByTileLocations = new HashMap<>();
		for (Entry<Integer, ProceduralTree> entry: polygonForestData.entrySet()) {
			JSite polygon = voronoi.get(entry.getKey());
			Point2D centroid = polygon.getSite().getPolygon().getCentroid();
			int[] tileLocation = {(int) (centroid.x / rasterExpandFactor), (int) (centroid.y / rasterExpandFactor)};
			treesByTileLocations.put(tileLocation, entry.getValue());
		}
		return treesByTileLocations;
	}
	
	public double[][] generateGrass(double[][] terrain, int[][] biomes, double[][] temperature, double[][] rain) {
		int rasterExpandFactor = 6;
		Point2D topLeftBound = new Point2D(0,0),
				bottomRightBound = new Point2D(localGrid.rows * rasterExpandFactor, localGrid.cols * rasterExpandFactor);
		int averageDistance = 25;
		
		//Use the Forest Generation to create a forest using the Voronoi libraries and advanced terrain generation
		Object[] forestData = ForestGeneration.generateForest(topLeftBound, bottomRightBound, averageDistance, 
				terrain, biomes, temperature, rain, 2.5); 
		List<JSite> voronoi = (List) forestData[0]; 
		
		//Rasterize the Voronoi polygons tile by tile
		Rectangle2D.Double voronoiBounds = new Rectangle2D.Double(
				topLeftBound.x, topLeftBound.y, bottomRightBound.x, bottomRightBound.y);
		double[][] rasterizedGrass = RasterizeVoronoi.getPixelRasterGridFromVoronoi(voronoi, voronoiBounds, 
				biomes, 1, 1);
		return rasterizedGrass;
	}
	
	/**
	 * Use a combination of normal Gaussian distributions to simulate a multi-variate Gaussian.
	 * This simulates a probabilistic clustering around a certain point, to simulate resource patches.
	 * 
	 * A random process uses the multi-variate Gaussian to lossely follow it when creating a cluster.
	 * This random process creates a kind of 'forced' random covariance on this distribution.
	 * 
	 * 
	 * 
	 * @param clusterUbiquityMap  The mean number of clusters of this item
	 * @param clusterSizesMap	  The mean radius of each cluster, in either 2d dimension
	 * @param terrain
	 * @return
	 */
	public int[][] generateSurfaceClusters(Map<Integer, Double> clusterUbiquityMap, 
			Map<Integer, Double> clusterSizesMap, double[][] terrain) {
		int[][] surfaceClusters = new int[terrain.length][terrain[0].length];
		for (int r = 0; r < terrain.length; r++) {
			for (int c = 0; c < terrain[0].length; c++) {
				surfaceClusters[r][c] = ItemData.ITEM_EMPTY_ID;
			}
		}
		
		for (Entry<Integer, Double> entry: clusterUbiquityMap.entrySet()) {
			int itemId = entry.getKey();
			double clusterUbiquity = entry.getValue();
			int actualNumClusters = (int) (new NormalDistribution(clusterUbiquity, clusterUbiquity * 0.3).sample());
			
			double clusterSize = 5;
			if (clusterSizesMap.containsKey(itemId)) {
				clusterSize = clusterSizesMap.get(itemId);
			}
			else {
				//throw new IllegalArgumentException("Cluster data request does not match: " + itemId);
			}
			double meanWithinCluster = clusterSize / 1.85;
			
			for (int clusterNum = 0; clusterNum < actualNumClusters; clusterNum++) {
				double meanR = meanWithinCluster * (Math.random()*0.8 + 0.6),
						meanC = meanWithinCluster * (Math.random()*0.8 + 0.6);
				NormalDistribution gaussianResExist = new NormalDistribution(0, 
						(meanR + meanC) / 2 * 0.6);
				
				NormalDistribution gaussianR = new NormalDistribution(meanR, meanR * 0.4);
				NormalDistribution gaussianC = new NormalDistribution(meanC, meanC * 0.4);
				int rows = (int) Math.round(gaussianR.sample());
				int cols = (int) Math.round(gaussianC.sample());
				
				int randomR = (int) (terrain.length * Math.random());
				int randomC = (int) (terrain[0].length * Math.random());
				for (int r = -rows; r <= rows; r++) {
					for (int c = -cols; c <= cols; c++) {
						int distCenter = Math.abs(r) + Math.abs(c);
						double probability = 1.25 - gaussianResExist.cumulativeProbability(distCenter);
						if (Math.random() < probability) {
							int trueR = randomR + r, trueC = randomC + c;
							if (trueR >= 0 && trueC >= 0 && trueR < terrain.length && trueC < terrain[0].length) {
								surfaceClusters[trueR][trueC] = itemId;
							}
						}
					}
				}
			}
		}
		return surfaceClusters;
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
