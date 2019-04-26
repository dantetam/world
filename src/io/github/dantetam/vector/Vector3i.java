package io.github.dantetam.vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.lwjgl.util.vector.Vector3f;

import kdtreegeo.KdPoint;

/**
 * Conveniently wrap two three integers
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

	public Vector3f getScaled(float f) {
		return new Vector3f(x * f, y * f, z * f);
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
	
	public static List<Vector3i> getRange(Vector3i v1, Vector3i v2) {
		List<Vector3i> data = new ArrayList<>();
		Vector3i minVec = new Vector3i(Math.min(v1.x, v2.x), Math.min(v1.y, v2.y), Math.min(v1.z, v2.z));
		Vector3i maxVec = new Vector3i(Math.max(v1.x, v2.x), Math.max(v1.y, v2.y), Math.max(v1.z, v2.z));
		for (int r = minVec.x; r <= maxVec.x; r++) {
			for (int c = minVec.y; c <= maxVec.y; c++) {
				for (int h = minVec.z; h <= maxVec.z; h++) {
					data.add(new Vector3i(r,c,h));
				}
			}
		}
		return data;
	} 

}
