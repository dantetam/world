package io.github.dantetam.world.worldgen;

import java.text.DecimalFormat;

import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.worldgen.newnoiselib.FastNoiseGen;
import io.github.dantetam.world.worldgen.oldnoiselib.DiamondSquare2D;

public class LocalGridTerrainGenerate {

	//TODO 3d/2d perlin noise alg mix for 3d 'voxels' of terrain;
	// https://www.gamedev.net/forums/topic/612655-3d-perlin-noise-map-ridges/
	// https://www.gamedev.net/blogs/entry/2249106-more-procedural-voxel-world-generation/
	
	public static float[][][] genTerrain(Vector3i dimensions) {
		FastNoiseGen noiseGenLib = new FastNoiseGen(FastNoiseGen.NoiseType.SimplexFractal);
		float[][][] uncommonStone = noiseGenLib.getNoise(dimensions);
		
		return world;
	}
	
	public static void main(String[] args) {
		float[][][] world = genTerrain(new Vector3i(200,200,50));
		printTable(world[0]);
	}
	
	public static void printTable(float[][] a) {
		DecimalFormat df = new DecimalFormat("#.00"); 
		for (int i = 0; i < a.length; i++) {
			for (int j = 0; j < a[0].length; j++) {
				System.out.print(df.format(a[i][j]) + " ");
			}
			System.out.println();
		}
	}
	
}
