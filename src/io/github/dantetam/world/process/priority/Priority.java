package io.github.dantetam.world.process.priority;

import io.github.dantetam.toolbox.restricted.ReflectionUtil;
import io.github.dantetam.vector.Vector3i;

public abstract class Priority {

	public Vector3i coords;
	
	public Priority(Vector3i coords) {
		this.coords = coords;
	}
	
	public String toString() {
		return ReflectionUtil.getDeclaredToString(this) + '[' + coords + ']';
	}

}
