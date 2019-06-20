package io.github.dantetam.world.worldgen;

import java.text.DecimalFormat;

import io.github.dantetam.vector.Vector2i;
import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.dataparse.ItemData;
import io.github.dantetam.world.dataparse.WorldCsvParser;
import io.github.dantetam.world.worldgen.newnoiselib.FastNoiseGen;
import io.github.dantetam.world.worldgen.newnoiselib.NoiseUtil;
import io.github.dantetam.world.worldgen.oldnoiselib.DiamondSquare2D;

public class LocalGridTerrainGenerate {

	//TODO 3d/2d perlin noise alg mix for 3d 'voxels' of terrain;
	// https://www.gamedev.net/forums/topic/612655-3d-perlin-noise-map-ridges/
	// https://www.gamedev.net/blogs/entry/2249106-more-procedural-voxel-world-generation/
	
	public static int[][][] genTerrain(Vector3i dimensions) {
		int[][][] terrain = new int[dimensions.x][dimensions.y][dimensions.z];
		
		FastNoiseGen noiseGenLib = new FastNoiseGen(FastNoiseGen.NoiseType.SimplexFractal);
		boolean[][][] uncommonStone = NoiseUtil.arrFloatToBool(noiseGenLib.getNoise(dimensions), 0.7f);
		
		noiseGenLib = new FastNoiseGen(FastNoiseGen.NoiseType.SimplexFractal);
		noiseGenLib.SetFractalOctaves(3);
		boolean[][][] rareStone = NoiseUtil.arrFloatToBool(noiseGenLib.getNoise(dimensions), 0.50f, 0, 0.02f);
		
		float[][][] perturbBinary = noiseGenLib.getNoise(dimensions);
		boolean[][][] perturbSurfaceBlocks = NoiseUtil.arrFloatToBool(perturbBinary, 0.50f, 40, -0.08f);
		float[][][] perturbSurfaceAmount = noiseGenLib.getNoise(dimensions);
		float[][][] dirtAmount = noiseGenLib.getNoise(dimensions);
		
		FastNoiseGen noiseGenLib2d = new FastNoiseGen(FastNoiseGen.NoiseType.SimplexFractal);
		noiseGenLib2d.SetFractalType(FastNoiseGen.FractalType.RigidMulti);
		float[][] surfaceLevel = noiseGenLib2d.getNoise(new Vector2i(dimensions.x, dimensions.y));
		
		for (int y = 0; y < dimensions.y; y++) {
			for (int x = 0; x < dimensions.x; x++) {
				int realHeight = (int) Math.min(surfaceLevel[x][y] * 70, dimensions.z - 1);
				for (int z = 0; z < dimensions.z; z++) {
					terrain[x][y][z] = ItemData.ITEM_EMPTY_ID;
					if (z <= realHeight) {
						if (rareStone[x][y][z]) {
							terrain[x][y][z] = ItemData.getIdFromName("Iron Sludge");
						}
						else if (uncommonStone[x][y][z]) {
							terrain[x][y][z] = ItemData.getIdFromName("Marble");
						}
						else {
							terrain[x][y][z] = ItemData.getIdFromName("Quartz");
						}
					}
				}
			}
		}
					
		for (int z = 0; z < dimensions.z; z++) {
			//This variable relies on z height, and determines the ability of a block to deviate from the ground baseline
			double perturbHeightGradientChance = 0.8 - (dimensions.z - z) * 0.1;
			for (int y = 0; y < dimensions.y; y++) {
				for (int x = 0; x < dimensions.x; x++) {
					int realHeight = (int) Math.min(surfaceLevel[x][y] * 70, dimensions.z - 1);
					int heightPerturb = (int) Math.round(perturbSurfaceAmount[x][y][z] * 8);
					if (perturbBinary[x][y][z] < perturbHeightGradientChance) {
						//Use 3d noise and "perturbed" blocks i.e. blocks brought away from the surface baseline
						if (perturbSurfaceBlocks[x][y][z]) {
							int modHeight = Math.min(z + heightPerturb, dimensions.z - 1);
							terrain[x][y][modHeight] = ItemData.getIdFromName("Pine Wood");
						}
					}
					if (z < realHeight) {
						//Use regular 2D noise like a regular height-map
						terrain[x][y][z] = ItemData.getIdFromName("Quartz");
					}
				}
			}
		}
		
		terrain = createCaves(terrain);
		
		return terrain;
	}
	
	/**
	 * This technique of multiplying together two ridged multifractal 3D Perlin noise objects,
	 * is described in the source below.  
	 * 
	 * JTippetts, "More Procedural Voxel World Generation", 2011
	 * https://www.gamedev.net/blogs/entry/2227887-more-on-minecraft-type-world-gen/
	 */
	private static int[][][] createCaves(int[][][] terrain) {
		FastNoiseGen noiseGenLib = new FastNoiseGen(FastNoiseGen.NoiseType.SimplexFractal);
		noiseGenLib.SetFractalType(FastNoiseGen.FractalType.RigidMulti);
		noiseGenLib.SetInterp(FastNoiseGen.Interp.Quintic);
		noiseGenLib.SetFractalOctaves(8);
		noiseGenLib.SetFrequency(2);
		float[][][] rigidMultiData = noiseGenLib.getNoise(
				new Vector3i(terrain.length, terrain[0].length, terrain[0][0].length));
		
		FastNoiseGen noiseGenLibSecond = new FastNoiseGen(FastNoiseGen.NoiseType.SimplexFractal, 
				(int) (System.currentTimeMillis() * Math.random()));
		noiseGenLibSecond.SetFractalType(FastNoiseGen.FractalType.RigidMulti);
		noiseGenLibSecond.SetInterp(FastNoiseGen.Interp.Quintic);
		noiseGenLibSecond.SetFractalOctaves(8);
		noiseGenLibSecond.SetFrequency(2);
		float[][][] rigidMultiDataSecond = noiseGenLibSecond.getNoise(
				new Vector3i(terrain.length, terrain[0].length, terrain[0][0].length));
		
		//TODO: Mod with 3d turbulence noise
		
		for (int z = 0; z < terrain[0][0].length; z++) {
			float modCutoff = 0.8f + 0.2f * ((float) z / terrain[0][0].length);
			for (int x = 0; x < terrain.length; x++) {
				for (int y = 0; y < terrain[0].length; y++) {
					float value = rigidMultiData[x][y][z] * rigidMultiDataSecond[x][y][z];
					if (value >= modCutoff) {
						terrain[x][y][z] = ItemData.getIdFromName("Coal");
					}
				}
			}
		}
		
		return terrain;
	}
	
	public static void main(String[] args) {
		WorldCsvParser.init();
		
		int[][][] world = genTerrain(new Vector3i(200,200,50));
		printTable(world[0]);
	}
	
	public static void printTable(int[][] a) {
		DecimalFormat df = new DecimalFormat("#.00"); 
		for (int i = 0; i < a.length; i++) {
			for (int j = 0; j < a[0].length; j++) {
				System.out.print(df.format(a[i][j]) + " ");
			}
			System.out.println();
		}
	}
	
	public static void printTable(boolean[][] a) {
		DecimalFormat df = new DecimalFormat("#.00"); 
		for (int i = 0; i < a.length; i++) {
			for (int j = 0; j < a[0].length; j++) {
				System.out.print((a[i][j] ? "T" : "F") + " ");
			}
			System.out.println();
		}
	}
	
}
