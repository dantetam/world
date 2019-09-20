package io.github.dantetam.world.process.priority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.civilization.gridstructure.AnnotatedRoom;
import io.github.dantetam.world.life.LivingEntity;

public class ImproveRoomPriority extends Priority {

	public AnnotatedRoom room;
	public Set<LivingEntity> validEntitiesOwners;
	
	public ImproveRoomPriority(AnnotatedRoom room, Set<LivingEntity> validEntitiesOwners) {
		super(null);
		this.room = room;
		this.validEntitiesOwners = validEntitiesOwners;
	}

}
