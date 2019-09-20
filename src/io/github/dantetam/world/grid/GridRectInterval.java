package io.github.dantetam.world.grid;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.lwjgl.util.vector.Vector3f;

import io.github.dantetam.toolbox.VecGridUtil;
import io.github.dantetam.vector.Vector3i;

/**
 * 
 * Represent an inclusive 3d vector coords bound
 * 
 * @author Dante
 *
 */

public class GridRectInterval {
	
	private Vector3i start;
	private Vector3i end;
	private Vector3i avg;
	
	public GridRectInterval(Vector3i s, Vector3i e) {
		start = new Vector3i(
					Math.min(s.x, e.x),
					Math.min(s.y, e.y),
					Math.min(s.z, e.z)
				);
		end = new Vector3i(
				Math.max(s.x, e.x),
				Math.max(s.y, e.y),
				Math.max(s.z, e.z)
			);
		updateAvg();
	}
	
	private void updateAvg() {
		Vector3f avgf = start.getSum(end).getScaled(0.5f);
		avg = new Vector3i((int) avgf.x, (int) avgf.y, (int) avgf.z);
	}
	
	public Vector3i avgVec() {
		return avg;
	}
	
	public boolean vecInBounds(Vector3i coords) {
		return VecGridUtil.vecInBounds(this.start, this.end, coords);
	}
	
	public boolean intersectsInterval(GridRectInterval interval) {
		return (this.start.x <= interval.end.x && this.end.x >= interval.start.x) &&
		         (this.start.y <= interval.end.y && this.end.y >= interval.start.y) &&
		         (this.start.z <= interval.end.z && this.end.z >= interval.start.z);
	}
	
	public boolean intersectsSetVector(Set<Vector3i> vecs) {
		Set<Vector3i> boundingCorners = Vector3i.getAllBoundingBoxCorners(vecs);
		for (Vector3i corner: boundingCorners) {
			if (vecInBounds(corner)) {
				return true;
			}
		}
		return false;
	}
	
	public int get2dSize() {
		int x = end.x - start.x;
		int y = end.y - start.y;
		return x * y;
	}
	
	public Iterator<Vector3i> getRange() {
		return Vector3i.getRange(start, end);
	}
	
	public Vector3i getStart() {
		return start;
	}
	public Vector3i getEnd() {
		return end;
	}
	
	public String toString() {
		return "Grid Interval: " + start + " <-> " + end;
	}
	
}