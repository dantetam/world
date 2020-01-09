package io.github.dantetam.toolbox;

import java.util.Arrays;
import java.util.Set;
import java.util.Stack;

import io.github.dantetam.vector.Vector2i;
import io.github.dantetam.vector.Vector3i;

public class VecGridMaxArea {

	private enum VecGridFillAlg {
		MAXIMUM, CLOSEST
	}
	
	/**
	 * Histogram algorithm for 2d dynamic programming of optimal and maximal rectangular spaces
	 * 
	 * @param hist
	 * @return the 1d index of the largest rectangle (inclusive), and its width and height
	 */
	public static Triplet<Integer> maxRectAreaInHist(int[] hist) {
	    final Stack<Integer> s = new Stack<>();

	    int maxArea = 0;
	    Integer indexStart = 0, width = 0, height = 0;
	    
	    int tp, areaWithTop;

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
	                indexStart = tp;
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
                indexStart = tp;
                width = w;
                height = hist[tp];
            }
	    }

	    Triplet<Integer> rectangle = new Triplet<Integer>(indexStart, width, height);
	    return rectangle;
	}
	
	/**
	 * Histogram algorithm for 2d dynamic programming of optimal and maximal rectangular spaces
	 * 
	 * @param hist
	 * @return the 1d index of the largest rectangle (inclusive), and its width and height
	 */
	public static Triplet<Integer> closestRectAreaInHist(int[] hist, int desiredR, int desiredC) {
	    final Stack<Integer> s = new Stack<>();

	    Integer indexStart = 0, width = 0, height = 0;
	    Double bestDiff = null;
	    
	    int tp;

	    int i = 0;
	    while (i < hist.length) {
	        if (s.empty() || hist[s.peek()] <= hist[i]) {
	            s.push(i++);
	        } else {
	            tp = s.pop();
	            int w = s.empty() ? i : i - s.peek() - 1;

	            int dr = Math.max(0, desiredR - w), dc = Math.max(0, desiredC - hist[tp]);
	            double diff = Math.pow(dr, 2) + Math.pow(dc, 2);
	            
	            if (bestDiff == null || diff < bestDiff) {
	            	bestDiff = diff;
	                indexStart = tp;
	                width = Math.min(desiredR, w);
	                height = Math.min(desiredC, hist[tp]);
	            }
	        }
	    }

	    while (!s.empty()) {
	        tp = s.pop();
	        int w = s.empty() ? i : i - s.peek() - 1;
	        
	        int dr = Math.max(0, desiredR - w), dc = Math.max(0, desiredC - hist[tp]);
            double diff = Math.pow(dr, 2) + Math.pow(dc, 2);
	        
	        if (bestDiff == null || diff < bestDiff) {
	        	bestDiff = diff;
                indexStart = tp;
                width = Math.min(desiredR, w);
                height = Math.min(desiredC, hist[tp]);
            }
	    }

	    Triplet<Integer> rectangle = new Triplet<Integer>(indexStart, width, height);
	    return rectangle;
	}
	
	public static Pair<Vector2i> findBestRect(int[][] convertedOffsetVec, int desiredR, int desiredC, VecGridFillAlg alg) {
		int rows = convertedOffsetVec.length, cols = convertedOffsetVec[0].length;
		
		int[] histogramData = new int[cols];
		
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
			else {
				for (int c = 0; c < cols; c++) {
					histogramData[c] = convertedOffsetVec[0][c];
				}
			}
			
			Triplet<Integer> rectangle = null;
			if (alg == VecGridFillAlg.MAXIMUM) rectangle = maxRectAreaInHist(histogramData);
			else if (alg == VecGridFillAlg.CLOSEST) rectangle = closestRectAreaInHist(histogramData, desiredR, desiredC);
			
			int width = rectangle.second, height = rectangle.third;
			if ((width * height >= maxArea && width >= desiredC && height >= desiredR) || bestRectangle == null) {
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
	
	/**
	 * 
	 * @param coords
	 * @param desiredR
	 * @param desiredC
	 * @return A pair of Vector2i, representing the inclusive top-left corner of the rectangle, 
	 * 		and the dimensions of it.
	 */
	public static Pair<Vector2i> findMaxSubRect(Set<Vector3i> coords, int desiredR, int desiredC) {
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
		
		return findBestRect(convertedOffsetVec, desiredR, desiredC, VecGridFillAlg.MAXIMUM);
	}
	
	/**
	 * 
	 */
	public static Pair<Vector2i> findMaxSubRect(int M[][]) { 
		return findBestRect(M, -1, -1, VecGridFillAlg.MAXIMUM);
	}
	
	/**
	 * @param M 2D boolean array
	 * @return The smallest rectangle greater than dimensions (targetR, targetC) containing all true, 
	 * at top-left starting location (r,c) and sizes (rows, cols)
	 */
	public static Pair<Vector2i> findClosestSubRect(int M[][], int desiredR, int desiredC) { 
		return findBestRect(M, desiredR, desiredC, VecGridFillAlg.CLOSEST);
	}
	
	public static void main(String[] args) {
		int[][] data =
		{
			{1,0,1,0,0},
			{1,0,1,1,1},
			{1,1,1,1,1},
			{1,0,0,1,0}
		};
		
		int[] histo = {2,3,1,4,5,4,2};
		Triplet<Integer> rectData = VecGridMaxArea.maxRectAreaInHist(histo);
		System.out.println(rectData);
		
		Pair<Vector2i> maxRect = findBestRect(data,3,2,VecGridFillAlg.MAXIMUM);
		System.out.println(maxRect);
	}

}
