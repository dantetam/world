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

	public static final int DNA_RACE_LEN = 20, DNA_CULTURE_LEN = 20, DNA_LANG_LEN = 20;
	
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
		
		//1 to 1 per tile correspondence
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
				data[r][c] = dnaAtPoly;
			}
		}
		return data;
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
			String race = StringUtil.genAlphaNumericStr(DNA_RACE_LEN);
			String culture = StringUtil.genAlphaNumericStr(DNA_CULTURE_LEN);
			List<String> languages = StringUtil.genAlphaNumericStrList(DNA_LANG_LEN, 3);
			data.put(index, new DNATileData(race, culture, languages));
			fringe.add(index);
		}
		
		while (fringe.size() > 0) {			
			int index = fringe.remove(0);
			
			if (!data.containsKey(index)) {
				int previousIndex = prev.get(index);
				DNATileData prevCul = data.get(previousIndex);
				
				List<String> cloneLanguages = new ArrayList<>(prevCul.languages);
				if (Math.random() < 0.05) {
					int randIndex = (int) (Math.random() * cloneLanguages.size());
					String origLang = cloneLanguages.remove(randIndex);
					int multiMutateTimes = 1;
					if (Math.random() < 0.2) { //Effectively, 1% of the time
						multiMutateTimes = (int) (Math.random() * 6) + 2;
						for (int i = 0; i < multiMutateTimes; i++) {
							origLang = StringUtil.mutateAlphaNumStr(origLang);
						}
					} 
					cloneLanguages.add(origLang);
				}
				if (Math.random() < 0.01) {
					int randIndex = (int) (Math.random() * cloneLanguages.size());
					String origLang = cloneLanguages.remove(randIndex);
					String newLang = StringUtil.genAlphaNumericStr(DNA_LANG_LEN);
					String mergedLang = StringUtil.randMergeStrs(origLang, newLang, 0.75);
					cloneLanguages.add(mergedLang);
				}
				
				DNATileData cloneCul = new DNATileData(prevCul.race, prevCul.culture,
						cloneLanguages);
				if (Math.random() < 0.5) {
					cloneCul.culture = StringUtil.mutateAlphaNumStr(cloneCul.culture);
				}
				if (Math.random() < 0.15) {
					cloneCul.race = StringUtil.mutateAlphaNumStr(cloneCul.race);
				}
				data.put(index, cloneCul);
			}
			
			Set<Integer> neighbors = polygonNeighborMap.get(index);
			for (Integer neighbor: neighbors) {
				if (prev.containsKey(neighbor)) continue;
				prev.put(neighbor, index);
				fringe.add(neighbor);
			}
		}
		
		return data;
	}

	public static class DNATileData {
		public String race;
		public String culture;
		public List<String> languages;
		
		public DNATileData(String race, String culture, List<String> languages) {
			this.race = race;
			this.culture = culture;
			this.languages = languages;
		}
		
		public String toString() {
			return this.hashCode() + "";
		}
	}
	
	public static void main(String[] args) {
		DNATileData[][] data = createGrid(new Vector2i(50,50));
		for (int r = 0; r < data.length; r++) {
			for (int c = 0; c < data[0].length; c++) {
				if (data[r][c] != null)
					System.out.print(data[r][c].languages.toString());
				System.out.print(" ");
			}
			System.out.println();
		}
	}
	
}
