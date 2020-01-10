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

import io.github.dantetam.toolbox.log.CustomLog;
import io.github.dantetam.toolbox.CollectionUtil;
import io.github.dantetam.toolbox.MapUtil;
import io.github.dantetam.toolbox.Pair;
import io.github.dantetam.toolbox.VecGridMaxArea;
import io.github.dantetam.vector.Vector2i;
import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.civilization.Society;
import io.github.dantetam.world.civilization.gridstructure.AnnotatedRoom;
import io.github.dantetam.world.civilization.gridstructure.PurposeAnnotatedBuild;
import io.github.dantetam.world.life.Human;
import kdtreegeo.KdTree;

//TODO;
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
			Society society, Set<Human> validLandOwners,
			PurposeAnnotatedBuild complex, AnnotatedRoom room) {
		return findAvailSpaceCloseFactorClaims(grid, complex.singleLocation, 
				room.desiredSize.x, room.desiredSize.y, true, 
				society, validLandOwners, false,
				false,
				null,
				null);
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
	 * @param validLandOwners  The owners of the land that can be used for finding this space; 
	 * 		if null, no owner checks are made.
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
			//Chain these two conditions as AND, if both exist
			LocalTileCond tempCond = new LocalTileCond() {
				@Override
				public boolean isDesiredTile(LocalGrid grid, Vector3i coords) {
					boolean foundLandClaims = false; //For diagnostics
					if (validLandOwners != null && validLandOwners.size() > 0) {
						for (Human human: validLandOwners) {
							CustomLog.errPrintln("DesignatedBuilds: " + human.designatedBuildsByPurpose);
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
		
		//Find all viable clusters (connected components) that can provide spaces
		Collection<ClusterVector3i> closestClusters; 
		if (mustBeWithinAreas != null) {
			closestClusters = new ArrayList<>(mustBeWithinAreas);
		}
		else {
			if (validLandOwners == null || validLandOwners.size() <= 0) {
				KdTree<ClusterVector3i> componentsTree = grid.clustersList2dSurfaces;
				closestClusters = componentsTree.nearestNeighbourListSearch(25, 
						new ClusterVector3i(center, new HashSet<>()));
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
		
		List<ClusterVector3i> clustersList = new ArrayList<ClusterVector3i>(closestClusters);
		Collections.sort(clustersList, 
				new Comparator<ClusterVector3i>() {
				@Override
				public int compare(ClusterVector3i o1, ClusterVector3i o2) {
					//Ascending sort in distance
					return o1.center.manhattanDist(center) - o2.center.manhattanDist(center);
				}
		});
		
		for (ClusterVector3i cluster: clustersList) {
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
			
			//After all the filtering has been done, check if the area is 
			if (component.size() == 0) continue; 
			
			Pair<Vector2i> maxSubRect = VecGridMaxArea.findMaxSubRect(component, desiredR, desiredC);
			
			if (maxSubRect == null) // || maxSubRect.second.x < desiredR || maxSubRect.second.y < desiredC) 
				continue; 
			
			Vector3i start = new Vector3i(maxSubRect.first.x, maxSubRect.first.y, height);
			Vector3i end = new Vector3i(
					maxSubRect.first.x + maxSubRect.second.x - 1, 
					maxSubRect.first.y + maxSubRect.second.y - 1, 
					height);
			GridRectInterval interval = new GridRectInterval(start, end);
			
			Vector3i centerRectSpace = interval.avgVec();
			int dist = (int) Math.ceil(centerRectSpace.dist(center));
			int verticalDist = Math.abs(center.z - height) * 5;
			
			double finalScore = interval.get2dSize() - dist - verticalDist;
			
			if (finalScore > 0) {
				return interval;
			}
		}
		return null;
	}
		
	public static ClusterVector3i findSingleComponent(
			LocalGrid grid, Vector3i coords, NeighborMode mode) {
		Set<Vector3i> visitedSet = new HashSet<>();

		Set<Vector3i> singleComponent = new HashSet<>();
		Set<Vector3i> fringe = new HashSet<Vector3i>() {{add(coords);}};
		while (fringe.size() > 0) {
			Set<Vector3i> newFringe = new HashSet<>();
			for (Vector3i fringeVec: fringe) {
				if (visitedSet.contains(fringeVec)) continue;
				if (!grid.tileIsPartAccessible(fringeVec)) continue;
				
				visitedSet.add(fringeVec);
				singleComponent.add(fringeVec);
				Set<Vector3i> neighbors = grid.getAllNeighbors6(fringeVec);
				
				if (mode == NeighborMode.ADJ_8) neighbors = grid.getAllNeighbors8(fringeVec);
				else if (mode == NeighborMode.ADJ_14) neighbors = grid.getAllNeighbors14(fringeVec);
				else if (mode == NeighborMode.ADJ_26) neighbors = grid.getAllNeighbors26(fringeVec);
				
				for (Vector3i neighborVec: neighbors) {
					newFringe.add(neighborVec);
				}
			}
			fringe = newFringe;
		}
		if (singleComponent.size() == 0)
			return null;
		return new ClusterVector3i(singleComponent.iterator().next(), singleComponent);
	}
	public static enum NeighborMode {
		ADJ_6, ADJ_8, ADJ_14, ADJ_26
	}
	
	/**
	 * Return all connected components spreading from a set of vectors.
	 * Note that all vectors reachable from the starting set, are linked to exactly one cluster 
	 */
	public static List<ClusterVector3i> findAllComponents(
			LocalGrid grid, Set<Vector3i> multipleCoords, NeighborMode mode) {
		Set<Vector3i> visitedSet = new HashSet<>();
		List<ClusterVector3i> components = new ArrayList<>();
		
		for (Vector3i coords: multipleCoords) {
			Set<Vector3i> singleComponent = new HashSet<>();
			Set<Vector3i> fringe = new HashSet<Vector3i>() {{add(coords);}};
			while (fringe.size() > 0) {
				Set<Vector3i> newFringe = new HashSet<>();
				for (Vector3i fringeVec: fringe) {
					if (visitedSet.contains(fringeVec)) continue;
					if (!grid.tileIsPartAccessible(fringeVec)) continue;
					
					visitedSet.add(fringeVec);
					singleComponent.add(fringeVec);
					Set<Vector3i> neighbors = grid.getAllNeighbors6(fringeVec);
					
					if (mode == NeighborMode.ADJ_8) neighbors = grid.getAllNeighbors8(fringeVec);
					else if (mode == NeighborMode.ADJ_14) neighbors = grid.getAllNeighbors14(fringeVec);
					else if (mode == NeighborMode.ADJ_26) neighbors = grid.getAllNeighbors26(fringeVec);
					
					for (Vector3i neighborVec: neighbors) {
						newFringe.add(neighborVec);
					}
				}
				fringe = newFringe;
			}
			if (singleComponent.size() != 0) {
				components.add(new ClusterVector3i(singleComponent.iterator().next(), singleComponent));
			}
		}
		
		return components;
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
							if (!grid.tileIsPartAccessible(fringeVec)) continue;
							
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
