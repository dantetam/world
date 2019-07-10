package io.github.dantetam.world.grid;

import java.util.ArrayList;
import java.util.Arrays;
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
	public static Set<Vector3i> findAvailableSpaceWithinClaims(LocalGrid grid, 
			int desiredR, int desiredC, boolean sameLevel, Set<Human> validLandOwners,
			LocalTileCond tileCond) {
		Set<Vector3i> bestSpace = null;
		int minScore = 0;
		for (Human landOwner: validLandOwners) {
			for (LocalGridLandClaim claim: landOwner.allClaims) {
				Set<Vector3i> space = findAvailableSpace(grid, claim.boundary.avgVec(), 
						desiredR, desiredC, sameLevel, validLandOwners, tileCond);
				int reverseScore = Math.abs(space.size() - (desiredR * desiredC)); 
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
	public static Set<Vector3i> findAvailableSpace(LocalGrid grid, Vector3i center, 
			int desiredR, int desiredC, boolean sameLevel, Set<Human> validLandOwners, LocalTileCond tileCond) {
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
						if (claimants.size() == 0 || CollectionUtil.colnsHasIntersect(claimants, validLandOwners)) {
							candidates.add(candidate);
						}
					}
				}
			}

			List<Set<Vector3i>> components = findContFreeTiles(grid, candidates);
			List<int[]> componentMaxSubRect = new ArrayList<>();
			Map<Integer, Integer> componentScore = new HashMap<>();
			
			int componentIndex = 0;
			for (Set<Vector3i> component: components) {
				
				//int[] maxSubRect = AlgUtil.findMaxRect(component);
				int[] maxSubRect = VecGridUtil.findBestRect(component, desiredR, desiredC);
				
				if (maxSubRect == null || maxSubRect[2] < desiredR || maxSubRect[3] < desiredC) continue; 
				
				componentMaxSubRect.add(maxSubRect);
				
				Vector3i centerRectSpace = new Vector3i(
						maxSubRect[0] + maxSubRect[2] / 2,
						maxSubRect[1] + maxSubRect[3] / 2,
						center.z
				);
				int dist = (int) Math.ceil(centerRectSpace.dist(center));
				int verticalDist = (center.z - component.iterator().next().z) * 5;
				
				int maxRectArea = maxSubRect[2] * maxSubRect[3];
				
				componentScore.put(componentIndex, maxRectArea - dist - verticalDist);
				componentIndex++;
			}
			
			if (componentScore.size() > 0) {
				componentScore = MapUtil.getSortedMapByValueDesc(componentScore);
				Integer bestSpaceIndex = (Integer) componentScore.keySet().toArray()[0];
				Set<Vector3i> bestSpace = components.get(bestSpaceIndex);
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
