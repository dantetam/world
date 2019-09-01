package io.github.dantetam.world.life;

public class Animal extends LivingEntity {

	public Animal(String name, String speciesName) {
		super(name);
		this.body = new Body(speciesName);
	}

	
	
}
