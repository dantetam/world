package io.github.dantetam.world.civhumanrelation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import io.github.dantetam.toolbox.MapUtil;
import io.github.dantetam.world.civhumanai.StringDoubleGamut;

/**
 * Represents a range of different emotions with distinct values. This is needed
 * to capture nuances in emotion beyond good and bad. For example, being friends with someone,
 * does not mean getting married to that person.
 * 
 * @author Dante
 *
 */

public class EmotionGamut extends StringDoubleGamut {

	public static final Set<String> EMOTIONS = new HashSet<String>() {{
		add("Kindness"); add("Honor"); add("Attraction"); add("Admiration");
		add("Rationality"); 
		add("Hate"); add("Indifference");
	}};
	
	public EmotionGamut() {
		super();
	}
	
}
