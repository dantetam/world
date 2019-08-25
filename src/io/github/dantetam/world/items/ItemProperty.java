package io.github.dantetam.world.items;

import io.github.dantetam.toolbox.StringUtil;
import io.github.dantetam.world.civilization.artwork.ArtworkGraph;
import io.github.dantetam.world.items.InventoryItem.ItemQuality;
import io.github.dantetam.world.life.Human;

public abstract class ItemProperty {
	
	public final String propertyType;
	public String propertyValue;
	
	public ItemProperty(String propertyType, String propertyValue) {
		this.propertyType = propertyType;
		this.propertyValue = propertyValue;
	}
	
	public static class ItemArtProperty extends ItemProperty {
		public String artworkName;
		public Human author;
		public String artworkGeneralStyle;
		public ItemQuality quality;
		
		public ArtworkGraph artGraph;
		
		public ItemArtProperty(String artworkName, Human author, String artworkGeneralStyle, ItemQuality quality,
				ArtworkGraph artGraph) {
			super("Artwork", artworkName);
			this.artworkName = artworkName;
			this.author = author;
			this.artworkGeneralStyle = artworkGeneralStyle;
			this.quality = quality;
			this.artGraph = artGraph;
		}
		
		public static String generateArtName() {
			return StringUtil.genAlphaNumericStr(20);
		}
	}
	
}
