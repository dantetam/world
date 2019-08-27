package io.github.dantetam.world.civhumanai;

import java.util.HashSet;
import java.util.Set;

public class NeedsGamut extends StringDoubleGamut {

	public static final String EAT = "Eat", SHELTER = "Shelter", CLOTHING = "Clothing",
			PERSONAL_HOME = "Personal Home", FURNITURE = "Furniture", BEAUTY = "Beauty",
			SOLDIER = "Soldier";
	
	public NeedsGamut() {
		super();
	}
	
	@Override
	public Set<String> getFields() {
		return GamutDataFields.NEEDS;
	}
	
}
