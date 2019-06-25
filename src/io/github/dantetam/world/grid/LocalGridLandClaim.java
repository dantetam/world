package io.github.dantetam.world.grid;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.lwjgl.util.vector.Vector3f;

import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.life.Human;
import kdtreegeo.KdPoint;

public class LocalGridLandClaim {

	public Human claimant;
	public GridRectInterval boundary;
	private Vector3f averageVec;
	
	public LocalGridLandClaim(Human human, Vector3i s, Vector3i e) {
		this.claimant = human;
		averageVec = s.getSum(e).getScaled(0.5f);
		boundary = new GridRectInterval(s, e);
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((boundary == null) ? 0 : boundary.hashCode());
		result = prime * result + ((claimant == null) ? 0 : claimant.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LocalGridLandClaim other = (LocalGridLandClaim) obj;
		if (boundary == null) {
			if (other.boundary != null)
				return false;
		} else if (!boundary.equals(other.boundary))
			return false;
		if (claimant == null) {
			if (other.claimant != null)
				return false;
		} else if (!claimant.equals(other.claimant))
			return false;
		return true;
	}
	
}
