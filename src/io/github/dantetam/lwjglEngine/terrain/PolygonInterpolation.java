package io.github.dantetam.lwjglEngine.terrain;

import kn.uni.voronoitreemap.j2d.Point2D;
import kn.uni.voronoitreemap.j2d.PolygonSimple;

/**
 * Based off of the work in "Mean value coordinates for arbitrary planar polygons", Hormann, Floater, 2006.
 * Calculate barycentric coordinates (i.e. 'polygon' coordinates) for arbitrary convex polygons.
 * @author Dante
 *
 */

public class PolygonInterpolation {

	public static double[] getPolyCoords(PolygonSimple polygon, Point2D vertex, double[] fWeights) {
		int n = polygon.length;
		if (fWeights.length != n) {
			throw new IllegalArgumentException("Polygonal vertices weight array length needs to match polygon length");
		}
		Point2D[] s = new Point2D[n];
		double[] r = new double[n], A = new double[n], D = new double[n];
		double[] f = new double[n];
		for (int i = 0; i < n; i++) {
			s[i] = polygon.getPoint(i).subtract(vertex);
		}
		for (int i = 0; i < n; i++) {
			int next = (i+1) % n;
			r[i] = s[i].length();
			A[i] = s[i].determinant(s[next]) / 2;
			D[i] = s[i].dotProduct(s[next]);
			if (r[i] == 0) { //v = v_i
				f[i] = 1;
				return f;
			}
			if (A[i] == 0 && D[i] < 0) { //v lies on an edge of the polygon
				r[next] = s[next].length();  
				f[i] = (r[next] * fWeights[i]) / (r[i] + r[next]);
				f[next] = (r[i] * fWeights[next]) / (r[i] + r[next]); 
				return f;
			}
		}
		
		double W = 0;
		for (int i = 0; i < n; i++) {
			double w = 0;
			int before = i == 0 ? n - 1 : i - 1;
			int next = (i+1) % n;
			if (A[before] != 0) {
				w += (r[before] - D[before]/r[i]) / A[before];
			}
			if (A[i] != 0) {
				w += (r[next] - D[i]/r[i]) / A[i];
			}
			f[i] += w * fWeights[i];
			W += w;
		}
		for (int i = 0; i < n; i++) {
			f[i] /= W;
		}
		return f;
	}
	
	public static double[] getPolyCoordsImproved(PolygonSimple polygon, Point2D vertex, double[] fWeights) {
		int n = polygon.length;
		if (fWeights.length != n) {
			throw new IllegalArgumentException("Polygonal vertices weight array length needs to match polygon length");
		}
		Point2D[] s = new Point2D[n];
		double[] r = new double[n], A = new double[n], D = new double[n];
		double[] f = new double[n];
		for (int i = 0; i < n; i++) {
			s[i] = polygon.getPoint(i).subtract(vertex);
		}
		for (int i = 0; i < n; i++) {
			int next = (i + 1) % n;
			r[i] = s[i].length();
			A[i] = s[i].determinant(s[next]) / 2;
			D[i] = s[i].dotProduct(s[next]);
			if (r[i] == 0) { //v = v_i
				f[i] = 1;
				return f;
			}
			if (A[i] == 0 && D[i] < 0) { //v lies on an edge of the polygon
				r[next] = s[next].length();  
				f[i] = (r[next] * fWeights[i]) / (r[i] + r[next]);
				f[next] = (r[i] * fWeights[next]) / (r[i] + r[next]); 
				return f;
			}
		}
		
		double W = 0;
		for (int i = 0; i < n; i++) {
			double w = 0;
			int before = ((i - 2) % n) + 1;
			int next = (i + 1) % n;
			if (A[before] != 0) {
				w += (r[before] - D[before]/r[i]) / A[before];
			}
			if (A[i] != 0) {
				w += (r[next] - D[i]/r[i]) / A[i];
			}
			f[i] += w * fWeights[i];
			W += w;
		}
		for (int i = 0; i < n; i++) {
			f[i] /= W;
		}
		return f;
	}
	
	public static void main(String[] args) {
		PolygonSimple polyTest = new PolygonSimple(); //convex
		polyTest.setPoints(new double[] {1,2,3,6,10,25}, new double[] {3,6,10,17,21,28});
		double[] fWeights = new double[] {50, 50, 50, 100, 100, 100};
		double[] polyCoords = getPolyCoords(polyTest, new Point2D(4,8), fWeights);
		for (double coord: polyCoords) {
			System.out.print(coord + ", ");
		}
		System.out.println();
		
		fWeights = new double[] {50, 50, 50, 50, 50, 50};
		polyCoords = getPolyCoords(polyTest, new Point2D(4,8), fWeights);
		for (double coord: polyCoords) {
			System.out.print(coord + ", ");
		}
		System.out.println();
		
		fWeights = new double[] {1, 1, 1, 1, 1, 1};
		polyCoords = getPolyCoords(polyTest, new Point2D(4,8), fWeights);
		for (double coord: polyCoords) {
			System.out.print(coord + ", ");
		}
		System.out.println();
		
		fWeights = new double[] {1, 1, 1, 1, 1, 1};
		polyCoords = getPolyCoords(polyTest, new Point2D(3,15), fWeights);
		for (double coord: polyCoords) {
			System.out.print(coord + ", ");
		}
	}
	
}
