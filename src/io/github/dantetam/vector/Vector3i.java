package io.github.dantetam.vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.lwjgl.util.vector.Vector3f;

import io.github.dantetam.toolbox.Pair;
import io.github.dantetam.toolbox.VecGridUtil;
import io.github.dantetam.toolbox.log.CustomLog;
import io.github.dantetam.world.grid.GridRectInterval;
import io.github.dantetam.world.grid.LocalGrid;
import kdtreegeo.KdPoint;

/**
 * Conveniently wrap two three integers
 * 
 * TODO: Make this class 'parameter reflexive', i.e. we could do
 * 
 * Vector3i vec = new Vector3i(x,y,z);
 * vec.get('xz') -> Pair(x, z)
 * 
 * This would reduce the code clutter around dimension-generic algorithms
 * 
 * //Syntax sugar for get any dimension programmatically?
 */

public class Vector3i extends KdPoint {

	public int x,y,z;
	
	public Vector3i(int a) {
		super(a,a,a);
		x = a; y = a; z = a;
	}

	public Vector3i(int a, int b, int c) {
		super(a,b,c);
		x = a; y = b; z = c;
	}

	public boolean equals(Object obj) {
		if (!(obj instanceof Vector3i))
			return false;
		Vector3i v = (Vector3i) obj;
		return x == v.x && y == v.y && z == v.z;
	}

	public int hashCode() {
		int hash = 17;
		hash = hash * 31 + (int) (x * 127);
		hash = hash * 31 + (int) (y * 127);
		hash = hash * 31 + (int) (z * 127);
		return hash;
	}

	public String toString() {
		return x + " " + y + " " + z;
	}

	public float dist(Vector3i v) {
		return (float) Math.sqrt(Math.pow(x - v.x, 2) + Math.pow(y - v.y, 2) + Math.pow(z - v.z, 2));
	}
	
	public int manhattanDist(Vector3i v) {
		return Math.abs(x - v.x) + Math.abs(y - v.y) + Math.abs(z - v.z);
	}
	
	public int squareDist(Vector3i v) {
		return Math.max(Math.abs(z - v.z), Math.max(Math.abs(x - v.x), Math.abs(y - v.y)));
	}

	public boolean areAdjacent14(Vector3i v) {
		return this.equals(v) ||
				LocalGrid.allAdjOffsets14.contains(this.getSubtractedBy(v)) ||
				LocalGrid.allAdjOffsets14.contains(v.getSubtractedBy(this));
	}
	
	public Vector3f getScaled(float f) {
		return new Vector3f(x * f, y * f, z * f);
	}

	public Vector3i getSum(int a, int b, int c) {
		return new Vector3i(x + a, y + b, z + c);
	}
	
	public Vector3i getSum(Vector3i other) {
		return new Vector3i(x + other.x, y + other.y, z + other.z);
	}
	
	public Vector3i getSubtractedBy(Vector3i other) {
		return new Vector3i(x - other.x, y - other.y, z - other.z);
	}
	
	public float magnitude() {
		return (float) Math.sqrt(x * x + y * y + z * z);
	}

	public Vector3f normalized() {
		float m = magnitude();
		return new Vector3f(x / m, y / m, z / m);
	}

	public static Vector3i sum(Vector3i v1, Vector3i v2) {
		return new Vector3i(v1.x + v2.x, v1.y + v2.y, v1.z + v2.z);
	}
	
	public Vector3i clone() {
		return new Vector3i(x, y, z);
	}
	
	public Vector3i x(int a) {
		x = a;
		return this;
	}
	public Vector3i y(int a) {
		y = a;
		return this;
	}
	public Vector3i z(int a) {
		z = a;
		return this;
	}
	
	public Vector2i getXY() {
		return new Vector2i(x, y);
	}
	
	public static Iterator<Vector3i> getRange(Vector3i v1, Vector3i v2) {
		final Vector3i minVec = new Vector3i(Math.min(v1.x, v2.x), Math.min(v1.y, v2.y), Math.min(v1.z, v2.z));
		final Vector3i maxVec = new Vector3i(Math.max(v1.x, v2.x), Math.max(v1.y, v2.y), Math.max(v1.z, v2.z));
		
		return new Iterator<Vector3i>() {
			private final Vector3i min = minVec, max = maxVec;
			private Vector3i currentPointer = min.clone();
			
			@Override
			public boolean hasNext() {
				return currentPointer != null;
			}
	
			@Override
			public Vector3i next() {
				if (currentPointer == null) return null;
				Vector3i pointerToReturn = currentPointer.clone();
				currentPointer.x++;
				if (currentPointer.x > max.x) {
					currentPointer.x = min.x;
					currentPointer.y++;
					if (currentPointer.y > max.y) {
						currentPointer.x = min.x;
						currentPointer.y = min.y;
						currentPointer.z++;
						if (currentPointer.z > max.z) {
							currentPointer = null;
						}
					}
				}
				return pointerToReturn;
			}
		};
	}
	
	/**
	 * Convert between these units of vector measure
	 * @param interval
	 * @return
	 */
	public static Iterator<Vector3i> getRange(GridRectInterval interval) {
		return interval.getRange();
	}
	
	/**
	 * Convert between these units of vector measure
	 * @param intervals
	 * @return
	 */
	public static Iterator<Vector3i> getRange(List<GridRectInterval> intervals) {
		if (intervals == null || intervals.size() == 0) {
			throw new IllegalArgumentException("Cannot find a vector3i iterator over a list of null or size 0");
		}
		
		List<Iterator<Vector3i>> iterators = new ArrayList<>();
		for (GridRectInterval interval: intervals) {
			iterators.add(Vector3i.getRange(interval));
		}
		
		return new Iterator<Vector3i>() {
			private int index = 0;
			private List<Iterator<Vector3i>> iteratorsRemaining = iterators;
			
			@Override
			public boolean hasNext() {
				return !(index >= iteratorsRemaining.size() - 1 && !iterators.get(index).hasNext());
			}
	
			@Override
			public Vector3i next() {
				Iterator<Vector3i> curIter = iterators.get(index);
				if (curIter.hasNext()) {
					return curIter.next();
				}
				else {
					index++;
					if (index >= iteratorsRemaining.size()) return null;
					curIter = iterators.get(index);
					return curIter.next();
				}
			}
		};
	}
	
	public static Set<Vector3i> getAllBoundingBoxCorners(Set<Vector3i> vecs) {
		Set<Vector3i> results = new HashSet<>();
		Pair<Vector3i> bounds = VecGridUtil.findCoordBounds(vecs);
		Vector3i min = bounds.first, max = bounds.second;
		results.add(min); results.add(max);
		results.add(min.x(max.x));
		results.add(min.y(max.y));
		results.add(min.z(max.z));
		results.add(min.x(max.x).y(max.y));
		results.add(min.y(max.y).z(max.z));
		results.add(min.z(max.z).x(max.x));
		return results;
	}
	
	public static Collection<Vector3i> mapCollectionVec(Collection<Vector3i> collection, 
			Function<Vector3i, Vector3i> func) {
		Collection<Vector3i> results = new ArrayList<>();
		for (Vector3i vec: collection) {
			Vector3i result = func.apply(vec);
			if (result != null)
				results.add(result);
		}
		return results;
	}

	public static Set<Vector3i> uniqueMapCollectionVec(Collection<Vector3i> collection, 
			Function<Vector3i, Vector3i> func) {
		Set<Vector3i> results = new HashSet<>();
		for (Vector3i vec: collection) {
			Vector3i result = func.apply(vec);
			if (result != null)
				results.add(result);
		}
		return results;
	}
	public static Set<Vector3i> uniqueMapCollectionVec(Iterator<Vector3i> collection, 
			Function<Vector3i, Vector3i> func) {
		Set<Vector3i> results = new HashSet<>();
		while (collection.hasNext()) {
			Vector3i result = func.apply(collection.next());
			if (result != null)
				results.add(result);
		}
		return results;
	}
	
	public static void main(String[] args) {
		GridRectInterval interval = new GridRectInterval(new Vector3i(3), new Vector3i(4));
		GridRectInterval interval2 = new GridRectInterval(new Vector3i(4), new Vector3i(5));
		Iterator<Vector3i> iter = interval.getRange();
		while (iter.hasNext()) {
			CustomLog.errPrintln(iter.next());
		}
		CustomLog.errPrintln("----------------");
		
		Iterator<Vector3i> combineIter = Vector3i.getRange(new ArrayList<GridRectInterval>() {{
			add(interval); add(interval2);
		}});
		while (combineIter.hasNext()) {
			CustomLog.errPrintln(combineIter.next());
		}
	}
	
}
