package io.github.dantetam.world.dataparse;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.dantetam.toolbox.ListUtil;
import io.github.dantetam.world.life.Ethos;
import io.github.dantetam.world.life.HumanBrain.EthosModifier;

public class EthosData {

	//Note that opposite ethos is a one way relation e.g.
	//kind people can have an aversion to cruel people, 
	//but cruel people are apathetic the other way.
	public static Map<String, Set<String>> oppositeEthosMap;
	
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
	public static Map<String, Ethos> initGreatEthos() {
		return greatEthos;
	}
	
	/**
	 * For initialization only
	 */
	public static Map<String, Ethos> initPersonalTraits() {
		return ethosPersonalityTraits;
	}
	
	public static EthosModifier[] parseEthosMods(String str) {
		return new EthosModifier[] {};
	}
	
	public static Collection<Ethos> getAllEthos() {
		return ListUtil.stream(greatEthos.values(), ethosPersonalityTraits.values());
	}
	
}
