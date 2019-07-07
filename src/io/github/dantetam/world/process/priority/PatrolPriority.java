package io.github.dantetam.world.process.priority;

import java.util.List;

import io.github.dantetam.toolbox.CollectionUtil;
import io.github.dantetam.vector.Vector3i;

public class PatrolPriority extends Priority {

	public List<Vector3i> locations;
	
	public PatrolPriority(List<Vector3i> locations) {
		super(null);
		this.locations = CollectionUtil.getShuffledList(locations);
	}

}
