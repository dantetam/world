package io.github.dantetam.world.grid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.dantetam.toolbox.VecGridUtil;
import io.github.dantetam.toolbox.log.CustomLog;
import io.github.dantetam.toolbox.CollectionUtil;
import io.github.dantetam.toolbox.MapUtil;
import io.github.dantetam.toolbox.Pair;
import io.github.dantetam.vector.Vector2i;
import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.civilization.Society;
import io.github.dantetam.world.civilization.gridstructure.AnnotatedRoom;
import io.github.dantetam.world.civilization.gridstructure.PurposeAnnotatedBuild;
import io.github.dantetam.world.life.Human;
import kdtreegeo.KdTree;

TODO;
//Reform all these methods and definitions of land claims, purpose annotated rooms, and so on
//Idea: annotated unbuilt land (home expansions?) or unannotated land in land claims for expansions

public class SpaceFillingAlg {

	/*
	public static Set<Vector3i> fillSpacesIntoGrid(LocalGrid grid, Vector3i center, 
			List<Vector2i> sizes, boolean sameLevel) {
		return null;
	}
	*/
	
	public static GridRectInterval expandAnnotatedComplex(LocalGrid grid, 
			int desiredR, int desiredC, boolean sameLevel, Society society, Set<Human> validLandOwners,
			PurposeAnnotatedBuild complex, AnnotatedRoom room) {
		return findAvailSpaceCloseFactorClaims(grid, complex.singleLocation, 
				room.desiredSize.x, room.desiredSize.y, true, 
				society, validLandOwners, false,
				false,
				null,
				complex.)
	}
	
	/**
	 * Intended for finding space within a group's own existing land claims.
	 * Option of excluding existing annotated rooms
	 * 
	 * @return The best rectangle close to center and the closest size, 
	 * with minimum dimensions (desiredR, desiredC),
	 * if one exists within maxDistFlat * trials distance (square dist) away from center.
	 */
	//TODO: ???
	
	/**
	 * Intended for finding land (free or occupied by correct owners) near coords center
	 * When given no owners, it is intended to find new land claims, chunks of new unoccupied land.
	 * 
	 * @param validLandOwners  The owners of the land that can be used for finding this space
	 * 
	 * @return The maximum rectangle closest to center, with minimum dimensions (desiredR, desiredC),
	 * if one exists within maxDistFlat * trials distance (square dist) away from center.
	 */
	public static GridRectInterval findAvailSpaceCloseFactorClaims(LocalGrid grid, Vector3i center, 
			int desiredR, int desiredC, boolean sameLevel, 
			Society society,
			Set<Human> validLandOwners, boolean allowAnnotated,
			boolean exactStrictMatch,
			LocalTileCond tileCond,
			Collection<ClusterVector3i> mustBeWithinAreas) {
		LocalTileCond originalCond = tileCond; 
		if (!allowAnnotated) { //Add to the custom tile condition to restrict annotated tiles 
			LocalTileCond tempCond = new LocalTileCond() {
				@Override
				public boolean isDesiredTile(LocalGrid grid, Vector3i coords) {
					if (validLandOwners != null) {
						for (Human human: validLandOwners) {
							for (List<PurposeAnnotatedBuild> builds: human.designatedBuildsByPurpose.values()) {
								for (PurposeAnnotatedBuild annoBuild: builds) {
									if (annoBuild.annoBuildContainsVec(coords)) {
										return false;
									}
								}
							}
						}
					}
					if (originalCond == null) 
						return true;
					return originalCond.isDesiredTile(grid, coords);
				}
			};
			
			return findAvailSpaceClose(grid, center, desiredR, desiredC, sameLevel, society,
					validLandOwners, exactStrictMatch, tempCond, mustBeWithinAreas);
		}
		else {
			return findAvailSpaceClose(grid, center, desiredR, desiredC, sameLevel, society,
					validLandOwners, exactStrictMatch, originalCond, mustBeWithinAreas);
		}
	}
	
	
	/**
	 * Like SpaceFillingAlg::findAvailableSpace(...) but without the center coords restriction
	 * This is used to find available space near but not always exactly at certain coords.
	 * @return
	 */
	//TODO: Create space filling alg like findAvailableSpaceNear, that expands from already existing annotated rooms
	//This algorithm findAvailSpaceClose is generic enough
	public static GridRectInterval findAvailSpaceClose(LocalGrid grid, Vector3i center, 
			int desiredR, int desiredC, boolean sameLevel, 
			Society society,
			Set<Human> validLandOwners,
			boolean exactStrictMatch,
			LocalTileCond tileCond,
			Collection<ClusterVector3i> mustBeWithinAreas) {
		
		Collection<ClusterVector3i> closestClusters; 
		
		if (mustBeWithinAreas != null) {
			closestClusters = new ArrayList<>(mustBeWithinAreas);
		}
		else {
			if (validLandOwners == null || validLandOwners.size() <= 0) {
				KdTree<ClusterVector3i> componentsTree = grid.clustersList2dSurfaces;
				closestClusters = componentsTree.nearestNeighbourListSearch(10, 
						new ClusterVector3i(center, null));
			}
			else {
				closestClusters = new ArrayList<>();
				for (Human human: validLandOwners) {
					for (LocalGridLandClaim claim: human.allClaims) {
						//TODO Fill closestClusters with new clusters
						closestClusters.add(new ClusterVector3i(claim.avgVec(), 
								Vector3i.getRange(claim.boundary)));
					}
				}
			}
		}
		List<GridRectInterval> componentMaxSubRect = new ArrayList<>();
		Map<Integer, Integer> componentScore = new HashMap<>();
		
		int componentIndex = 0;
		for (ClusterVector3i cluster: closestClusters) {
			int height = cluster.center.z;
			Set<Vector3i> component = cluster.clusterData;
			
			if (tileCond != null)
				component = LocalTileCond.filter(component, grid, tileCond);
			
			if (exactStrictMatch && !component.contains(center)) continue;
			
			if (society != null && validLandOwners != null)
				component = LocalTileCond.filter(component, grid, 
						new LocalTileCond.IsValidLandOwnerSoc(society, validLandOwners));
			
			if (mustBeWithinAreas != null) {
				component = LocalTileCond.filter(component, grid,
						new LocalTileCond.IsWithinIntervalsStrict(mustBeWithinAreas));
			}
			
			if (component.size() == 0) continue; //After all the filtering has been done
			
			
			//int[] maxSubRect = AlgUtil.findMaxRect(component);
			Pair<Vector2i> maxSubRect = VecGridUtil.findBestRect(component, desiredR, desiredC);
			
			if (maxSubRect == null || maxSubRect.second.x < desiredR || maxSubRect.second.y < desiredC) 
				continue; 
			
			Vector3i start = new Vector3i(maxSubRect.first.x, maxSubRect.first.y, height);
			Vector3i end = new Vector3i(
					maxSubRect.first.x + maxSubRect.second.x - 1, 
					maxSubRect.first.y + maxSubRect.second.y - 1, 
					height);
			GridRectInterval interval = new GridRectInterval(start, end);
			componentMaxSubRect.add(interval);
			
			Vector3i centerRectSpace = interval.avgVec();
			int dist = (int) Math.ceil(centerRectSpace.dist(center));
			int verticalDist = Math.abs(center.z - height) * 5;
			
			componentScore.put(componentIndex, interval.get2dSize() - dist - verticalDist);
			componentIndex++;
		}
		
		if (componentScore.size() > 0) {
			componentScore = MapUtil.getSortedMapByValueDesc(componentScore);
			Integer bestSpaceIndex = (Integer) componentScore.keySet().toArray()[0];
			GridRectInterval bestSpace = componentMaxSubRect.get(bestSpaceIndex);
			return bestSpace;
		}
		return null;
	}
		
	public static ClusterVector3i findSingleComponent(
			LocalGrid grid, Vector3i coords) {

		Set<Vector3i> visitedSet = new HashSet<>();

		Set<Vector3i> singleComponent = new HashSet<>();
		Set<Vector3i> fringe = new HashSet<Vector3i>() {{add(coords);}};
		while (fringe.size() > 0) {
			Set<Vector3i> newFringe = new HashSet<>();
			for (Vector3i fringeVec: fringe) {
				if (visitedSet.contains(fringeVec)) continue;
				if (!grid.tileIsAccessible(fringeVec)) continue;
				
				visitedSet.add(fringeVec);
				singleComponent.add(fringeVec);
				Set<Vector3i> neighbors = grid.getAllNeighbors6(fringeVec);
				for (Vector3i neighborVec: neighbors) {
					newFringe.add(neighborVec);
				}
			}
			fringe = newFringe;
		}
		if (singleComponent.size() > 0)
			return null;
		return new ClusterVector3i(singleComponent.iterator().next(), singleComponent);
	}
	
	/**
	 * @return All separate surfaces, where a surface is defined as a group of vectors
	 * 		   adjacent to each other in the r,c dimensions, and having the exact same height h.
	 */
	public static List<ClusterVector3i> allFlatSurfaceContTiles(LocalGrid grid) {
		List<ClusterVector3i> allSurfaceClusters = new ArrayList<>();
		Set<Vector3i> visitedSet = new HashSet<>();
		
		for (int r = 0; r < grid.rows; r++) {
			for (int c = 0; c < grid.cols; c++) {
				Vector3i ground = grid.findHighestAccessibleHeight(r, c);
				if (ground != null) {
					Set<Vector3i> singleComponent = new HashSet<>();
					Set<Vector3i> fringe = new HashSet<Vector3i>() {{add(ground);}};
					while (fringe.size() > 0) {
						Set<Vector3i> newFringe = new HashSet<>();
						for (Vector3i fringeVec: fringe) {
							if (visitedSet.contains(fringeVec)) continue;
							if (!grid.tileIsAccessible(fringeVec)) continue;
							
							visitedSet.add(fringeVec);
							singleComponent.add(fringeVec);
							Set<Vector3i> neighbors = grid.getAllNeighbors8(fringeVec);
							for (Vector3i neighborVec: neighbors) {
								newFringe.add(neighborVec);
							}
						}
						fringe = newFringe;
					}
					if (singleComponent.size() > 0) {
						ClusterVector3i cluster = new ClusterVector3i(singleComponent.iterator().next(), 
								singleComponent);
						allSurfaceClusters.add(cluster);
					}
				}
			}
		}
		
		return allSurfaceClusters;
	}
	
	/*
	private static int squareDistance(Vector3i a, Vector3i b) {
		return Math.max(Math.abs(a.x - b.x), Math.abs(a.y - b.y));
	}
	*/
	
}
