package io.github.dantetam.world.dataparse;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import io.github.dantetam.world.civilization.HumanBrain.Ethos;
import io.github.dantetam.world.civilization.HumanBrain.EthosModifier;

public class EthosData {

	private static Map<String, Ethos> greatEthos; //TODO
	
	public static Collection<Ethos> getMajorEthos() {
		return greatEthos.values();
	}
	
	public static EthosModifier[] parseEthosMods(String str) {
		return new EthosModifier[] {};
	}
	
}
