package io.github.dantetam.world.civilization.language;

import java.util.HashMap;
import java.util.Map;

import io.github.dantetam.toolbox.MapUtil;

public class LanguageMarkovChain {

	private TrieProbability root; 
	
	public LanguageMarkovChain() {
		root = new TrieProbability(null, false);
	}
	
	public String traverseRandom() {
		String result = "";
		TrieProbability node = root;
		while (node.letter == null) {
			node = node.randomNextChar();
		}
		while (node != null) {
			result += node.letter;
			if (node.activated) {
				if (Math.random() < 0.1) {
					break;
				}
			}
			node = node.randomNextChar();
		}
		return result;
	}
	
	public static class TrieProbability {
		public Character letter; //null iff root
		public boolean activated;
		public Map<TrieProbability, Double> nextTrieNodes;
		
		public TrieProbability(Character letter, boolean activated) {
			this.letter = letter;
			this.activated = activated;
			nextTrieNodes = new HashMap<>();
		}
		
		public void putNewLetter(Character letter, double value) {
			nextTrieNodes.put(new TrieProbability(letter, true), value);
		}
		
		public TrieProbability randomNextChar() {
			return MapUtil.randChoiceFromWeightMap(nextTrieNodes);
		}
	}
	
}
