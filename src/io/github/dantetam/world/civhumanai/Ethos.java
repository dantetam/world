package io.github.dantetam.world.civhumanai;

import org.apache.commons.math3.analysis.function.Sigmoid;

import io.github.dantetam.world.dataparse.EthosData;

/**
 * Represents a societal thought, personality trait, or other intellectual quirk
 * Note that ethos are essentially brain values with state, so they should be cloned
 * into other objects that require ethos.
 * @author Dante
 *
 */
public class Ethos {
	public String name;
	
	//Compilable in the standard format of people opinion modifier syntax
	//All of these are evaluated at once, i.e.:
	
	/*
	 * For all other people in society,
	 * if person is an elf and has a job,
	 * lower base opinion to -50.
	 * 
	 * {"SOCIETY_OTHERS", "IF_RACE_IS_ELF", "BASE_OPINION"}
	 * 
	 * Available:
	 * MOD:xyz <- An additional modifier to this ethos for data purposes 
	 */
	public EthosModifier[] modifiers; 
	
	/*
	 * After believing in this ethos, apply these psychological effects to the human
	 * or sentient being when making decisions. The effects:
	 * 
	 */
	public EthosModifier[] effects;
	
	//log scale from 
	public double severity; 
	
	public double ethosLifetimeHappiness;
	
	public Ethos(String name, double severity, String modsString, String effectsString) {
		this.name = name;
		this.severity = severity;
		this.modifiers = EthosData.parseEthosMods(modsString);
		this.effects = EthosData.parseEthosMods(effectsString);
		this.ethosLifetimeHappiness = 0;
	}
	
	/**
	 * @return A value between low and high on the sigmoid graph, at value severity
	 */
	public double getLogisticVal(double low, double high) {
		return new Sigmoid(low, high).value(severity);
	}
	/**
	 * @return A value between 0 and 1 on the sigmoid graph, such that returned value = sigmoid(severity)
	 */
	public double getNormLogisticVal() {
		return new Sigmoid().value(severity);
	}
	
	public Ethos clone() {
		Ethos clone = new Ethos(this.name, this.severity, "", "");
		clone.modifiers = this.modifiers;
		clone.effects = this.effects;
		return clone;
	}
	
	public boolean equals(Object other) {
		if (!(other instanceof Ethos)) {
			return false;
		}
		Ethos ethos = (Ethos) other;
		return this.name.equals(ethos.name);
	}
	
	public int hashCode() {
		return this.name.hashCode();
	}
	
}