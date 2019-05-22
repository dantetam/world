package io.github.dantetam.world.dataparse;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.dantetam.world.life.Ethos;
import io.github.dantetam.world.life.HumanBrain.EthosModifier;

public class EthosData {

	private static Map<String, Ethos> greatEthos = new HashMap<>(); //TODO
	
	public static Collection<Ethos> getMajorEthos() {
		return greatEthos.values();
	}
	
	public static EthosModifier[] parseEthosMods(String str) {
		return new EthosModifier[] {};
	}
	
}
