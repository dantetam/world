package io.github.dantetam.lwjglEngine.terrain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import io.github.dantetam.lwjglEngine.terrain.NeighborsGraphStructure.Edge;
import kn.uni.voronoitreemap.gui.JSite;
import kn.uni.voronoitreemap.j2d.Point2D;
import kn.uni.voronoitreemap.j2d.PolygonSimple;

public class ParallelogramNoise {

	/*
	 * Return noise fixed around a line, constrained to a quadrilateral. This uses
	 * the "diamond-square" method of bending a line segment by moving one of its
	 * midpoints up or down a random amount, and then applying the same recursion to
	 * the two new line segments. The recursion stops at a base case of some desired
	 * granularity.
	 */
	public static void generateNoiseConstQuad() {
		return;
	}

	public static double[] divideLineSegment(Point2D p1, Point2D p2, double amp, double splitLimit) {
		if (splitLimit <= 0 || splitLimit > 0.5)
			throw new IllegalArgumentException("splitLimit must be within bounds 0 < s <= 0.5");

		if (p1.x > p2.x) {
			Point2D temp = p1;
			p1 = p2;
			p2 = temp;
		}
		// double slope = (p2.y - p1.y) / (p2.x - p1.x);
		double theta = Math.atan2((p2.y - p1.y), (p2.x - p1.x));
		double randomSplit = splitLimit + (Math.random() * (1 - 2 * splitLimit));
		double cutX = p1.x + (p2.x - p1.x) * randomSplit, cutY = p1.y + (p2.y - p1.y) * randomSplit;

		double extendAngle = theta + Math.PI / 2;
		if (Math.random() < 0.5)
			extendAngle -= Math.PI;
		double randomAmp = amp * (Math.random() * 0.5 + 0.6);
		double xDisp = Math.cos(extendAngle) * randomAmp;
		double yDisp = Math.sin(extendAngle) * randomAmp;

		double extendX = cutX + xDisp, extendY = cutY + yDisp;

		/*
		 * System.out.println("Took 2 points: " + p1 + " ; " + p2);
		 * System.out.println("Slope: " + slope + ", Theta: " + theta);
		 * System.out.println("(" + cutX + "," + cutY + ")");
		 * System.out.println("Angle to extend: " + extendAngle + " " +
		 * Math.toDegrees(extendAngle)); System.out.println("Split proportion: " +
		 * randomSplit); System.out.println("(" + extendX + "," + extendY + ")");
		 * System.out.println(xDisp + " " + yDisp);
		 * System.out.println("--------------");
		 */

		return new double[] { extendX, extendY, randomSplit };
	}

	/**
	 * Recursively split any chain of vertices of size n into 2n - 1, and repeat the
	 * process for each new chain
	 * 
	 * @param chainEdges
	 * @param amp
	 * @param ampReduceFactor
	 * @param splitPercentage
	 * @param cutoffSplitDist
	 * @return
	 */
	public static List<Point2D> parallelogramBoundNoise(List<Point2D> chainEdges, double amp, double ampReduceFactor,
			double splitPercentage, double cutoffSplitDist) {
		List<Point2D> newChainEdges = new ArrayList<Point2D>();
		for (int i = 0; i < chainEdges.size() - 1; i++) {
			newChainEdges.add(chainEdges.get(i));
			if (chainEdges.get(i).distance(chainEdges.get(i + 1)) >= cutoffSplitDist) {
				double[] splitSegment = divideLineSegment(chainEdges.get(i), chainEdges.get(i + 1), amp,
						splitPercentage);
				newChainEdges.add(new Point2D(splitSegment[0], splitSegment[1]));
			}
		}
		newChainEdges.add(chainEdges.get(chainEdges.size() - 1));
		if (newChainEdges.size() == chainEdges.size()) {
			return newChainEdges;
		}
		return parallelogramBoundNoise(newChainEdges, amp * ampReduceFactor, ampReduceFactor, splitPercentage,
				cutoffSplitDist);
	}

	public static List<JSite> parallelogramNoiseOnVoronoi(List<JSite> originalVoronoi, double ampDistUnits,
			double cutoffDistUnits) {
		Object[] results = NeighborsGraphStructure.computePolygonalNeighbors(originalVoronoi);
		Map<Integer, Set<Integer>> polygonNeighborMap = (Map) results[0];
		Map<Edge, List<Integer>> sharedEdgesMap = (Map) results[1];

		for (Entry<Edge, List<Integer>> entry : sharedEdgesMap.entrySet()) {
			List<Integer> polygonIndices = entry.getValue();
			if (polygonIndices.size() == 2) {
				// Create the new edge of the border between these two polygons
				Edge edge = entry.getKey();
				List<Point2D> beginningChain = Arrays.asList(new Point2D[] { edge.a, edge.b });
				double dist = edge.a.distance(edge.b);
				List<Point2D> newChain = parallelogramBoundNoise(beginningChain, dist * ampDistUnits, 0.65, 0.45,
						dist * cutoffDistUnits);

				// Update the vertex data in both polygons
				Integer p0 = polygonIndices.get(0), p1 = polygonIndices.get(1);
				PolygonSimple poly0 = originalVoronoi.get(p0).getSite().getPolygon();
				PolygonSimple poly1 = originalVoronoi.get(p1).getSite().getPolygon();
				poly0.replaceEdge(edge.a, edge.b, newChain);
				poly1.replaceEdge(edge.a, edge.b, newChain);
			}
		}

		return originalVoronoi;
	}

	/**
	 * Replace each edge of an existing polygon with a new chain of edges.
	 * 
	 * @param polygon
	 * @param ampDistUnits
	 * @param cutoffDistUnits
	 * @return
	 */
	public static PolygonSimple polygonNoiseAllEdges(PolygonSimple polygon, double ampDistUnits,
			double cutoffDistUnits) {
		List<List<Point2D>> newEdgeChains = new ArrayList<List<Point2D>>();
		int totalNewLength = 0;
		for (int polygonIndex = 0; polygonIndex < polygon.length; polygonIndex++) {
			int nextPolygonIndex = polygonIndex == polygon.length - 1 ? 0 : polygonIndex + 1;
			Edge edge = new Edge(polygon.getPoint(polygonIndex), polygon.getPoint(nextPolygonIndex));
			List<Point2D> beginningChain = Arrays.asList(new Point2D[] { edge.a, edge.b });
			double dist = edge.a.distance(edge.b);
			List<Point2D> newChain = parallelogramBoundNoise(beginningChain, dist * ampDistUnits, 0.65, 0.45,
					dist * cutoffDistUnits);
			newEdgeChains.add(newChain);
			totalNewLength += newChain.size();
		}
		totalNewLength -= polygon.length;
		double[] newXPoints = new double[totalNewLength];
		double[] newYPoints = new double[totalNewLength];
		int counter = 0;
		for (List<Point2D> chain : newEdgeChains) {
			chain.remove(0); // Do not double count these vertices, which are the points where chains join
								// together
			for (Point2D point : chain) {
				newXPoints[counter] = point.x;
				newYPoints[counter] = point.y;
				counter++;
			}
		}
		return new PolygonSimple(newXPoints, newYPoints, totalNewLength);
	}

	public static void main(String[] args) {
		List<Point2D> testPoints = new ArrayList<Point2D>();
		testPoints.add(new Point2D(0, 0));
		testPoints.add(new Point2D(800, 800));

		List<Point2D> chainPoints = parallelogramBoundNoise(testPoints, 130, 0.65, 0.5, 50);
		for (Point2D point : chainPoints) {
			System.out.println(point);
		}

	}

}
