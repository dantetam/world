package io.github.dantetam.world.dataparse.texturespritegen;

import java.util.HashMap;
import java.util.Map;

public class AyeneSpriteHandler extends SpriteHandler {
	
	protected String fileName = "res/spritesets/ayene/terrain.png";
	protected int spriteWidth = 32, spriteHeight = 32;
	
	@Override
	protected Map<String, SpriteSheetInstruction> getItemsToSpriteCoordsMap() {
		Map<String, SpriteSheetInstruction> itemsToSpriteCoordsMap = new HashMap<>();
		SpriteSheetInstruction inst;
		
		inst = new SpriteSheetInstruction(4,3);
		itemsToSpriteCoordsMap.put("Mud", inst);
		
		return itemsToSpriteCoordsMap;
	}
	
}
