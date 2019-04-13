package io.github.dantetam.toolbox;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.Stack;

import io.github.dantetam.vector.Vector3i;

public class AlgUtil {
	
	/**
	 * @return The minimum and maximum bounds of the set of coords in three dimensions,
	 * formatted as two Vector3i objects.
	 */
	public static Vector3i[] findCoordBounds(Collection<Vector3i> coords) {
		Vector3i minBounds = null, maxBounds = null;
		for (Vector3i coord: coords) {
			if (minBounds == null) {
				minBounds = coord;
				maxBounds = coord;
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
				if (coord.x > minBounds.x) {
					maxBounds.x = coord.x;
				}
				if (coord.y > minBounds.y) {
					maxBounds.y = coord.y;
				}
				if (coord.z > minBounds.z) {
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
	 * @param M 2D boolean array
	 * @return The maximum rectangle containing all true, 
	 * at top-left starting location (r,c) and sizes (rows, cols)
	 */
	public static int[] findMaxSubRect(int M[][]) 
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
				{true, true, false, false, false},
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
	}

}
