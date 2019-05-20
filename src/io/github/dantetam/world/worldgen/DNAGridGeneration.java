package io.github.dantetam.world.worldgen;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.dantetam.lwjglEngine.terrain.RasterizeVoronoi;
import io.github.dantetam.toolbox.StringUtil;
import io.github.dantetam.lwjglEngine.terrain.NeighborsGraphStructure;
import io.github.dantetam.lwjglEngine.terrain.NeighborsGraphStructure.Edge;
import io.github.dantetam.vector.Vector2i;
import io.github.dantetam.world.life.DNAHuman;
import kn.uni.voronoitreemap.gui.JSite;
import kn.uni.voronoitreemap.gui.VoronoiLibrary;
import kn.uni.voronoitreemap.j2d.Point2D;

public class DNAGridGeneration {

	/**
	 * @return A mapping for the world's starting distribution of DNA, cultures, and so on.
	 */
	public static DNATileData[][] createGrid(Vector2i dimensions) {
		Point2D topLeftBound = new Point2D(0, 0);
		Point2D bottomRightBound = new Point2D(1600, 1600);
		double averageDistance = 50;
		int lloydRelaxationTimes = 4;
		List<JSite> voronoi = VoronoiLibrary.voronoiLib(topLeftBound, bottomRightBound, averageDistance,
				lloydRelaxationTimes);
		Rectangle2D.Double voronoiBounds = new Rectangle2D.Double(0, 0, 1600, 1600);
		int[][] polyIndicesArr = RasterizeVoronoi.rasterizedPolyPointMap(voronoi, voronoiBounds, dimensions.x, dimensions.y);
		
		Object[] neighborData = NeighborsGraphStructure.computePolygonalNeighbors(voronoi);
		Map<Integer, Set<Integer>> polygonNeighborMap = (Map) neighborData[0];
		//Map<Edge, List<Integer>> sharedEdgesMap = (Map) neighborData[1];
		
		Map<Integer, DNATileData> initialData = initializeVoronoi(voronoi.size(), polygonNeighborMap);
		
		DNATileData[][] data = new DNATileData[dimensions.x][dimensions.y];
		for (int r = 0; r < dimensions.x; r++) {
			for (int c = 0; c < dimensions.y; c++) {
				int polyIndex = polyIndicesArr[r][c];
				DNATileData dnaAtPoly = initialData.get(polyIndex);
				TODO
			}
		}
	}
	
	private static Map<Integer, DNATileData> initializeVoronoi(int numPolygons, Map<Integer, Set<Integer>> polygonNeighborMap) {
		Map<Integer, DNATileData> data = new HashMap<>();
		Map<Integer, Integer> prev = new HashMap<>();
		List<Integer> fringe = new ArrayList<>();
		
		int progenCulturesNum = (int) (Math.random() * 3) + 3;
		for (int i = 0; i < progenCulturesNum; i++) {
			int index;
			do {
				index = (int) (Math.random() * numPolygons);
			} while (fringe.contains(index));
			String race = StringUtil.genAlphaNumericStr(20);
			String culture = StringUtil.genAlphaNumericStr(20);
			data.put(index, new DNATileData(race, culture));
		}
		
		while (fringe.size() > 0) {
			int index = fringe.remove(0);
			if (prev.containsKey(index)) continue;
			
			if (!data.containsKey(index)) {
				int previousIndex = prev.get(index);
				DNATileData prevCul = data.get(previousIndex);
				DNATileData cloneCul = new DNATileData(prevCul.race, prevCul.culture);
				if (Math.random() < 0.5) {
					cloneCul.culture = StringUtil.mutateAlphaNumStr(cloneCul.culture);
				}
				if (Math.random() < 0.15) {
					cloneCul.race = StringUtil.mutateAlphaNumStr(cloneCul.race);
				}
				data.put(previousIndex, cloneCul);
			}
			
			Set<Integer> neighbors = polygonNeighborMap.get(index);
			for (Integer neighbor: neighbors) {
				prev.put(index, neighbor);
				fringe.add(neighbor);
			}
		}
		
		return data;
	}

	public static class DNATileData {
		public String race;
		public String culture;
		
		public DNATileData(String race, String culture) {
			this.race = race;
			this.culture = culture;
		}
	}
	
}
