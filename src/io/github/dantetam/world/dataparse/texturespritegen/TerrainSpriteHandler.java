package io.github.dantetam.world.dataparse.texturespritegen;

import java.util.HashMap;
import java.util.Map;

import io.github.dantetam.world.dataparse.ItemData;

public class TerrainSpriteHandler extends SpriteHandler {
	
	protected int getSpriteWidth() {return 32;}
	protected int getSpriteHeight() {return 32;}
	
	@Override
	protected Map<String, SpriteSheetInstruction> getItemsToSpriteCoordsMap() {
		Map<String, SpriteSheetInstruction> map = new HashMap<>();
		SpriteSheetInstruction inst;
		
		String ay1 = "res/spritesets/ayene/terrain.png";
		String mc = "res/spritesets/mine/minecraft_tiles_big.png";
		
		inst = new SpriteSheetInstruction(ay1,4,3);
		map.put("Mud", inst);

		map.put("Ice", new SpriteSheetInstruction(mc,5,4));
		
		map.put("Granite", 
				new SpriteSheetInstruction(mc,1,2).shade(rgb(125,125,125), rgb(190,80,80), 800));
		map.put("Shale", new SpriteSheetInstruction(mc,2,2));
		map.put("Slate", 
				new SpriteSheetInstruction(mc,2,2).shade(rgb(125,125,125), rgb(0), 800));
		map.put("Gneiss", new SpriteSheetInstruction(mc,2,2));
		map.put("Olivine", 
				new SpriteSheetInstruction(mc,2,2).shade(rgb(125,125,125), rgb(120,120,0), 800));
		map.put("Mudstone", 
				new SpriteSheetInstruction(mc,2,4).shade(rgb(125,125,125), rgb(100,70,36), 800));
		map.put("Diorite", 
				new SpriteSheetInstruction(mc,1,2).shade(rgb(125,125,125), rgb(255), 800));
		
		map.put("Gold Ore", new SpriteSheetInstruction(mc,3,1));
		map.put("Native Gold Ore", new SpriteSheetInstruction(mc,3,1));
		map.put("Crystalite Ore", new SpriteSheetInstruction(mc,3,2));
		map.put("Hematite", 
				new SpriteSheetInstruction(mc,3,2).shade(rgb(255,255,255), rgb(255,0,0), 200));
		map.put("Vaelium Ore", new SpriteSheetInstruction(mc,3,3));
		map.put("Fluvium Ore", new SpriteSheetInstruction(mc,4,4));
		map.put("Dirfractium Ore", new SpriteSheetInstruction(mc,15,15));
		map.put("Electrum Ore", new SpriteSheetInstruction(mc,11,1));
		map.put("Hellenite Ore", 
				new SpriteSheetInstruction(mc,4,4).shade(rgb(125,125,125), rgb(255,0,0), 200));
		map.put("Glidestone Ore", new SpriteSheetInstruction(mc,7,10));
		map.put("Leadenstone Ore", 
				new SpriteSheetInstruction(mc,3,3).shade(rgb(125,125,125), rgb(0,0,0), 200));
		
		map.put("Water", new SpriteSheetInstruction(mc,13,15));
		
		map.put("Straw Bale", new SpriteSheetInstruction(ay1,6,8));
		
		listAddTexture(map, ItemData.itemsWithPartName("Tree"), new SpriteSheetInstruction(mc,1,16));
		listAddTexture(map, ItemData.itemsWithPartName("Sapling"), new SpriteSheetInstruction(mc,4,16));
		
		map.put("Wooden Wall", new SpriteSheetInstruction(ay1,1,1));
		map.put("Brick Wall", new SpriteSheetInstruction(mc,1,8));
		map.put("Concrete", new SpriteSheetInstruction(mc,1,7));
		map.put("Stone Wall", new SpriteSheetInstruction(mc,1,6));
		map.put("Earthen Wall", 
				new SpriteSheetInstruction(mc,3,3).shade(rgb(125,125,125), rgb(100,70,36), 200));
		
		map.put("Compost", new SpriteSheetInstruction(mc,1,3));
		
		map.put("Wooden Bed", new SpriteSheetInstruction(mc,9,8));
		
		listAddTexture(map, ItemData.itemsWithPartName("Wooden "), new SpriteSheetInstruction(mc,3,12));
		
		listAddTexture(map, ItemData.itemsWithPartName("Plant"), new SpriteSheetInstruction(mc,5,16));
		
		return map;
	}
	
}
