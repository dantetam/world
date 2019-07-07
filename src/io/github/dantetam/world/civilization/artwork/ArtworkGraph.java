package io.github.dantetam.world.civilization.artwork;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.dantetam.toolbox.StringUtil;
import io.github.dantetam.world.civilization.LocalExperience;
import io.github.dantetam.world.life.Human;

public class ArtworkGraph {

	public List<ArtworkMotif> allMotifs;
	
	public ArtworkGraph() {
		this.allMotifs = new ArrayList<>();
	}
	
	public static class ArtworkMotif {
		public String name;
		public String description;
		public String quickDesc;
		
		public Map<ArtworkMotif, String> motifLinks;
		
		public ArtworkMotif(String name, String description, String quickDesc) {
			this.name = name;
			this.description = description;
			this.quickDesc = quickDesc;
			motifLinks = new HashMap<>();
		}
	}
	
	public String toString() {
		return null;
	}
	
	public static ArtworkGraph generateRandArtGraph(Human artist, LocalExperience experience) {
		TODO; //Use in art creation events
		
		ArtworkGraph art = new ArtworkGraph();
		
		List<ArtworkMotif> allMotifs = new ArrayList<>();
		int num = (int) (Math.random() * 5) + 3;
		for (int i = 0; i < num; i++) {
			ArtworkMotif motif = new ArtworkMotif(
					StringUtil.genAlphaNumericStr(12),
					StringUtil.genAlphaNumericStr(12),
					StringUtil.genAlphaNumericStr(30)
			);
			allMotifs.add(motif);
		}
		
		int numConnections = (int) (Math.random() * 4) + 4;
		for (int i = 0; i < numConnections; i++) {
			int randIndexSuper = (int) (Math.random() * num);
			int randIndexSub = (int) (Math.random() * num);
			if (Math.random() > 0.85 && num > 1) {
				while (randIndexSuper == randIndexSub) {
					randIndexSuper = (int) (Math.random() * num);
					randIndexSub = (int) (Math.random() * num);
				}
			}
			ArtworkMotif motifA = allMotifs.get(randIndexSuper);
			ArtworkMotif motifB = allMotifs.get(randIndexSub);
			motifA.motifLinks.put(motifB, StringUtil.genAlphaNumericStr(12));
		}
		
		art.allMotifs = allMotifs;
		return art;
	}
	
}
