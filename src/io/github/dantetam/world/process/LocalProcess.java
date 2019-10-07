
package io.github.dantetam.world.process;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.dantetam.toolbox.MapUtil;
import io.github.dantetam.world.civilization.SkillProcessDistribution;
import io.github.dantetam.world.civilization.SkillProcessDistribution.ProcessModKey;
import io.github.dantetam.world.civilization.SkillProcessDistribution.SkillProcessMod;
import io.github.dantetam.world.dataparse.ItemData;
import io.github.dantetam.world.dataparse.ItemTotalDrops;
import io.github.dantetam.world.grid.LocalBuilding;
import io.github.dantetam.world.grid.LocalTile;
import io.github.dantetam.world.items.Inventory;
import io.github.dantetam.world.items.InventoryItem;
import io.github.dantetam.world.life.Human;

/**
 * A process or flow which a person can take up and complete.
 * Flows have predefined steps that are advanced through an action pseudocode.
 * See the file "res/world-recipes.csv"
 * @author Dante
 *
 */

public class LocalProcess {

	public String name;
	public List<InventoryItem> inputItems;
	public ItemTotalDrops outputItems;
	public String requiredBuildNameOrGroup;
	public boolean isCreatedAtSite;
	public String requiredTileNameOrGroup;
	public List<ProcessStep> processSteps; //The linear set of actions done by humans to achieve this goal
	public List<ProcessStep> processResActions; //The set of actions that occurs when this process is finished
	
	public int recRepeats; //Recommended number of times to repeat the sequence of this process
	public int processStepIndex; //Keep track of which step the human is completing
	
	//Information for the human to store the location of where a process is done
	public LocalBuilding processBuilding;
	public LocalTile processTile; //Person should move directly to this tile
	public LocalTile targetTile; //Person should target this tile for harvest, operations, etc.
	
	//Add a skill distribution consideration here i.e.
	//a probabilistic mapping of skill levels and requirements, into the possible outcomes a process,
	//such as item quality, quantity, residual action effects strength, and so on.
	public SkillProcessDistribution skillProcDistr;
	public Map<ProcessModKey, Double> currentSkillProcOutput; //Store for the current human working on this,
		//various modifiers to the process, gained through skills, circumstances, world conditions like climate, etc.
	//TODO; //Implement all keys of currentSkillProcOutput in LocalGridTimeExecution
	
	public LocalProcess(String name, List<InventoryItem> input, ItemTotalDrops output, 
			String buildingName, boolean site, String tileFloorId, 
			List<ProcessStep> steps, List<ProcessStep> processResActions,
			int recRepeats, SkillProcessDistribution skillProcDistr) {
		this.name = name;
		this.inputItems = input;
		this.outputItems = output;
		this.requiredBuildNameOrGroup = buildingName;
		this.isCreatedAtSite = site;
		this.requiredTileNameOrGroup = tileFloorId;
		this.processSteps = steps;
		this.processResActions = processResActions;
		this.recRepeats = recRepeats;
		this.skillProcDistr = skillProcDistr;
		processStepIndex = 0;
	}
	
	//Change the values in currentSkillProcOutput to update for the human
	public void applySkillProcessDistr(Human human) {
		this.currentSkillProcOutput = new HashMap<>();
		if (this.skillProcDistr != null) {
			for (SkillProcessMod mod: this.skillProcDistr.skillProcessMods) {
				double value = mod.func.eval(human.skillBook.getSkillLevel(mod.skillName));
				MapUtil.addNumMap(this.currentSkillProcOutput, mod.processKey, value);
			}
		}
	}
	
	public ProcessStep findStepName(String stepType) {
		for (ProcessStep step: processSteps) {
			if (step.stepType.equals(stepType)) {
				return step;
			}
		}
		return null;
	}
	
	public int totalTime() {
		int sum = 0;
		for (ProcessStep step: processSteps) {
			sum += step.timeTicks;
		}
		return sum;
	}
	
	public int totalSupervisedTime() {
		int sum = 0;
		for (ProcessStep step: processSteps) {
			if (step.stepType.startsWith("S")) {
				sum += step.timeTicks;
			}
		}
		return sum;
	}
	
	public List<Integer> heurCapitalInputs() {
		List<Integer> itemIds = new ArrayList<>();
		for (InventoryItem item: inputItems) {
			itemIds.add(item.itemId);
		}
		if (requiredBuildNameOrGroup != null) {
			itemIds.addAll(ItemData.getIdsFromNameOrGroup(requiredBuildNameOrGroup)); //See error whilst inputting group
		}
		if (requiredTileNameOrGroup != null) {
			itemIds.addAll(ItemData.getIdsFromNameOrGroup(requiredTileNameOrGroup));
		}
		return itemIds;
	}
	
	public String toString() {
		String result = "Process: " + name + " (" + this.recRepeats + ")/ Input: ";
		
		result += new Inventory(inputItems).toUniqueItemsMap();
		
		result += "/ Output: ";
		if (outputItems != null) {
			for (int id: outputItems.getAllItems()) {
				result += ItemData.getNameFromId(id) + "; ";
			}
		}
		
		result += "/ Steps: ";
		for (ProcessStep step: processSteps) {
			result += step.toString() + "; ";
		}
		
		result += "/ ReqBuild: " + this.requiredBuildNameOrGroup;
		result += "/ ReqTile: " + this.requiredTileNameOrGroup;
		
		return result;
	}
	
	public boolean equals(Object other) {
		if (!(other instanceof LocalProcess)) return false;
		return ((LocalProcess) other).name.equals(this.name);
	}
	
	public int hashCode() {
		return name.hashCode() + new Inventory(inputItems).hashCode();
	}
	
	public LocalProcess clone() {
		List<ProcessStep> steps = new ArrayList<>();
		for (ProcessStep step: this.processSteps) {
			steps.add(new ProcessStep(step.stepType, step.timeTicks, step.modifier));
		}
		LocalProcess clone = new LocalProcess(this.name, this.inputItems, this.outputItems, 
				this.requiredBuildNameOrGroup, this.isCreatedAtSite, this.requiredTileNameOrGroup,
				new ArrayList<>(steps), this.processResActions, this.recRepeats, this.skillProcDistr);
		return clone;
	}
	
	public static class ProcessStep {
		public String stepType;
		public int timeTicks;
		public double modifier;
		
		public ProcessStep(String type, int time) {
			this.stepType = type;
			this.timeTicks = time;
		}
		
		public ProcessStep(String type, int time, double modifier) {
			this.stepType = type;
			this.timeTicks = time;
			this.modifier = modifier;
		}
		
		public String toString() {
			return stepType + ", " + timeTicks + ", " + modifier;
		}
	}
	
}
