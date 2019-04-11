package io.github.dantetam.world.grid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.dantetam.toolbox.AlgUtil;
import io.github.dantetam.toolbox.MathUti;
import io.github.dantetam.vector.Vector3i;

public class SpaceFillingAlgorithm {

	/**
	 * 
	 * @return
	 */
	public static Set<Vector3i> findAvailableSpace(LocalGrid grid, Vector3i center, 
			int desiredNumTiles, boolean sameLevel) {
		//int minDistFlat = 0;
		int maxDistFlat = 10;
		int trials = 0;
		
		while (true) {
			Set<Vector3i> candidates = new HashSet<>();
			for (int r = -maxDistFlat; r <= maxDistFlat; r++) {
				for (int c = -maxDistFlat; c <= maxDistFlat; c++) {
					candidates.add(center.getSum(new Vector3i(r,c,0)));
				}
			}
			List<Set<Vector3i>> components = findConnectedFreeTiles(grid, candidates);
			List<int[]> componentMaxSubRect = new ArrayList<>();
			Map<Integer, Integer> componentScore = new HashMap<>();
			
			int componentIndex = 0;
			for (Set<Vector3i> component: components) {
				int[] maxSubRect = AlgUtil.findMaxRect(component);
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
				componentScore = MathUti.getSortedMapByValueDesc(componentScore);
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
	
	public static List<Set<Vector3i>> findConnectedFreeTiles(
			LocalGrid grid, Set<Vector3i> candidates) {
		List<Set<Vector3i>> connectedComponents = new ArrayList<>();
		Set<Vector3i> visitedSet = new HashSet<>();
		for (Vector3i startVec : candidates) {
			Set<Vector3i> singleComponent = new HashSet<>();
			Set<Vector3i> fringe = new HashSet<Vector3i>() {{add(startVec);}};
			while (fringe.size() > 0) {
				Set<Vector3i> newFringe = new HashSet<>();
				for (Vector3i fringeVec: fringe) {
					if (visitedSet.contains(fringeVec) || !candidates.contains(fringeVec)) continue;
					singleComponent.add(fringeVec);
					Set<Vector3i> neighbors = grid.getAllNeighbors(fringeVec);
					for (Vector3i neighborVec: neighbors) {
						newFringe.add(neighborVec);
					}
				}
				fringe = newFringe;
			}
			connectedComponents.add(singleComponent);
		}
		return connectedComponents;
	}
	
	private static int squareDistance(Vector3i a, Vector3i b) {
		return Math.max(Math.abs(a.x - b.x), Math.abs(a.y - b.y));
	}
	
}
