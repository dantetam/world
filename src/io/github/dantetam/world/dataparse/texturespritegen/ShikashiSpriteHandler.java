package io.github.dantetam.world.dataparse.texturespritegen;

import java.util.HashMap;
import java.util.Map;

import io.github.dantetam.world.dataparse.texturespritegen.SpriteHandler.SpriteSheetInstruction;

public class ShikashiSpriteHandler extends SpriteHandler {

	protected String getFileName() {return "res/spritesets/shikashi/main.png";}
	protected int getSpriteWidth() {return 32;}
	protected int getSpriteHeight() {return 32;}
	
	@Override
	protected Map<String, SpriteSheetInstruction> getItemsToSpriteCoordsMap() {		
		Map<String, SpriteSheetInstruction> itemsToSpriteCoordsMap = new HashMap<>();
		SpriteSheetInstruction inst;
		
		return itemsToSpriteCoordsMap;
	}
	
}
