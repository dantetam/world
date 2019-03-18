package io.github.dantetam.lwjglEngine.terrain;

/**
 * Using the Voronoi power diagram and parallelogram noise libraries, as well as 
 * rasterization point testing algorithms, generate 2D noise.
 */

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import kdtreegeo.KdTree;

import java.util.Set;

import kn.uni.voronoitreemap.convexHull.JVertex;
import kn.uni.voronoitreemap.gui.JSite;
import kn.uni.voronoitreemap.gui.VoronoiLibrary;
import kn.uni.voronoitreemap.j2d.Point2D;
import kn.uni.voronoitreemap.j2d.PolygonSimple;
import io.github.dantetam.lwjglEngine.terrain.NeighborsGraphStructure.Edge;
import io.github.dantetam.lwjglEngine.toolbox.RGBUtil;

public class RasterizeVoronoi {

	private static double[][] convertRasterToTerrain(double rasterLandPercent, boolean[][] rasterGrid, int rows,
			int cols, int rasterRowTimes, int rasterColTimes) {
		double[][] terrain = new double[cols][rows];
		for (int c = 0; c < cols; c++) {
			for (int r = 0; r < rows; r++) {
				int landCount = 0, tileCount = 0;
				int tileStartC = c * rasterColTimes, tileStartR = r * rasterRowTimes;
				for (int rasterC = tileStartC; rasterC < tileStartC + rasterColTimes; rasterC++) {
					for (int rasterR = tileStartR; rasterR < tileStartR + rasterRowTimes; rasterR++) {
						tileCount++;
						if (rasterGrid[rasterC][rasterR]) {
							landCount++;
						}
					}
				}
				// TODO: Smarter way of counting tileCount, which is always a rectangle
				double landPercent = (double) landCount / (double) tileCount;
				if (landPercent >= rasterLandPercent) {
					terrain[c][r] = 100 * landPercent;
				} else {
					terrain[c][r] = 0;
				}
			}
		}
		return terrain;
	}
	
	public static double[][] getTransposedTable(double[][] arr) {
        double[][] transposed = new double[arr[0].length][arr.length];
        for (int i = 0; i < arr.length; i++) {
            for (int j = 0; j < arr[0].length; j++) {
                transposed[j][i] = arr[i][j];
            }
        }
        return transposed;
	}
	
	/**
	 * Return a new double[][] arr representing the pixels of a new blend map, i.e. a grayscale terrain.
	 * @param voronoi
	 * @param voronoiBounds
	 * @param biomesGuide
	 * @param rasterRowTimes
	 * @param rasterColTimes
	 * @return
	 */
	private static double[][] getPixelRasterGridFromVoronoi(List<JSite> voronoi, Rectangle2D.Double voronoiBounds,
			int[][] biomesGuide, int rasterRowTimes, int rasterColTimes) {
		int rows = biomesGuide.length, cols = biomesGuide[0].length;
		double[][] rasterResults = new double[cols * rasterColTimes][rows * rasterRowTimes];
		for (int r = 0; r < rasterResults.length; r++) {
			for (int c = 0; c < rasterResults[0].length; c++) {
				rasterResults[r][c] = -1;
			}
		}

		//Create this geometric data all in advance for use in overlapping calculations later
		KdTree<Point2D> centroidKdTree = new KdTree<>(); 
		Map<Point2D, Integer> centroidToPolygons = new HashMap<>();
		for (int i = 0; i < voronoi.size(); i++) {
			JVertex poly = voronoi.get(i).getSite();
			centroidToPolygons.put(new Point2D(poly.x, poly.y), i);
			centroidKdTree.add(new Point2D(poly.x, poly.y));
		}
		Map<Integer, int[]> biomeAtPolyVerticesMap = new HashMap<>();
		for (int polygonIndex = 0; polygonIndex < voronoi.size(); polygonIndex++) {
			JSite jsite = voronoi.get(polygonIndex);
			PolygonSimple polygon = jsite.getSite().getPolygon();
			int[] biomesAtVertices = getBiomePolyVoronoiSpace(polygon, voronoiBounds, biomesGuide);
			biomeAtPolyVerticesMap.put(polygonIndex, biomesAtVertices);
		}
		
		int failedMatches = 0;
		
		for (int rasterCol = 0; rasterCol < cols * rasterColTimes; rasterCol++) {
			for (int rasterRow = 0; rasterRow < rows * rasterRowTimes; rasterRow++) {
				double voronoiSpaceX = ((rasterCol + 0.5) / rasterResults.length) * voronoiBounds.width + voronoiBounds.x;
				double voronoiSpaceY = ((rasterRow + 0.5) / rasterResults[0].length) * voronoiBounds.height + voronoiBounds.y;
				Point2D inspectPoint = new Point2D(voronoiSpaceX, voronoiSpaceY);
				
				boolean foundMatch = false;
				
				//Search for the polygon containing this calculated point in the 'Voronoi space'
				//Note that because of the noise generation algorithms, there is no guarantee either way between
				//point is closest to centroid x <-> this point 'belongs' to the modified Voronoi polygon.
				//This was a guaranteed if and only if relationship for normal Voronoi diagrams.
				Collection<Point2D> nearestNeighbors = centroidKdTree.nearestNeighbourListSearch(7, inspectPoint);
				for (Point2D neighborCandidate : nearestNeighbors) {
					int polygonCandidateIndex = centroidToPolygons.get(neighborCandidate);
					PolygonSimple polygonCandidate = voronoi.get(polygonCandidateIndex).getSite().getPolygon();
					if (polygonCandidate.contains(inspectPoint)) {
						//Get stored biome data for polygon's vertices
						int[] biomeAtVertices = biomeAtPolyVerticesMap.get(polygonCandidateIndex);
						
						double[] fWeights = new double[polygonCandidate.length];
						for (int i = 0; i < polygonCandidate.length; i++) {
							fWeights[i] = 1;		
						}
								
						//Set the 'raster' data to be the interpolated biome data across this polygon
						double avgBiome = 0;
						double[] vertexWeights = PolygonInterpolation.getPolyCoords(polygonCandidate, new Point2D(voronoiSpaceX, voronoiSpaceY),
								fWeights);
						for (int polygonIndex = 0; polygonIndex < polygonCandidate.length; polygonIndex++) {
							if (vertexWeights[polygonIndex] < -1) {
								//System.out.println("Faulty weight: " + vertexWeights[polygonIndex]);
							}
							else {
								avgBiome += vertexWeights[polygonIndex] * biomeAtVertices[polygonIndex];
							}
						}
						rasterResults[rasterCol][rasterRow] = (int) Math.round(avgBiome); //avgBiome;
						foundMatch = true;
						break;
					}
 				}
				
				if (!foundMatch)
					failedMatches++;
			}
		}
		
		System.out.println("Failed matches: " + failedMatches);
		
		return rasterResults;
	}
	
	private static int[] getBiomePolyVoronoiSpace(PolygonSimple polygon, Rectangle2D.Double voronoiBounds, int[][] biomesGuide) {
		int[] biomeAtVertices = new int[polygon.length];
		for (int polygonIndex = 0; polygonIndex < polygon.length; polygonIndex++) {
			Point2D vertex = polygon.getPoint(polygonIndex);
			double percentageX = (vertex.x - voronoiBounds.x) / voronoiBounds.width;
			double percentageY = (vertex.y - voronoiBounds.y) / voronoiBounds.height;
			int tileX = (int) (biomesGuide[0].length * percentageX);
			int tileY = (int) (biomesGuide.length * percentageY);
			tileX = Math.min(tileX, biomesGuide[0].length - 1);
			tileY = Math.min(tileY, biomesGuide.length - 1);
			int biome = biomesGuide[tileY][tileX];
			biomeAtVertices[polygonIndex] = biome;
		}
		return biomeAtVertices;
	}

	private static boolean[][] fillInRasterHoles(boolean[][] origRaster) {
		boolean[][] newRaster = new boolean[origRaster.length][origRaster[0].length];
		for (int r = 0; r < newRaster.length; r++) {
			for (int c = 0; c < newRaster[0].length; c++) {
				newRaster[r][c] = origRaster[r][c];
				if (r > 0 && c > 0 && r < newRaster.length - 1 && c < newRaster[0].length - 1) {
					boolean verticalDark = !origRaster[r - 1][c] && !origRaster[r + 1][c];
					boolean horizontalDark = !origRaster[r][c - 1] && !origRaster[r][c + 1];
					boolean verticalLight = origRaster[r - 1][c] && origRaster[r + 1][c];
					boolean horizontalLight = !origRaster[r][c - 1] && origRaster[r][c + 1];
					if ((verticalDark && horizontalLight) || (horizontalDark && verticalLight)) {
						newRaster[r][c] = true;
					} else if (verticalDark && horizontalDark) {
						newRaster[r][c] = false;
					} else if (verticalLight && horizontalLight) {
						newRaster[r][c] = true;
					}
				}
			}
		}
		return newRaster;
	}

	//The process of creating a random Voronoi tessellation and then performing Lloyd relaxation,
	//unintentionally simulates a Poisson point process, such that the resulting
	//Poisson-Voronoi tessellation has well known statistical properties. 
	//Particularly, E(Voronoi polygon edges) ~ 6 and Var(Voronoi polygon edges) ~ 1.78.
	//See "Statistical Distributions of Poisson Voronoi Cells...", Tanemura, 2002, and also
	//http://www.mathematik.uni-ulm.de/stochastik/fundl/paper/frey/survey2/node17.html
	
	/**
	 * 
	 * @param terrain
	 * @param biomes
	 * @param rasterRows
	 * @param rasterCols
	 * @param transposed
	 * @return
	 */
	public static BufferedImage generateVoronoiTesselPoly(double[][] terrain, int[][] biomes, int rasterRows, int rasterCols, boolean transposed) {		
		Point2D topLeftBound = new Point2D(0, 0);
		Point2D bottomRightBound = new Point2D(rasterCols * terrain[0].length, rasterRows * terrain.length);
		double averageDistance = 50;
		int lloydRelaxationTimes = 2;
		List<JSite> voronoi = VoronoiLibrary.voronoiLib(topLeftBound, bottomRightBound, averageDistance,
				lloydRelaxationTimes);
		Rectangle2D.Double voronoiBounds = new Rectangle2D.Double(0, 0, rasterCols * terrain[0].length, rasterRows * terrain.length);
		//voronoi = ParallelogramNoise.parallelogramNoiseOnVoronoi(voronoi, 0.25, 0.1);

		double[][] rasterData = getPixelRasterGridFromVoronoi(voronoi, voronoiBounds, biomes, rasterRows, rasterCols);
		
		BufferedImage terrainImg = getBlendMapFromTerrainData(rasterData, 1, transposed);
		return terrainImg;
	}
	
	public static void main(String[] args) {
		/*
		Point2D topLeftBound = new Point2D(0, 0);
		Point2D bottomRightBound = new Point2D(1600, 1600);
		double averageDistance = 50;
		int lloydRelaxationTimes = 4;
		List<JSite> voronoi = VoronoiLibrary.voronoiLib(topLeftBound, bottomRightBound, averageDistance,
				lloydRelaxationTimes);
		Rectangle2D.Double voronoiBounds = new Rectangle2D.Double(0, 0, 1600, 1600);

		//voronoi = ParallelogramNoise.parallelogramNoiseOnVoronoi(voronoi, 0.15, 0.15);

		long seed = 500L; //System.currentTimeMillis(); //500L

		int len = 32;
		double[][] temp = DiamondSquare.makeTable(50, 50, 50, 50, len + 1);
		temp[temp.length / 2][temp[0].length / 2] = 200;
		temp[0][temp[0].length / 2] = 50;
		temp[temp.length - 1][temp[0].length / 2] = 50;
		temp[temp[0].length / 2][0] = 50;
		temp[temp[0].length / 2][temp.length - 1] = 50;
		BaseTerrain map = new DiamondSquare(temp);
		map.seed(seed);
		double[][] terrain = map.generate(new double[] { 0, 0, len, 40, 0.7 });
		double cutoff = 100;

		//int rows = 66, cols = 66, rasterRows = 10, rasterCols = 10;
		int rasterRows = 40, rasterCols = 40;
		GameTerrainGeneration gameTerrainGen = new GameTerrainGeneration(seed);
		int[][] biomes = gameTerrainGen.getAssignedBiome(terrain, cutoff);
		double[][] rasterData = getPixelRasterGridFromVoronoi(voronoi, voronoiBounds, biomes, rasterRows, rasterCols);

		BufferedImage terrain2Img = getBlendMapFromTerrainData(rasterData, 1, false);
		RGBUtil.writeBlendMapToFile(terrain2Img, "res/testVoronoiBlendMap.png");
		//printTable(rasterData);
		System.out.println("Completed writing to file: blendMap");
		
		printTable(biomes);
		*/
	}

	private static BufferedImage getBlendMapFromTerrainData(double[][] terrain, int scaleMap, boolean transposed) {
		int rows = terrain.length, cols = terrain[0].length;
		int blendMapWidth = rows * scaleMap;
		int blendMapHeight = cols * scaleMap;
		if (transposed) {
			rows = cols;
			cols = terrain.length;
			blendMapWidth = rows * scaleMap;
			blendMapHeight = cols * scaleMap;
		}
		try {
			BufferedImage img = new BufferedImage(blendMapWidth, blendMapHeight, BufferedImage.TYPE_INT_RGB);
			int chunkWidth = (int) ((float) blendMapWidth / (float) rows),
					chunkHeight = (int) ((float) blendMapHeight / (float) cols);
			int[][] colors = new int[blendMapWidth][blendMapHeight];
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					double data = transposed ? terrain[c][r] : terrain[r][c];
					data = data < 0 ? 0 : data;
					int gray = (int) (data * 255.0 / 8.0);
					int intColor = RGBUtil.getIntColor(gray, gray, gray);
					for (int rr = r * chunkWidth; rr < (r + 1) * chunkWidth; rr++) {
						for (int cc = c * chunkHeight; cc < (c + 1) * chunkHeight; cc++) {
							if (rr >= colors.length || cc >= colors[0].length)
								break;
							if (rr < 0 || cc < 0)
								continue;
							colors[rr][cc] = intColor;
						}
					}
				}
			}
			for (int r = 0; r < colors.length; r++) {
				for (int c = 0; c < colors[0].length; c++) {
					img.setRGB(r, c, colors[r][c]);
				}
			}
			return img;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

}
