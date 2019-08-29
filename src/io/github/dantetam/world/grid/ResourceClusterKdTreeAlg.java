package io.github.dantetam.world.grid;

/**
 * Efficiently insert items into a KdTree by taking only the border of a cluster of resources.
 * 
 * A cluster of resources is defined as a 26-connected group of tiles with the same tile block id,
 * in the form of a set of Vector3i.
 * 
 * A leveled border is defined
 * 
 * within any set S, gather all border tiles t, such that t must have one least one 26-neighbor that is both
 * 1) on the same level;
 * 2) and outside of S. 
 * 
 * Alternatively, a corner is used in rectangular symmetry finding to prune nodes, see
 * RSRPathfinder.java
 * 
 * @author Dante
 *
 * TODO
 * 
 * 
 *
 */

//TODO //KdTree batch insertion algorithm

public class ResourceClusterKdTreeAlg {

}
