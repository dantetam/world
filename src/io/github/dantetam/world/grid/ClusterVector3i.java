package io.github.dantetam.world.grid;

import java.util.Set;

import io.github.dantetam.vector.Vector3i;
import kdtreegeo.KdPoint;

/**
 * 
 * Represent a set of grouped together vectors
 * @author Dante
 *
 */

public class ClusterVector3i extends KdPoint {

	public Vector3i center; //The representation of this cluster
	public Set<Vector3i> clusterData;
	
	public ClusterVector3i(Vector3i center, Set<Vector3i> clusterData) {
		super(center.x, center.y, center.z);
		this.center = center;
		this.clusterData = clusterData;
	}
	
	@Override
	public int hashCode() {
		return this.center.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ClusterVector3i other = (ClusterVector3i) obj;
		if (clusterData == null) {
			if (other.clusterData != null)
				return false;
		} else if (!clusterData.equals(other.clusterData))
			return false;
		return true;
	}
	
}
