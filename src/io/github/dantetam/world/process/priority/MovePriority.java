package io.github.dantetam.world.process.priority;

import java.util.List;

import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.grid.LocalTile;
import io.github.dantetam.world.items.Inventory;
import io.github.dantetam.world.items.InventoryItem;
import io.github.dantetam.world.process.prioritytask.DoneTaskPlaceholder;
import io.github.dantetam.world.process.prioritytask.Task;

public class MovePriority extends Priority {
	
	public boolean tolerateOneDist = false;
	public List<LocalTile> currentCalcPath; 
	public int pathIndex; //Represents the current location e.g. index 3 means position is path[3]
	
	public MovePriority(Vector3i coords, boolean tolerateOneDist) {
		super(coords);
		this.tolerateOneDist = tolerateOneDist;
		pathIndex = 0;
	}

	public void initPath(List<LocalTile> path) {
		this.currentCalcPath = path;
	}

}
