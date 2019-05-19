package io.github.dantetam.world.civilization.language;

public class Language {

	//public MarkovChainGenerator probGenerator;
	
	public String languageFamily;
	public String languageName;
	public String groupIdentifier;
	
	public Language(String languageFamily, String languageName, String groupIdentifier) {
		this.languageFamily = languageFamily;
		this.languageName = languageName;
		this.groupIdentifier = groupIdentifier;
	}
	
	public String generateName() {
		return "Name" + (int)(Math.random() * System.currentTimeMillis() % 100000); 
	}
	
}
