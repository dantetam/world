package io.github.dantetam.toolbox;

import java.util.Set;
import java.util.Stack;

import io.github.dantetam.vector.Vector2i;
import io.github.dantetam.vector.Vector3i;

public class VecGridMaxArea {

	//Histogram algorithm for 2d dynamic programming of optimal and maximal rectangular spaces
	//Return the 1d index of the largest rectangle, and its width and height
	public static Triplet<Integer> maxRectAreaInHist(int[] hist) {
	    final Stack<Integer> s = new Stack<>();

	    int maxArea = 0;
	    Integer indexStart = 0, width = 0, height = 0;
	    
	    int tp;
	    int areaWithTop;

	    int i = 0;
	    while (i < hist.length) {
	        if (s.empty() || hist[s.peek()] <= hist[i]) {
	            s.push(i++);
	        } else {
	            tp = s.pop();
	            int w = s.empty() ? i : i - s.peek() - 1;
	            areaWithTop = hist[tp] * w;

	            if (maxArea < areaWithTop) {
	                maxArea = areaWithTop;
	                indexStart = i;
	                width = w;
	                height = hist[tp];
	            }
	        }
	    }

	    while (!s.empty()) {
	        tp = s.pop();
	        int w = s.empty() ? i : i - s.peek() - 1;
	        areaWithTop = hist[tp] * w;

	        if (maxArea < areaWithTop) {
                maxArea = areaWithTop;
                indexStart = i;
                width = w;
                height = hist[tp];
            }
	    }

	    Triplet<Integer> rectangle = new Triplet<Integer>(indexStart, width, height);
	    return rectangle;
	}
	
	/**
	 * 
	 * @param coords
	 * @param desiredR
	 * @param desiredC
	 * @return A pair of Vector2i, representing the inclusive top-left corner of the rectangle, 
	 * 		and the dimensions of it.
	 */
	public static Pair<Vector2i> findBestRectNew(Set<Vector3i> coords, int desiredR, int desiredC) {
		if (coords.size() < desiredR * desiredC) return null;
		
		Pair<Vector3i> bounds = VecGridUtil.findCoordBounds(coords);
		Vector3i topLeftBound = bounds.first, bottomRightBound = bounds.second;
		int rows = bottomRightBound.x - topLeftBound.x + 1;
		int cols = bottomRightBound.y - topLeftBound.y + 1;
		
		if (rows < desiredR || cols < desiredC) return null;
		
		int[][] convertedOffsetVec = new int[rows][cols];
		
		for (Vector3i coord: coords) {
			int r = coord.x - topLeftBound.x;
			int c = coord.y - topLeftBound.y;
			convertedOffsetVec[r][c] = 1;
		}
		
		int[] histogramData = new int[cols];
		for (int c = 0; c < cols; c++) {
			histogramData[c] = convertedOffsetVec[0][c];
		}
		
		int maxArea = 0;
		Triplet<Integer> bestRectangle = null;
		Vector2i topLeftCorner = null;
		
		for (int r = 0; r < rows; r++) {
			if (r != 0) { //First row initialized by a trivial base case histogram (one row)
				//For the next rows, keep tracking of continuous filled entries
				for (int c = 0; c < cols; c++) {
					histogramData[c] = convertedOffsetVec[r][c] == 1 ? histogramData[c] + 1 : 0;
				}
			}
			
			Triplet<Integer> rectangle = maxRectAreaInHist(histogramData);
			int width = rectangle.second, height = rectangle.third;
			if ((width * height > maxArea && width >= desiredC && height >= desiredR) || bestRectangle == null) {
				bestRectangle = rectangle;
				maxArea = width * height;
				
				//Height goes up the rows, convert the bottom-left to top-left coord
				topLeftCorner = new Vector2i(r - bestRectangle.third, bestRectangle.first); 
			}
		}

		Vector2i dim = new Vector2i(bestRectangle.second, bestRectangle.third);
		Pair<Vector2i> results = new Pair<Vector2i>(topLeftCorner, dim);
		return results;
	}
	
	//TODO Make this method more time efficient
	//From VisualVM: findBestRect(...) -> 10883 ms
	//HashSet.contains() -> 5898 ms
	public static Pair<Vector2i> findBestRect(Set<Vector3i> coords, int desiredR, int desiredC) {
		if (coords.size() < desiredR * desiredC) return null;
		
		Pair<Vector3i> bounds = VecGridUtil.findCoordBounds(coords);
		Vector3i topLeftBound = bounds.first, bottomRightBound = bounds.second;
		int rows = bottomRightBound.x - topLeftBound.x + 1;
		int cols = bottomRightBound.y - topLeftBound.y + 1;
		
		if (rows < desiredR || cols < desiredC) return null;
		
		int[][] convertedOffsetVec = new int[rows][cols];
		
		for (Vector3i coord: coords) {
			int r = coord.x - topLeftBound.x;
			int c = coord.y - topLeftBound.y;
			convertedOffsetVec[r][c] = 1;
		}
		
		Pair<Vector2i> zeroCenteredRect = VecGridMaxArea.findClosestSubRect(convertedOffsetVec, desiredR, desiredC);
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
	 * 
	 * 
		CustomLog.outPrintln("S (col): ###########################");
		CustomLog.outPrintlnArr(S);
		CustomLog.outPrintln("T (row): ###########################");
		CustomLog.outPrintlnArr(T);
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
	            	if (S[i][j] >= targetC && T[i][j] >= targetR) {
	            		return new Pair<Vector2i>(
	                    		new Vector2i(i - T[i][j] + 1, j - S[i][j] + 1),
	                    		new Vector2i(T[i][j], S[i][j])
	                    		);
	            	}
	            }
	            else {
	                S[i][j] = 0;
	                T[i][j] = 0;
	            }
	        }  
	    }
	     
	    return null;
	}
	
	public static void main(String[] args) {
		int[][] data =
		{
			{1,0,1,0,0},
			{1,0,1,1,1},
			{1,1,1,1,1},
			{1,0,0,1,0}
		};
		
		TODO;
	}

}
