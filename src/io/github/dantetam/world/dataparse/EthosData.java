package io.github.dantetam.world.dataparse;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.dantetam.world.life.Ethos;
import io.github.dantetam.world.life.HumanBrain.EthosModifier;

public class EthosData {

	private static Map<String, Ethos> greatEthos = new HashMap<>(); //TODO
	private static Map<String, Ethos> ethosPersonalityTraits = new HashMap<>();
	
	public static Collection<Ethos> getMajorEthos() {
		return greatEthos.values();
	}
	
	public static Collection<Ethos> getPersonalityTraits() {
		return ethosPersonalityTraits.values();
	}
	
	/**
	 * For initialization only
	 */
	public Map<String, Ethos> initGreatEthos() {
		return greatEthos;
	}
	
	/**
	 * For initialization only
	 */
	public Map<String, Ethos> initPersonalTraits() {
		return ethosPersonalityTraits;
	}
	
	public static EthosModifier[] parseEthosMods(String str) {
		return new EthosModifier[] {};
	}
	
}
