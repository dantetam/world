package io.github.dantetam.toolbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import io.github.dantetam.vector.Vector2i;
import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.grid.LocalGrid;

public class VecGridUtil {
	
	/**
	 * @return The minimum and maximum bounds of the set of coords in three dimensions,
	 * formatted as two Vector3i objects.
	 */
	public static Vector3i[] findCoordBounds(Collection<Vector3i> coords) {
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
		return new Vector3i[] {minBounds, maxBounds};
	}
	
	public static int[] findMaxRect(Set<Vector3i> coords) {
		Vector3i[] bounds = findCoordBounds(coords);
		Vector3i topLeftBound = bounds[0], bottomRightBound = bounds[1];
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
		int[] zeroCenteredRect = findMaxSubRect(convertedOffsetVec);
		zeroCenteredRect[0] += topLeftBound.x;
		zeroCenteredRect[1] += topLeftBound.y;
		return zeroCenteredRect;
	}
	
	/**
	 * @return Any vectors in the given list, that border the outside of the set.
	 * If the given list of vectors represents a building foundation, the result of this calculation
	 * represents the bounding walls of the buildings, with corners included.
	 * 
	 * This returns a list as a solution to create an ordered set for use in calculations,
	 * involving human distance travelling and priority. 
	 */
	public static List<Vector3i> getBorderRegionFromCoords(List<Vector3i> coords) {
		Set<Vector3i> vecSet = new HashSet<>();
		for (Vector3i coord: coords) {
			vecSet.add(coord);
		}
		return getBorderRegionFromCoords(vecSet);
	}
	public static List<Vector3i> getBorderRegionFromCoords(Set<Vector3i> coords) {
		List<Vector3i> perimeter = new ArrayList<>();
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
	
	public static Vector3i getRandVecInBounds(Vector3i a, Vector3i b) {
		int minX = Math.min(a.x, b.x), minY = Math.min(a.y, b.y), minZ = Math.min(a.z, b.z);
		int maxX = Math.max(a.x, b.x), maxY = Math.max(a.y, b.y), maxZ = Math.max(a.z, b.z);
		int newX = (int) (Math.random() * (maxX - minX + 1)) + minX;
		int newY = (int) (Math.random() * (maxY - minY + 1)) + minY;
		int newZ = (int) (Math.random() * (maxZ - minZ + 1)) + minZ;
		return new Vector3i(newX, newY, newZ);
	}
	
	public static int[] findBestRect(Set<Vector3i> coords, int desiredR, int desiredC) {
		Vector3i[] bounds = findCoordBounds(coords);
		Vector3i topLeftBound = bounds[0], bottomRightBound = bounds[1];
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
		
		int[] zeroCenteredRect = findClosestSubRect(convertedOffsetVec, desiredR, desiredC);
		for (int r = 0; r < convertedOffsetVec.length; r++) {
			for (int c = 0; c < convertedOffsetVec[0].length; c++) {
				System.out.print(convertedOffsetVec[r][c] + " ");
			}
			System.out.println();
		}
		
		if (zeroCenteredRect != null) {
			zeroCenteredRect[0] += topLeftBound.x;
			zeroCenteredRect[1] += topLeftBound.y;
		}
		return zeroCenteredRect;
	}
	
    /**
	 * @param M 2D boolean array
	 * @return The maximum rectangle containing all true, 
	 * at top-left starting location (r,c) and sizes (rows, cols)
	 */
	public static int[] findMaxSubRect(int M[][]) { 
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
        return new int[] {maxR - rows + 1, maxC - cols + 1, rows, cols};
    }  
	
	/**
	 * @param M 2D boolean array
	 * @return The smallest rectangle greater than dimensions (targetR, targetC) containing all true, 
	 * at top-left starting location (r,c) and sizes (rows, cols)
	 */
	public static int[] findClosestSubRect(int M[][], int targetR, int targetC) 
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
        return new int[] {maxR - rows + 1, maxC - cols + 1, rows, cols};
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
	
	private static void printTable(int[][] data) {
		for (int r = 0; r < data.length; r++) {
			for (int c = 0; c < data[0].length; c++) {
				System.out.print(data[r][c] + " ");
			}
			System.out.println();
		}
	}
	
	public static void main(String[] args) {
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
		System.out.println(Arrays.toString(findMaxSubRect(A)));
		
		System.out.println(Arrays.toString(findClosestSubRect(A, 2, 2)));
		
		List<Vector3i> coords = new ArrayList<>();
		coords.add(new Vector3i(1,1,1));
		coords.add(new Vector3i(5,-1,1));
		coords.add(new Vector3i(2,1,4));
		System.out.println(Arrays.toString(VecGridUtil.findCoordBounds(coords)));
		
		
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
		System.out.println(border);
		
		Set<Vector2i> corners = VecGridUtil.getCornerRegionCoords2d(cornerCoordsTest);
		System.out.println(corners);
		
		System.out.println(VecGridUtil.getConnectedComponents(matrix, true));
	}

}