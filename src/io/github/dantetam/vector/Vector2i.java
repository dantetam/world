package io.github.dantetam.vector;

import org.lwjgl.util.vector.Vector3f;

import kdtreegeo.KdPoint;

/**
 * Conveniently wrap two three integers
 */
public class Vector2i {

	public int x,y;
	
	public Vector2i(int a) {
		x = a; y = a;
	}

	public Vector2i(int a, int b) {
		x = a; y = b; 
	}

	public boolean equals(Object obj) {
		if (!(obj instanceof Vector2i))
			return false;
		Vector2i v = (Vector2i) obj;
		return x == v.x && y == v.y;
	}

	public int hashCode() {
		int hash = 17;
		hash = hash * 31 + (int) (x * 127);
		hash = hash * 31 + (int) (y * 127);
		return hash;
	}

	public String toString() {
		return x + " " + y;
	}

	public float dist(Vector2i v) {
		return (float) Math.sqrt(Math.pow(x - v.x, 2) + Math.pow(y - v.y, 2));
	}
	
	public int manhattanDist(Vector2i v) {
		return Math.abs(x - v.x) + Math.abs(y - v.y);
	}

	public Vector2i getSum(Vector2i other) {
		return new Vector2i(x + other.x, y + other.y);
	}
	
	public float magnitude() {
		return (float) Math.sqrt(x * x + y * y);
	}

	public static Vector2i sum(Vector2i v1, Vector2i v2) {
		return new Vector2i(v1.x + v2.x, v1.y + v2.y);
	}

}
