package io.github.dantetam.world.grid;

import java.util.List;

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
	
	public Vector3i start;
	public Vector3i end;
	
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
	}
	
	public boolean vecInBounds(Vector3i coords) {
		return VecGridUtil.vecInBounds(this.start, this.end, coords);
	}
	
	public boolean intersectsInterval(GridRectInterval interval) {
		return (this.start.x <= interval.end.x && this.end.x >= interval.start.x) &&
		         (this.start.y <= interval.end.y && this.end.y >= interval.start.y) &&
		         (this.start.z <= interval.end.z && this.end.z >= interval.start.z);
	}
	
	public int get2dSize() {
		int x = end.x - start.x;
		int y = end.y - start.y;
		return x * y;
	}
	
	public Vector3i avgVec() {
		Vector3f avg = this.start.getSum(this.end).getScaled(0.5f);
		return new Vector3i((int) avg.x, (int) avg.y, (int) avg.z);
	}
	
	public List<Vector3i> getRange() {
		return Vector3i.getRange(start, end);
	}
	
	public String toString() {
		return "Grid Interval: " + start + " <-> " + end;
	}
	
}