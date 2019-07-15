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
	
}
