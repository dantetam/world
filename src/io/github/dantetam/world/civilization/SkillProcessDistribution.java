package io.github.dantetam.world.civilization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;

import io.github.dantetam.world.dataparse.SkillData;

/*
 	Add a skill distribution consideration here i.e.
	a probabilistic mapping of skill levels and requirements, into the possible outcomes a process,
	such as item quality, quantity, residual action effects strength, and so on.
	
	From the CSV parsing, a skill process distribution is made of individual skill process mods.
	A skill process mod can take 6 or 9 arguments (last 3 optional):
	
	skillName:  The skill of human which triggers this process mod
	processKey: The desired effect, one of the enum ProcessModKey
	a, b, F, n (double, double, String, double): The parameters of the formula OUTPUT = a * F_n(x) + b
	
	(optional args)
	Noise (String): The noise distribution to be used in simulating random chance/error
	noise_p1, noise_p2: The two parameters of the noise distribution (see enum NoiseDistribution)
*/

public class SkillProcessDistribution {
	//Implement common buffs/debuffs/results here
	//These are stored as objects here,
	//and in the CSV for recipes as a unified made up language.
	public List<SkillProcessMod> skillProcessMods;
	
	public SkillProcessDistribution() {
		skillProcessMods = new ArrayList<>();
	}
	
	public static class SkillProcessMod {
		public String skillName;
		public ProcessModKey processKey;
		public SkillOutFunction func;
		public NoiseDistribution noise;
		public SkillProcessMod(String skillName, ProcessModKey processKey, SkillOutFunction func) {
			super();
			this.skillName = skillName;
			this.processKey = processKey;
			this.func = func;
		}
	}
	
	public static enum ProcessModKey {
		outQuality, 
		outQuantCount, outQuantMulti,
		resActionCount, resActionMulti,
		timeSupMul,
		timeUnsupMul,
		timeAllMul
	}
	
	/*
	public static class MultiCompositeOutFunction {
		public Map<SkillOutFunction, Double> functionsByWeight;
		
		public double eval(Map<String, Skill>)
	}
	*/
	
	/**
	Represent a function that takes in the skill level,
	and returns a point. The output of the final function is this point with error sampled from a distribution.
	
	The function is represented as 
	a * F(x) + b
	where a, b are constants, F(x) is a function which takes the input x and a parameter n i.e. sin nx
	*/
	public static class SkillOutFunction {
		public double a, b;
		public BasicFunction func;
		public Double n = null;
		
		public SkillOutFunction(double a, double b, BasicFunction func, double n) {
			this.a = a;
			this.b = b;
			this.func = func;
			this.n = n;
		}
		
		//Return y = a * F(x) + b
		public double eval(double x) {
			double fx = 0;
			if (func == BasicFunction.POWER) {
				fx = Math.pow(x, n);
			}
			else if (func == BasicFunction.LOG) {
				fx = Math.log(x) / Math.log(n);
			}
			else if (func == BasicFunction.SIN) {
				fx = Math.sin(n * x);
			}
			return a * fx + b;
		}
	}
	
	public enum BasicFunction {
		//x^n, log_n(x), sin(nx)
		POWER, LOG, SIN
	}
	
	public static class NoiseDistribution {
		private RealDistribution distr;
		public NoiseDistribution(NoiseDistributionType type, double param1, double param2) {
			if (type == NoiseDistributionType.NORMAL) {
				distr = new NormalDistribution(param1, param2);
			} else if (type == NoiseDistributionType.EXPO) {
				distr = new ExponentialDistribution(param1, param2);
			} else if (type == NoiseDistributionType.UNIFORM) {
				distr = new UniformRealDistribution(param1, param2);
			}
		}	
		public double sample() {
			return distr.sample();
		}
	}
	
	public enum NoiseDistributionType {
		NORMAL,    //mean, var
		EXPO,      //start, probability (continuous analog of geometric distr)
		UNIFORM    //start, end (inclusive)
	}
	
}
