package io.github.dantetam.world.civhumanai;

import java.util.HashSet;
import java.util.Set;

import io.github.dantetam.world.civhumanrelation.EmotionGamut;

public class GamutDataFields {

	public static Set<String> NEEDS = new HashSet<String>() {{
		add(NeedsGamut.EAT); add(NeedsGamut.SHELTER); add(NeedsGamut.CLOTHING);
		add(NeedsGamut.PERSONAL_HOME); add(NeedsGamut.FURNITURE); add(NeedsGamut.BEAUTY);
		add(NeedsGamut.SOLDIER); add(NeedsGamut.SOCIAL); add(NeedsGamut.REST);
	}};
	
	public static Set<String> EMOTIONS = new HashSet<String>() {{
		add(EmotionGamut.KINDNESS); add(EmotionGamut.HONOR); add(EmotionGamut.ATTRACTION);
		add(EmotionGamut.ADMIRATION); add(EmotionGamut.RATIONALITY);
		add(EmotionGamut.HATE); add(EmotionGamut.INDIFFERENCE);
	}};
	
}
