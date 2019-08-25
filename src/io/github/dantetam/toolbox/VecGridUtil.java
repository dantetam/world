package io.github.dantetam.toolbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.LinkedHashSet;
import java.util.function.Function;

import io.github.dantetam.toolbox.log.CustomLog;
import io.github.dantetam.vector.Vector2i;
import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.dataparse.WorldCsvParser;
import io.github.dantetam.world.grid.ClusterVector3i;
import io.github.dantetam.world.grid.LocalGrid;
import io.github.dantetam.world.grid.LocalTile;
import io.github.dantetam.world.worldgen.LocalGridBiome;
import io.github.dantetam.world.worldgen.LocalGridInstantiate;

public class VecGridUtil {
	
	public static class VecDistComp implements Comparator<Vector3i> {
		public Vector3i target;
		public VecDistComp(Vector3i target) {
			this.target = target;
		}
		
		@Override
		public int compare(Vector3i o1, Vector3i o2) {
			// TODO Auto-generated method stub
			return o1.manhattanDist(target) - o2.manhattanDist(target);
		}
	}
	
	/**
	 * @return The minimum and maximum bounds of the set of coords in three dimensions,
	 * formatted as two Vector3i objects.
	 */
	public static Pair<Vector3i> findCoordBounds(Collection<Vector3i> coords) {
		if (coords.size() == 0) {
			throw new IllegalArgumentException("Attempted to find the bounds of an empty set of coords");
		}
		Vector3i minBounds = null, maxBounds = null;
		for (Vector3i coord: coords) {
			if (minBounds == null) {
				minBounds = coord.clone();
				maxBounds = coord.clone();
			}
			else {
				if (coord.x < minBounds.x) {
					minBounds.x = coord.x;
				}
				if (coord.y < minBounds.y) {
					minBounds.y = coord.y;
				}
				if (coord.z < minBounds.z) {
					minBounds.z = coord.z;
				}
				if (coord.x > maxBounds.x) {
					maxBounds.x = coord.x;
				}
				if (coord.y > maxBounds.y) {
					maxBounds.y = coord.y;
				}
				if (coord.z > maxBounds.z) {
					maxBounds.z = coord.z;
				}
			}
		}
		return new Pair<Vector3i>(minBounds, maxBounds);
	}
	
	/**
	 * @param coords  The available coords that a rectangle could occupy
	 * @return        The maximal rectangle within the set of vectors
	 */
	public static Pair<Vector2i> findMaxRect(Set<Vector3i> coords) {
		Pair<Vector3i> bounds = findCoordBounds(coords);
		Vector3i topLeftBound = bounds.first, bottomRightBound = bounds.second;
		int rows = bottomRightBound.x - topLeftBound.x + 1;
		int cols = bottomRightBound.z - topLeftBound.z + 1;
		int[][] convertedOffsetVec = new int[rows][cols];
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				if (coords.contains(topLeftBound.getSum(new Vector3i(r,c,0)))) {
					convertedOffsetVec[r][c] = 1;
				}
			}
		}
		Pair<Vector2i> zeroCenteredRect = findMaxSubRect(convertedOffsetVec);
		zeroCenteredRect.first.x += topLeftBound.x;
		zeroCenteredRect.first.y += topLeftBound.y;
		return zeroCenteredRect;
	}
	
	
	//TODO generalize algorithm to finding maximal rects (for land claims?)
	
	
	/**
	 * ("Breaking Path Symmetries on 4-Connected Grid Maps", Harabor and Botea, 2010)
	 * In A* pathfinding, too many nodes can be expanded due to the fact that symmetric paths
	 * with the same cost are factored into node expansion. This step is to find maximal
	 * rectangular solids, which can be streamlined connections across the perimeter.
	 * 
	 * This approach is extended to three dimensions with trivial proof. 
	 *  
	 * @return A list of the maximal rectangular solids
	 */
	public static List<RectangularSolid> findMaximalRectSolids(LocalGrid grid) {
		return findMaximalRectSolids(grid, null);
	}
	public static List<RectangularSolid> findMaximalRectSolids(LocalGrid grid, Set<Vector3i> validSetVec) {
		List<RectangularSolid> solids = new ArrayList<>();
		int[][][] solidData = new int[grid.rows][grid.cols][grid.heights];
		for (int r = 0; r < grid.rows; r++) {
			for (int c = 0; c < grid.cols; c++) {
				for (int h = 0; h < grid.heights; h++) {
					solidData[r][c][h] = -1;
				}
			}
		}
		Vector3i minBoundsInc = new Vector3i(0,0,0);
		Vector3i maxBoundsInc = new Vector3i(grid.rows, grid.cols, grid.heights);
		int compNum = 0;
		for (int r = minBoundsInc.x; r < maxBoundsInc.x; r++) {
			for (int c = minBoundsInc.y; c < maxBoundsInc.y; c++) {
				for (int h = minBoundsInc.z; h < maxBoundsInc.z; h++) {
					Vector3i minPoint = new Vector3i(r,c,h);
					if (validSetVec != null && !validSetVec.contains(minPoint)) continue;
					
					Vector3i dimensions = new Vector3i(1,1,1);
					boolean rExp = true, cExp = true, hExp = true;
					while (rExp || cExp || hExp) {
						if (rExp) {
							if (canExpand(grid, minPoint, dimensions, 'r', solidData, validSetVec)) 
								dimensions.x++;
							else rExp = false;
						}
						if (cExp) {
							if (canExpand(grid, minPoint, dimensions, 'c', solidData, validSetVec)) 
								dimensions.y++;
							else cExp = false;
						}
						if (hExp) {
							if (canExpand(grid, minPoint, dimensions, 'h', solidData, validSetVec)) 
								dimensions.z++;
							else hExp = false;
						}
					}
					int interiorNodes = Math.max(0, dimensions.x - 2) * Math.max(0, dimensions.y - 2) * Math.max(0, dimensions.z - 2);
					if (interiorNodes > 1) {
						RectangularSolid newSolid = new RectangularSolid(minPoint, dimensions);
						solids.add(newSolid);
						for (int d0 = minPoint.x; d0 < minPoint.x + dimensions.x; d0++) {
							for (int d1 = minPoint.y; d1 < minPoint.y + dimensions.y; d1++) {
								for (int d2 = minPoint.z; d2 < minPoint.z + dimensions.z; d2++) {
									solidData[d0][d1][d2] = compNum;
								}
							}
						}
						compNum++;
					}
				}
			}
		}
		return solids;
	}
	public static boolean canExpand(LocalGrid grid, Vector3i minPoint, Vector3i dimensions, char direction,
			int[][][] solidData, Set<Vector3i> validSetVec) {
		Set<Vector3i> face = new HashSet<>();
		if (direction == 'r') {
			int newDim = minPoint.x + dimensions.x;
			for (int d0 = minPoint.y; d0 < minPoint.y + dimensions.y; d0++) {
				for (int d1 = minPoint.z; d1 < minPoint.z + dimensions.z; d1++) {
					face.add(new Vector3i(newDim, d0, d1));
				}
			}
		}
		else if (direction == 'c') {
			int newDim = minPoint.y + dimensions.y;
			for (int d0 = minPoint.x; d0 < minPoint.x + dimensions.x; d0++) {
				for (int d1 = minPoint.z; d1 < minPoint.z + dimensions.z; d1++) {
					face.add(new Vector3i(d0, newDim, d1));
				}
			}
		}
		else if (direction == 'h') {
			int newDim = minPoint.z + dimensions.z;
			for (int d0 = minPoint.x; d0 < minPoint.x + dimensions.x; d0++) {
				for (int d1 = minPoint.y; d1 < minPoint.y + dimensions.y; d1++) {
					face.add(new Vector3i(d0, d1, newDim));
				}
			}
		}
		else {
			throw new IllegalArgumentException("Direction parameter must be one of 'r','c','h', got: " + direction);
		}
		for (Vector3i vec: face) {
			if (!grid.inBounds(vec) || !grid.tileIsAccessible(vec) || solidData[vec.x][vec.y][vec.z] != -1 ||
					(validSetVec != null && !validSetVec.contains(vec))
					) {
				return false;
			}
		}
		return true;
	}
	
	//Represents a single rectangular solid, for use in rectangular symmetry pathfinding 
	//See VecGridUtil::findMaximalRectSolids();
	public static class RectangularSolid {
		public Vector3i topLeftCorner; //The coordinate of the rectangular solid with the least x,y,z coords
		public Vector3i solidDimensions; //The measurements of the solid, e.g. 2x2x2 represents an object,
			//2 tiles wide in each dimension, for a total of 8 occupied tiles.
		public RectangularSolid(Vector3i corner, Vector3i dim) {
			this.topLeftCorner = corner;
			this.solidDimensions = dim;
		}
		
		/**
		 * @return True if the given point is fully within this rect solid 
		 * 		   (does not include perimeter, hence why there is offset).
		 */
		public boolean insideInterior(Vector3i v) {
			Vector3i minBounds = this.topLeftCorner.getSum(1,1,1);
			Vector3i maxBounds = this.topLeftCorner.getSum(this.solidDimensions).getSum(-2,-2,-2);
			return vecInBounds(minBounds, maxBounds, v);
		}
		
		public String toString() {
			return "RectSolid, Min-Corner: " + topLeftCorner.toString() + ", Sizes: " + solidDimensions.toString(); 
		}
	}
	
	/**
	 * @return Any vectors in the given list, that border the outside of the set.
	 * If the given list of vectors represents a building foundation, the result of this calculation
	 * represents the bounding walls of the buildings, with corners included.
	 * 
	 * This returns an ordered set for use in calculations,
	 * involving human distance travelling and priority. 
	 */
	public static LinkedHashSet<Vector3i> getBorderRegionFromCoords(Collection<Vector3i> coords) {
		Set<Vector3i> vecSet = new HashSet<>();
		for (Vector3i coord: coords) {
			vecSet.add(coord);
		}
		return getBorderRegionFromCoords(vecSet);
	}
	public static LinkedHashSet<Vector3i> getBorderRegionFromCoords(Set<Vector3i> coords) {
		LinkedHashSet<Vector3i> perimeter = new LinkedHashSet<>();
		Set<Vector3i> vecAdjOffsets = LocalGrid.allAdjOffsets8;
		for (Vector3i coord: coords) {
			for (Vector3i offset: vecAdjOffsets) {
				Vector3i neighbor = coord.getSum(offset);
				if (!coords.contains(neighbor)) {
					perimeter.add(coord);
					break;
				}
			}
		}
		return perimeter;
	}
	public static Set<Vector2i> getBorderRegionFromCoords2d(Set<Vector2i> coords) {
		Set<Vector2i> perimeter = new HashSet<>();
		Set<Vector2i> vecAdjOffsets = new HashSet<Vector2i>() {{
			add(new Vector2i(1,0)); add(new Vector2i(-1,0));
			add(new Vector2i(0,1)); add(new Vector2i(0,-1));
			add(new Vector2i(1,-1)); add(new Vector2i(-1,1));
			add(new Vector2i(1,1)); add(new Vector2i(-1,-1));
		}};
		for (Vector2i coord: coords) {
			for (Vector2i offset: vecAdjOffsets) {
				Vector2i neighbor = coord.getSum(offset);
				if (!coords.contains(neighbor)) {
					perimeter.add(coord);
					break;
				}
			}
		}
		return perimeter;
	}
	public static Set<Vector2i> getCornerRegionCoords2d(Set<Vector2i> coords) {
		Set<Vector2i> corners = new HashSet<>();
		for (Vector2i coord: coords) {
			Vector2i up = coord.getSum(new Vector2i(0,1));
			Vector2i down = coord.getSum(new Vector2i(0,-1));
			Vector2i left = coord.getSum(new Vector2i(-1,0));
			Vector2i right = coord.getSum(new Vector2i(1,0));
			
			Vector2i c1 = coord.getSum(new Vector2i(1,1));
			Vector2i c2 = coord.getSum(new Vector2i(-1,-1));
			Vector2i c3 = coord.getSum(new Vector2i(1,-1));
			Vector2i c4 = coord.getSum(new Vector2i(-1,1));
			if (!(coords.contains(up) && coords.contains(down)) && 
					!(coords.contains(left) && coords.contains(right)) && 
					!(coords.contains(c1) && coords.contains(c2)) &&
					!(coords.contains(c3) && coords.contains(c4))) {
				corners.add(coord);
			}
		}
		return corners;
	}
	
	public static Map<Vector3i, Integer> convertGroupsToMap(List<ClusterVector3i> clusters) {
		Map<Vector3i, Integer> results = new HashMap<>();
		for (int clusterNum = 0; clusterNum < clusters.size(); clusterNum++) {
			Set<Vector3i> cluster = clusters.get(clusterNum).clusterData;
			for (Vector3i vec: cluster) {
				results.put(vec, clusterNum);
			}
		}
		return results;
	}
	
	/**
	 * 
	 * @param minBoundsInc  The minimum bounds of the space to separate into components. If not given, the most minimum point in the grid.
	 * @param maxBoundsInc  The maximum bounds of the space to separate into components. If not given, the most maximum point in the grid.
	 * @param grid          The grid in question
	 * @return A mapping of every vector into a numbered connected component,
	 * 		   where all pairs of vectors in a component, 
	 * 	       are 14-traversable (a path can be created from one to the other using 14-neighbors).
	 *         See LocalGrid::getAllNeighbors14()
	 */
	public static Map<Vector3i, Integer> contComponent3dSolids(Vector3i minBoundsInc,
			Vector3i maxBoundsInc, LocalGrid grid) {
		List<ClusterVector3i> clusters = contComp3dSolidsClustersSpec(minBoundsInc, maxBoundsInc, grid);
		return convertGroupsToMap(clusters);
	}
	/**
	 * Much like VecGridUtil:contComponent3dSolids, but return a list of the cluster vectors
	 */
	public static List<ClusterVector3i> contComp3dSolidsClustersSpec(Vector3i minBoundsInc,
			Vector3i maxBoundsInc, LocalGrid grid) {
		List<ClusterVector3i> results = new ArrayList<>();
		Set<Vector3i> visited = new HashSet<>();
		
		if (minBoundsInc == null) {
			minBoundsInc = new Vector3i(0,0,0);
		}
		if (maxBoundsInc == null) {
			maxBoundsInc = new Vector3i(grid.rows - 1, grid.cols - 1, grid.heights - 1);
		}
		
		for (int r = minBoundsInc.x; r <= maxBoundsInc.x; r++) {
			for (int c = minBoundsInc.y; c <= maxBoundsInc.y; c++) {
				for (int h = minBoundsInc.z; h <= maxBoundsInc.z; h++) {
					Set<Vector3i> componentVecs = new HashSet<>();
					List<Vector3i> fringe = new ArrayList<>();
					fringe.add(new Vector3i(r,c,h));
					
					while (fringe.size() > 0) {
						Vector3i first = fringe.remove(0);
						if (!visited.contains(first) && 
								grid.inBounds(first) && vecInBounds(minBoundsInc, maxBoundsInc, first)) {
							visited.add(first);
							if (grid.tileIsAccessible(first)) {
								componentVecs.add(first);
								Set<Vector3i> neighbors = grid.getAllNeighbors14(first);
								for (Vector3i neighbor: neighbors) {
									fringe.add(neighbor);
								}
							}
						}
					}
					
					if (componentVecs.size() > 0) {
						Vector3i singleVec = componentVecs.iterator().next();
						results.add(new ClusterVector3i(singleVec, componentVecs));
					}
				}
			}
		}
		
		return results;
	}
	
	public static Vector3i getRandVecInBounds(Vector3i a, Vector3i b) {
		int minX = Math.min(a.x, b.x), minY = Math.min(a.y, b.y), minZ = Math.min(a.z, b.z);
		int maxX = Math.max(a.x, b.x), maxY = Math.max(a.y, b.y), maxZ = Math.max(a.z, b.z);
		int newX = (int) (Math.random() * (maxX - minX + 1)) + minX;
		int newY = (int) (Math.random() * (maxY - minY + 1)) + minY;
		int newZ = (int) (Math.random() * (maxZ - minZ + 1)) + minZ;
		return new Vector3i(newX, newY, newZ);
	}
	
	/**
	 * 
	 * @param minBounds,maxBounds The min and max bounds, inclusive
	 * @param coords              The coordinates in question
	 * @return True if the given vector is contained within the inclusive bounds
	 */
	public static boolean vecInBounds(Vector3i minBounds, Vector3i maxBounds, Vector3i coords) {
		List<Vector3i> pair = new ArrayList<>();
		pair.add(minBounds); pair.add(maxBounds);
		Pair<Vector3i> bounds = findCoordBounds(pair);
		Vector3i topLeftBound = bounds.first, bottomRightBound = bounds.second;
		if (minBounds != null) {
        	if (coords.x < topLeftBound.x || 
        		coords.y < topLeftBound.y ||
        		coords.z < topLeftBound.z) { //If tile is not within the inclusive bounds
        		return false;
        	}
        }
        if (maxBounds != null) {
        	if (coords.x > bottomRightBound.x || 
        		coords.y > bottomRightBound.y ||
        		coords.z > bottomRightBound.z) {
        		return false;
        	}
        }
        return true;
	}
	
	public static Pair<Vector2i> findBestRect(Set<Vector3i> coords, int desiredR, int desiredC) {
		Pair<Vector3i> bounds = findCoordBounds(coords);
		Vector3i topLeftBound = bounds.first, bottomRightBound = bounds.second;
		int rows = bottomRightBound.x - topLeftBound.x + 1;
		int cols = bottomRightBound.y - topLeftBound.y + 1;
		int[][] convertedOffsetVec = new int[rows][cols];
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				if (coords.contains(topLeftBound.getSum(new Vector3i(r,c,0)))) {
					convertedOffsetVec[r][c] = 1;
				}
			}
		}
		
		Pair<Vector2i> zeroCenteredRect = findClosestSubRect(convertedOffsetVec, desiredR, desiredC);
		if (zeroCenteredRect != null) {
			zeroCenteredRect.first.x += topLeftBound.x;
			zeroCenteredRect.first.y += topLeftBound.y;
		}
		return zeroCenteredRect;
	}
	
    /**
	 * @param M 2D boolean array
	 * @return The maximum rectangle containing all true, 
	 * at top-left starting location (r,c) and sizes (rows, cols)
	 */
	public static Pair<Vector2i> findMaxSubRect(int M[][]) { 
        int i,j; 
        int R = M.length;         //no of rows in M[][] 
        int C = M[0].length;     //no of columns in M[][] 
        int S[][] = new int[R][C];      
        int T[][] = new int[R][C];
      
        /* Set first column of S[][]*/
        for (i = 0; i < R; i++) {
            S[i][0] = M[i][0]; 
            T[i][0] = M[i][0];
        }
      
        /* Set first row of S[][]*/
        for (j = 0; j < C; j++) {
        	S[0][j] = M[0][j]; 
            T[0][j] = M[0][j]; 
        }
          
        /* Construct other entries of S[][]*/
        for (i = 1; i < R; i++) { 
            for (j = 1; j < C; j++) { 
                if (M[i][j] == 1) {
                	S[i][j] = Math.min(S[i-1][j-1], S[i][j-1]) + 1;
                	T[i][j] = Math.min(T[i-1][j-1], T[i-1][j]) + 1; 
                }
                else {
                    S[i][j] = 0;
                    T[i][j] = 0;
                }
            }  
        }
        
        int maxR = -1, maxC = -1, maxArea = 0, rows = 0, cols = 0;
        for (int r = 0; r < R; r++) {
        	for (int c = 0; c < C; c++) {
        		int area = S[r][c] * T[r][c];
        		if (area > maxArea) {
        			maxR = r;
        			maxC = c;
        			maxArea = area;
        			rows = T[r][c];
        			cols = S[r][c];
        		}
        	}
        }  
        return new Pair<Vector2i>(
        		new Vector2i(maxR - rows + 1, maxC - cols + 1),
        		new Vector2i(rows, cols)
        		);
    }  
	
	/**
	 * @param M 2D boolean array
	 * @return The smallest rectangle greater than dimensions (targetR, targetC) containing all true, 
	 * at top-left starting location (r,c) and sizes (rows, cols)
	 */
	public static Pair<Vector2i> findClosestSubRect(int M[][], int targetR, int targetC) 
    { 
        int i,j; 
        int R = M.length;         //no of rows in M[][] 
        int C = M[0].length;     //no of columns in M[][] 
        int S[][] = new int[R][C];      
        int T[][] = new int[R][C];
      
        /* Set first column of S[][]*/
        for (i = 0; i < R; i++) {
            S[i][0] = M[i][0]; 
            T[i][0] = M[i][0];
        }
      
        /* Set first row of S[][]*/
        for (j = 0; j < C; j++) {
        	S[0][j] = M[0][j]; 
            T[0][j] = M[0][j]; 
        }
          
        /* Construct other entries of S[][]*/
        for (i = 1; i < R; i++) { 
            for (j = 1; j < C; j++) { 
                if (M[i][j] == 1) {
                	S[i][j] = Math.min(S[i-1][j-1], S[i][j-1]) + 1;
                	T[i][j] = Math.min(T[i-1][j-1], T[i-1][j]) + 1; 
                }
                else {
                    S[i][j] = 0;
                    T[i][j] = 0;
                }
            }  
        }
        
        int maxR = -1, maxC = -1, curDist = 0, rows = 0, cols = 0;
        for (int r = 0; r < R; r++) {
        	for (int c = 0; c < C; c++) {
        		int imprDist = (T[r][c] - targetR) + (S[r][c] - targetC);
        		if (T[r][c] >= targetR && S[r][c] >= targetC) {
	        		if (maxR == -1 || maxC == -1 || imprDist < curDist) {
	        			maxR = r;
	        			maxC = c;
	        			curDist = imprDist;
	        			rows = T[r][c];
	        			cols = S[r][c];
	        		}
        		}
        	}
        }
         
        if (maxR == -1 || maxC == -1) return null;
        return new Pair<Vector2i>(
        		new Vector2i(maxR - rows + 1, maxC - cols + 1),
        		new Vector2i(rows, cols)
        		);
    }  
	
	public static List<Set<Vector2i>> getConnectedComponents(boolean[][] data, boolean target) {
		List<Set<Vector2i>> results = new ArrayList<>();
		boolean[][] visited = new boolean[data.length][data[0].length];
		for (int r = 0; r < data.length; r++) {
			for (int c = 0; c < data[0].length; c++) {
				if (visited[r][c]) continue;
				Set<Vector2i> component = new HashSet<>();
				List<Vector2i> fringe = new ArrayList<>();
				fringe.add(new Vector2i(r,c));
				while (fringe.size() > 0) {
					Vector2i first = fringe.remove(0);
					if (first.x < 0 || first.y < 0 || 
							first.x >= data.length || first.y >= data[0].length || 
							visited[first.x][first.y] || 
							data[first.x][first.y] != target) continue;
					visited[first.x][first.y] = true;
					component.add(first);
					fringe.add(first.getSum(new Vector2i(1, 0)));
					fringe.add(first.getSum(new Vector2i(-1, 0)));
					fringe.add(first.getSum(new Vector2i(0, 1)));
					fringe.add(first.getSum(new Vector2i(0, -1)));
					fringe.add(first.getSum(new Vector2i(1, 1)));
					fringe.add(first.getSum(new Vector2i(-1, -1)));
					fringe.add(first.getSum(new Vector2i(1, -1)));
					fringe.add(first.getSum(new Vector2i(-1, 1)));
				}
				if (component.size() > 0)
					results.add(component);
			}
		}
		return results;
	}
	
	public static Set<Vector3i> setUnderVecs(final LocalGrid grid, Collection<Vector3i> collection) {
		return Vector3i.uniqueMapCollectionVec(collection, new Function<Vector3i, Vector3i>() {

			@Override
			public Vector3i apply(Vector3i t) {
				Vector3i belowCoords = t.getSum(0, -1, 0);
				if (grid.inBounds(belowCoords))
					return belowCoords;
				return null;
			}
			
		});
	}
	
	private static void printTable(int[][] data) {
		for (int r = 0; r < data.length; r++) {
			for (int c = 0; c < data[0].length; c++) {
				System.out.print(data[r][c] + " ");
			}
			CustomLog.outPrintln(r);
		}
	}
	
	public static void main(String[] args) {
		cont3dCompTest();
	}
	
	public static void cont3dCompTest() {
		WorldCsvParser.init();
    	
    	Vector3i sizes = new Vector3i(50,50,50);
		LocalGrid activeLocalGrid = new LocalGridInstantiate(sizes, LocalGridBiome.defaultBiomeTest())
				.setupGrid(false);
		
		CustomLog.outPrintln("Start 3d component time trial now");
		long startTime = Calendar.getInstance().getTimeInMillis();
		
		Map<Vector3i, Integer> components = contComponent3dSolids(
				new Vector3i(0,0,0),
				new Vector3i(199,199,49),
				activeLocalGrid
				);
		
		long endTime = Calendar.getInstance().getTimeInMillis();
		CustomLog.outPrintln("Completed trials in " + (endTime - startTime) + "ms");
	
		//CustomLog.outPrintln(components);
	}
	
	public void matrixAndComp2DTest() {
		boolean[][] matrix = {
				{true, true, false, false, true},
				{true, false, false, false, false},
				{false, true, false, true, false},
				{false, true, true, true, true},
				{false, true, true, true, true}
		};
		
		int A[][] = { 
				{0, 1, 1, 0}, 
                {0, 1, 1, 1}, 
                {1, 1, 1, 1}, 
                {0, 1, 0, 0}, 
              }; 
		CustomLog.outPrintln(findMaxSubRect(A).toString());
		
		CustomLog.outPrintln(findClosestSubRect(A, 2, 2).toString());
		
		List<Vector3i> coords = new ArrayList<>();
		coords.add(new Vector3i(1,1,1));
		coords.add(new Vector3i(5,-1,1));
		coords.add(new Vector3i(2,1,4));
		CustomLog.outPrintln(VecGridUtil.findCoordBounds(coords).toString());
		
		
		Set<Vector2i> cornerCoordsTest = new HashSet<>();
		cornerCoordsTest.add(new Vector2i(1,1));
		cornerCoordsTest.add(new Vector2i(1,0));
		cornerCoordsTest.add(new Vector2i(1,-1));
		
		cornerCoordsTest.add(new Vector2i(-1,1));
		cornerCoordsTest.add(new Vector2i(-1,0));
		cornerCoordsTest.add(new Vector2i(-1,-1));
		
		cornerCoordsTest.add(new Vector2i(0,1));
		cornerCoordsTest.add(new Vector2i(0,-1));
		cornerCoordsTest.add(new Vector2i(0,0));
		
		Set<Vector2i> border = getBorderRegionFromCoords2d(cornerCoordsTest);
		CustomLog.outPrintln(border);
		
		Set<Vector2i> corners = VecGridUtil.getCornerRegionCoords2d(cornerCoordsTest);
		CustomLog.outPrintln(corners);
		
		CustomLog.outPrintln(VecGridUtil.getConnectedComponents(matrix, true));
	}

}
