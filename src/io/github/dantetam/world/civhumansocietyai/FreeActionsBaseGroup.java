package io.github.dantetam.world.civhumansocietyai;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * TODO: Create an extensible, idiot-proof framework for extending collections of free actions.
 * Standardize and write once this framework to
 * 
 * get all free actions,
 * determine their free action execute chanc
 * 
 * @author Dante
 */

public abstract class FreeActionsBaseGroup {

	public abstract List<Map<String, FreeAction>> getAllFreeActions();
	
	public void getFreeActions() {
		for (Map<String, FreeAction> collectionFreeAction: getAllFreeActions()) {
			for (Entry<String, FreeAction> entry: collectionFreeAction.entrySet()) {
				FreeAction freeAction = entry.getValue();
				
			}
		}
	}
	
}