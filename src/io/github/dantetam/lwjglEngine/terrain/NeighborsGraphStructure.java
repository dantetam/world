package io.github.dantetam.lwjglEngine.terrain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import kn.uni.voronoitreemap.gui.JSite;
import kn.uni.voronoitreemap.j2d.Point2D;
import kn.uni.voronoitreemap.j2d.PolygonSimple;

/**
 * 
 * @author Dante Take a Voronoi power diagram (graph) from the library, and then
 *         convert it into a usable graph structure, where neighboring Voronoi
 *         polygons are linked by an edge.
 *
 */

public class NeighborsGraphStructure {

	public static Object[] computePolygonalNeighbors(List<JSite> jSites) {
		Map<Integer, Set<Integer>> polygonNeighborMap = new HashMap<>();
		Map<Edge, List<Integer>> sharedEdgesMap = new HashMap<>();

		// Fill sharedEdgesMap with all the polygons that share a particular edge
		for (int polygonIndex = 0; polygonIndex < jSites.size(); polygonIndex++) {
			JSite jsite = jSites.get(polygonIndex);
			PolygonSimple polygon = jsite.getSite().getPolygon();
			double[] xPoints = polygon.getXpointsClosed(), yPoints = polygon.getYpointsClosed();
			for (int i = 0; i < polygon.length; i++) {
				Point2D first = new Point2D(xPoints[i], yPoints[i]);
				Point2D second = new Point2D(xPoints[i + 1], yPoints[i + 1]);
				Edge edge = new Edge(first, second);
				first = second;
				if (!sharedEdgesMap.containsKey(edge)) {
					sharedEdgesMap.put(edge, new ArrayList<>());
				}
				sharedEdgesMap.get(edge).add(polygonIndex); // Edge (a,b) is used by the polygon with this index
			}
		}

		// Record that polygons share their neighbors i.e.
		// if polygons x and y share an edge, then x -> y, ... and y -> x, ... within
		// polygonNeighborMap
		// In a Voronoi diagram, polygons neighboring each other share exactly one edge,
		// the perpendicular bisector of the segment between their Voronoi centroids.
		for (Entry<Edge, List<Integer>> entry : sharedEdgesMap.entrySet()) {
			List<Integer> nearPolygonIndices = entry.getValue();
			if (nearPolygonIndices.size() == 2) {
				Integer p0 = nearPolygonIndices.get(0), p1 = nearPolygonIndices.get(1);
				if (!polygonNeighborMap.containsKey(p0))
					polygonNeighborMap.put(p0, new HashSet<>());
				if (!polygonNeighborMap.containsKey(p1))
					polygonNeighborMap.put(p1, new HashSet<>());
				polygonNeighborMap.get(p0).add(p1);
				polygonNeighborMap.get(p1).add(p0);
			}
		}

		return new Object[] { polygonNeighborMap, sharedEdgesMap };
	}

	public static void main(String[] args) {
		Map<Edge, List<Integer>> sharedEdgesMap = new HashMap<>();
		Edge a = new Edge(new Point2D(0, 0), new Point2D(50, 25));
		Edge b = new Edge(new Point2D(50, 25), new Point2D(0, 0));
		sharedEdgesMap.put(a, new ArrayList<Integer>());
		System.out.println(sharedEdgesMap.containsKey(b));
	}

	public static class Edge {
		public Point2D a, b;

		public Edge(Point2D a, Point2D b) {
			this.a = a;
			this.b = b;
		}

		@Override
		public int hashCode() {
			return (int) ((a.x * a.y * 1000) + (b.x * b.y * 1000)) % 98317;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Edge)) {
				return false;
			}
			Edge edge = (Edge) obj;
			return (a.x == edge.a.x && a.y == edge.a.y && b.x == edge.b.x && b.y == edge.b.y)
					|| (a.x == edge.b.x && a.y == edge.b.y && b.x == edge.a.x && b.y == edge.a.y);
		}

		@Override
		public String toString() {
			return "Edge: " + a.toString() + "; " + b.toString();
		}
	}

}
