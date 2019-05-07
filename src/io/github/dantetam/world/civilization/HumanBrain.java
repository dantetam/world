package io.github.dantetam.world.civilization;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.analysis.function.Sigmoid;

public class HumanBrain {

	public Map<String, Ethos> personalEthos;
	
	public HumanBrain() {
		personalEthos = new HashMap<>();
		TODO
	}
	
	public static class Ethos {
		public String name;
		public String[] modifiers; //Compilable in the standard format of people opinion modifier syntax
		public double severity; 
		
		public double getLogisticVal(double low, double high) {
			return new Sigmoid(low, high).value(severity);
		}
		public double getNormLogisticVal() {
			return new Sigmoid().value(severity);
		}
	}
	
}
