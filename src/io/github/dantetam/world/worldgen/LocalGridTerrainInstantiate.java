package io.github.dantetam.world.worldgen;

import java.util.Arrays;

import io.github.dantetam.lwjglEngine.toolbox.CustomMathUtil;
import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.grid.LocalGrid;
import terrain.BaseTerrain;
import terrain.DiamondSquare;

public class LocalGridTerrainInstantiate {

	public LocalGrid localGrid;
	
	private int terrainPower2;
	
	public LocalGridTerrainInstantiate(Vector3i sizes) {
		localGrid = new LocalGrid(sizes);
		terrainPower2 = (int) CustomMathUtil.roundToPower2(Math.max(Math.max(sizes.x, sizes.y), sizes.z));
	}
	
	public void setupGrid() {
		
	}
	
	public double[][] generateTerrain() {
		double[][] temp = DiamondSquare.makeTable(30, 30, 30, 30, terrainPower2 + 1);
		BaseTerrain map = new DiamondSquare(temp);
		double[][] terrain = map.generate(new double[] { 0, 0, terrainPower2, 8, 0.5 });
		return terrain;
	}
	
	public double[][] generateSoilLevels() {
		double[][] temp = DiamondSquare.makeTable(0, 0, 0, 0, terrainPower2 + 1);
		BaseTerrain map = new DiamondSquare(temp);
		double[][] soilLevels = map.generate(new double[] { 0, 0, terrainPower2, 5, 0.65 });
		return soilLevels;
	}
	
	public double[][][] generateSoilCompositions() {
		double[][] temp = DiamondSquare.makeTable(20, 20, 20, 20, terrainPower2 + 1);
		BaseTerrain map = new DiamondSquare(temp);
		double[][] clayLevels = map.generate(new double[] { 0, 0, terrainPower2, 5, 0.55 });
		
		temp = DiamondSquare.makeTable(30, 30, 30, 30, terrainPower2 + 1);
		map = new DiamondSquare(temp);
		double[][] sandLevels = map.generate(new double[] { 0, 0, terrainPower2, 25, 0.45 });
		
		temp = DiamondSquare.makeTable(20, 20, 20, 20, terrainPower2 + 1);
		map = new DiamondSquare(temp);
		double[][] siltLevels = map.generate(new double[] { 0, 0, terrainPower2, 10, 0.45 });
		
		double[][][] normalizedSoilData = new double[terrainPower2 + 1][terrainPower2 + 1][3];
		for (int r = 0; r < terrainPower2 + 1; r++) {
			for (int c = 0; c < terrainPower2 + 1; c++) {
				double[] data = {clayLevels[r][c], sandLevels[r][c], siltLevels[r][c]};
				double sum = Arrays.stream(data).sum();
				data = Arrays.stream(data).map(amount -> amount / sum).toArray();
				normalizedSoilData[r][c] = data;
			}
		}
		return normalizedSoilData;
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
	}
	
	public double[] generateSandstoneIgneousHeights() {
		
	}
	
	public double[] generateIgneousMetaHeights() {
		
	}
	
}
