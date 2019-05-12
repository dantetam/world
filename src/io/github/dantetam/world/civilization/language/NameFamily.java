package io.github.dantetam.world.civilization.language;

public class NameFamily {

	//public MarkovChainGenerator probGenerator;
	
	public String languageFamily;
	
	public NameFamily(String languageFamily) {
		this.languageFamily = languageFamily;
	}
	
	public String generateName() {
		return "Name" + (int)(Math.random() * System.currentTimeMillis() % 100000); 
	}
	
}
