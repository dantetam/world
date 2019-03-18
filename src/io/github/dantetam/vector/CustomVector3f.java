package io.github.dantetam.vector;

import org.lwjgl.util.vector.Vector;
import org.lwjgl.util.vector.Vector3f;

/**
 * Created by Dante on 6/29/2016.
 * Some helper classes to conveniently wrap two and three floats
 */
public class CustomVector3f extends Vector3f {

	public CustomVector3f(float a) {
		super(a,a,a);
	}
	
    public CustomVector3f(float a, float b, float c) {
        super(a,b,c);
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof CustomVector3f)) return false;
        CustomVector3f v = (CustomVector3f) obj;
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

    public float dist(CustomVector3f v) {
        return (float) Math.sqrt(Math.pow(x - v.x, 2) + Math.pow(y - v.y, 2) + Math.pow(z - v.z, 2));
    }

    public CustomVector3f getScaled(float f) {
        CustomVector3f result = new CustomVector3f(x, y, z);
        result.scale(f);
        return result;
    }

    public float magnitude() {
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    public CustomVector3f normalized() {
        float m = magnitude();
        return new CustomVector3f(x / m, y / m, z / m);
    }

    public static CustomVector3f sum(CustomVector3f v1, CustomVector3f v2) {
    	return new CustomVector3f(v1.x + v2.x, v1.y + v2.y, v1.z + v2.z);
    }
    
}
