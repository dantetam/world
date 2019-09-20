package io.github.dantetam.world.process.priority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.civilization.gridstructure.AnnotatedRoom;
import io.github.dantetam.world.life.LivingEntity;

public class PlaceItemAnnoRoomPriority extends Priority {

	public AnnotatedRoom room;
	public Map<String, Integer> chosenNeedsMap;
	
	public PlaceItemAnnoRoomPriority(AnnotatedRoom room, Map<String, Integer> chosenNeedsMap) {
		// TODO Auto-generated constructor stub
		super(null);
		this.room = room;
		this.chosenNeedsMap = chosenNeedsMap;
	}

}
