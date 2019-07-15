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
import io.github.dantetam.toolbox.CollectionUtil;
import io.github.dantetam.toolbox.MapUtil;
import io.github.dantetam.toolbox.Pair;
import io.github.dantetam.vector.Vector2i;
import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.dataparse.ItemData;
import io.github.dantetam.world.life.Human;

public class SpaceFillingAlg {

	public static Set<Vector3i> fillSpacesIntoGrid(LocalGrid grid, Vector3i center, 
			List<Vector2i> sizes, boolean sameLevel) {
		return null;
	}
	
	/**
	 * Intended for finding space within a group's own existing land claims.
	 * 
	 * @return
	 */
	public static GridRectInterval findAvailableSpaceWithinClaims(LocalGrid grid, 
			int desiredR, int desiredC, boolean sameLevel, Set<Human> validLandOwners,
			LocalTileCond tileCond) {
		GridRectInterval bestSpace = null;
		int minScore = 0;
		for (Human landOwner: validLandOwners) {
			for (LocalGridLandClaim claim: landOwner.allClaims) {
				GridRectInterval space = findAvailableSpaceExact(grid, claim.boundary.avgVec(), 
						desiredR, desiredC, sameLevel, validLandOwners, tileCond);
				int reverseScore = Math.abs(space.get2dSize() - (desiredR * desiredC)); 
				if (reverseScore < minScore || bestSpace == null) {
					bestSpace = space;
					minScore = reverseScore;
				}
			}
		}
		return bestSpace;
	}
	
	/**
	 * Intended for finding land (free or occupied by correct owners) near coords center
	 * When given no owners, it is intended to find new land claims, chunks of new unoccupied land.
	 * 
	 * @param validLandOwners  The owners of the land that can be used for finding this space
	 * 
	 * @return The maximum rectangle closest to center, with minimum dimensions (desiredR, desiredC),
	 * if one exists within maxDistFlat * trials distance (square dist) away from center.
	 */
	public static GridRectInterval findAvailableSpaceExact(LocalGrid grid, Vector3i center, 
			int desiredR, int desiredC, boolean sameLevel, 
			Set<Human> validLandOwners, LocalTileCond tileCond) {
		//int minDistFlat = 0;
		int maxDistFlat = 10;
		int trials = 0;
		
		while (true) {
			Set<Vector3i> candidates = new HashSet<>();
			for (int r = -maxDistFlat; r <= maxDistFlat; r++) {
				for (int c = -maxDistFlat; c <= maxDistFlat; c++) {
					int h = grid.findHighestGroundHeight(r, c) + 1;
					Vector3i candidate = center.getSum(new Vector3i(r,c,0));
					candidate.z = h;
					
					if (grid.inBounds(candidate) && 
							(tileCond == null || tileCond.isDesiredTile(grid, candidate))) {
						List<Human> claimants = grid.findClaimantToTile(candidate);
						if (claimants == null || claimants.size() == 0 || 
								CollectionUtil.colnsHasIntersect(claimants, validLandOwners)) {
							candidates.add(candidate);
						}
					}
				}
			}

			List<Set<Vector3i>> components = findContFreeTiles(grid, candidates);
			List<GridRectInterval> componentMaxSubRect = new ArrayList<>();
			Map<Integer, Integer> componentScore = new HashMap<>();
			
			int componentIndex = 0;
			for (Set<Vector3i> component: components) {
				
				int height = component.iterator().next().z;
				
				//int[] maxSubRect = AlgUtil.findMaxRect(component);
				Pair<Vector2i> maxSubRect = VecGridUtil.findBestRect(component, desiredR, desiredC);
				
				if (maxSubRect == null || maxSubRect.second.x < desiredR || maxSubRect.second.y < desiredC) 
					continue; 
				
				Vector3i start = new Vector3i(maxSubRect.first.x, maxSubRect.first.y, height);
				Vector3i end = new Vector3i(
						maxSubRect.first.x + maxSubRect.second.x, 
						maxSubRect.first.y + maxSubRect.second.y, 
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
			else {
				maxDistFlat += 10;
				if (trials >= 3) {
					return null;
				}
				trials++;
			}
		}
	}
	
	/**
	 * Like SpaceFillingAlg::findAvailableSpace(...) but without the center coords restriction
	 * This is used to find available space near but not always exactly at certain coords.
	 * @return
	 */
	public static GridRectInterval findAvailableSpaceNear(LocalGrid grid, Vector3i coords,
			int desiredR, int desiredC, boolean sameLevel, 
			Set<Human> validLandOwners, LocalTileCond tileCond) {
		Map<GridRectInterval, Double> bestMinScoring = new HashMap<>();
		
		Collection<ClusterVector3i> nearestFreeClusters = grid.clustersList.nearestNeighbourListSearch(
				20, new ClusterVector3i(coords, null));
		for (ClusterVector3i cluster: nearestFreeClusters) {
			GridRectInterval bestInterval = findAvailableSpaceExact(grid, cluster.center, 
					desiredR, desiredC, sameLevel, validLandOwners, tileCond);
			double util = cluster.center.dist(coords) - bestInterval.get2dSize();
			bestMinScoring.put(bestInterval, util);
		}
		bestMinScoring = MapUtil.getSortedMapByValueDesc(bestMinScoring);
		
		GridRectInterval interval = bestMinScoring.keySet().iterator().next();
		return interval;
	}
	
	
	public static List<Set<Vector3i>> findContFreeTiles(
			LocalGrid grid, Set<Vector3i> candidates) {
		List<Set<Vector3i>> connectedComponents = new ArrayList<>();
		Set<Vector3i> visitedSet = new HashSet<>();
		for (Vector3i startVec : candidates) {
			if (visitedSet.contains(startVec)) continue;
			Set<Vector3i> singleComponent = new HashSet<>();
			Set<Vector3i> fringe = new HashSet<Vector3i>() {{add(startVec);}};
			while (fringe.size() > 0) {
				Set<Vector3i> newFringe = new HashSet<>();
				for (Vector3i fringeVec: fringe) {
					if (visitedSet.contains(fringeVec) || !candidates.contains(fringeVec)) continue;
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
				connectedComponents.add(singleComponent);
		}
		return connectedComponents;
	}
	
	public static List<Set<Vector3i>> allSurfaceContTiles(LocalGrid grid) {
		Set<Vector3i> candidates = new HashSet<>();
		for (int r = 0; r < grid.rows; r++) {
			for (int c = 0; c < grid.cols; c++) {
				int groundHeight = grid.findLowestEmptyHeight(r, c);
				for (int h = grid.heights - 1; h >= 0; h--) {
					LocalTile tile = grid.getTile(new Vector3i(r,c,h));
					if (h == groundHeight || (h < groundHeight && tile.exposedToAir)) {
						candidates.add(new Vector3i(r,c,h));
					}
				}
			}
		}
		return findContFreeTiles(grid, candidates);
	}
	
	/*
	private static int squareDistance(Vector3i a, Vector3i b) {
		return Math.max(Math.abs(a.x - b.x), Math.abs(a.y - b.y));
	}
	*/
	
}
