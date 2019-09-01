package io.github.dantetam.world.dataparse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lwjgl.util.vector.Vector3f;

import io.github.dantetam.world.items.InventoryItem;
import io.github.dantetam.world.life.BodyPart;

//TODO: Use CSV parsing here

public class AnatomyData {

	TODO; //world-anatomy.csv
	
	public static Set<String> species = new HashSet<String>() {{
		add("Human");
		add("Cow");
	}};
	
	/*
	 * For storing where armor and weapons go, e.g. pants go on legs and swords go into 'hands', 
	 * or any part of the hand group, like hands, wooden hooks, claws, and so on.
	 */
	public static Map<Integer, Set<String>> associatedBodyParts = new HashMap<>(); 
	
	public static Set<BodyPart> initBeingNameAnatomy(String name) {
		if (name.equals("Human")) {
			return new HashSet<BodyPart>() {{
				add(new MainBodyPart("Left Arm", new Vector3f(-0.5f, 0f, 0f), 0.5, 1.0, 10, 1.0));
				add(new MainBodyPart("Right Arm", new Vector3f(0.5f, 0f, 0f), 0.5, 1.0, 10, 1.0));
				add(new MainBodyPart("Left Leg", new Vector3f(-0.2f, -0.5f, 0f), 0.5, 1.0, 20, 0.1));
				add(new MainBodyPart("Right Leg", new Vector3f(0.2f, -0.5f, 0f), 0.5, 1.0, 20, 0.1));
				add(new MainBodyPart("Head", new Vector3f(0f, 0.3f, 0f), 0.5, 1.0, 20, 0.0).chainPartInside(
						new BodyPart("Brain", new Vector3f(0f, 0.05f, 0f), 0.2, 0.3, 5, 0.0))
				);
				add(new MainBodyPart("Torso", new Vector3f(0f, 0f, 0f), 0.5, 2.0, 40, 0.1).chainPartInside(
						new BodyPart("Heart", new Vector3f(0.08f, 0.12f, 0f), 0.1, 0.3, 5, 0.0))
				);
			}};
		}
		else if (name.equals("Cow")) {
			return new HashSet<BodyPart>() {{
				add(new MainBodyPart("Left Arm", new Vector3f(-0.5f, 0f, 0f), 0.5, 1.0, 10, 1.0));
				add(new MainBodyPart("Right Arm", new Vector3f(0.5f, 0f, 0f), 0.5, 1.0, 10, 1.0));
				add(new MainBodyPart("Left Shank", new Vector3f(-0.2f, -0.5f, 0f), 0.5, 1.0, 20, 0.1));
				add(new MainBodyPart("Right Shank", new Vector3f(0.2f, -0.5f, 0f), 0.5, 1.0, 20, 0.1));
				add(new MainBodyPart("Head", new Vector3f(0f, 0.3f, 0f), 0.5, 1.0, 20, 0.0).chainPartInside(
						new BodyPart("Brain", new Vector3f(0f, 0.05f, 0f), 0.2, 0.3, 5, 0.0))
				);
				add(new MainBodyPart("Loin", new Vector3f(0f, 0f, 0f), 0.5, 2.0, 40, 0.1).chainPartInside(
						new BodyPart("Heart", new Vector3f(0.08f, 0.12f, 0f), 0.1, 0.3, 5, 0.0)
				));
				add(new MainBodyPart("Flank", new Vector3f(0f, 0f, 0f), 0.5, 2.0, 40, 0.1));
			}};
		}
		throw new IllegalArgumentException("Could not instantiate anatomy slots for missing being type: " + name);
	}
	
	public static Collection<String[]> getNeighborBodyParts(String name) {
		List<String[]> neighborPairs = new ArrayList<String[]>();
		if (name.equals("Human")) {
			neighborPairs.add(new String[] {"Torso", "Left Arm"});
			neighborPairs.add(new String[] {"Torso", "Right Arm"});
			neighborPairs.add(new String[] {"Torso", "Left Leg"});
			neighborPairs.add(new String[] {"Torso", "Right Leg"});
			neighborPairs.add(new String[] {"Torso", "Head"});
		}
		else if (name.equals("Cow")) {
			neighborPairs.add(new String[] {"Loin", "Head"});
			neighborPairs.add(new String[] {"Loin", "Left Arm"});
			neighborPairs.add(new String[] {"Loin", "Right Arm"});
			neighborPairs.add(new String[] {"Loin", "Flank"});
			neighborPairs.add(new String[] {"Flank", "Left Shank"});
			neighborPairs.add(new String[] {"Flank", "Right Shank"});
		}
		else
			throw new IllegalArgumentException("Could not instantiate anatomy slots for missing being type: " + name);
		return neighborPairs;
	}
	
	TODO: //Why is this an extended class?
	public static class MainBodyPart extends BodyPart {
		public MainBodyPart(String name, Vector3f position, double size, double vulnerability, double maxHealth,
				double dexterity) {
			super(name, position, size, vulnerability, maxHealth, dexterity);
			// Auto-generated constructor stub
		}
	}
	
	public static class BodyDamage {
		public String name;
		public double damage;
		public double careNeeded;
		
		public BodyDamage(String name, double damage, double careNeeded) {
			this.name = name;
			this.damage = damage;
			this.careNeeded = careNeeded;
		}
	}
	
	public static class BodyTrait {
		public String traitName;
		public double traitModifier;
		
		public BodyTrait(String traitName, double traitModifier) {
			this.traitName = traitName;
			this.traitModifier = traitModifier;
		}
	}
	
}
