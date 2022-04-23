package io.github.dantetam.world.dataparse.texturespritegen;

import java.util.HashMap;
import java.util.Map;

import io.github.dantetam.world.dataparse.ItemData;
import io.github.dantetam.world.dataparse.texturespritegen.SpriteHandler.SpriteSheetInstruction;

public class ToolSpriteHandler extends SpriteHandler {

	protected String getFileName() {return "res/spritesets/shikashi/main.png";}
	protected int getSpriteWidth() {return 32;}
	protected int getSpriteHeight() {return 32;}
	
	@Override
	protected Map<String, SpriteSheetInstruction> getItemsToSpriteCoordsMap() {		
		Map<String, SpriteSheetInstruction> map = new HashMap<>();
		
		String net = "res/spritesets/nethack/nethack.jpg";
		
		listAddTexture(map, ItemData.itemsWithPartName("Axe"), new SpriteSheetInstruction(net,11,22));
		listAddTexture(map, ItemData.itemsWithPartName("Pickaxe"), new SpriteSheetInstruction(net,12,5));
		
		return map;
	}
	
}
