package io.github.dantetam.system;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.util.vector.Vector2f;

import io.github.dantetam.lwjglEngine.entities.Gui2DCamera;
import io.github.dantetam.lwjglEngine.gui.GuiQuad;
import io.github.dantetam.lwjglEngine.render.DisplayManager;
import io.github.dantetam.lwjglEngine.render.VBOLoader;
import io.github.dantetam.render.Button;
import io.github.dantetam.render.GameLauncher;
import io.github.dantetam.render.TextBox;
import io.github.dantetam.toolbox.MousePicker;
import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.dataparse.ItemData;
import io.github.dantetam.world.grid.LocalBuilding;
import io.github.dantetam.world.grid.LocalGrid;
import io.github.dantetam.world.grid.LocalTile;

public class GuiSystem extends BaseSystem {

	public MousePicker mousePicker;

	private Map<LocalTile, List<GuiQuad>> tileDisplay;
	
	private List<GuiQuad> allGuiQuad;
	private List<TextBox> allGuiText;

	private int guiDefaultTexture;

	// Done in this function ideally after GL context has been set up
	public void setupLoader() {
		guiDefaultTexture = VBOLoader.loadTexture("guiDefaultTexture");
	}

	public GuiSystem(GameLauncher gameLauncher) {
		super(gameLauncher);
		tileDisplay = new HashMap<>();
	}

	public void updateUI() {
		//tileDisplay.entrySet().removeIf(entry -> entry.getKey().location == null);
		LocalGrid activeGrid = gameLauncher.worldGrid.activeLocalGrid;
		Gui2DCamera camera = super.gameLauncher.camera;
		
		List<GuiQuad> listGuis = new ArrayList<>();
		List<TextBox> listTexts = new ArrayList<>();
		
		int originalHeight = (int) Math.floor(camera.tileLocationPosition.y);
		int minX = (int) Math.floor(camera.tileLocationPosition.x - camera.numTilesX);
		int minZ = (int) Math.floor(camera.tileLocationPosition.z - camera.numTilesZ);
		int maxX = (int) Math.ceil(camera.tileLocationPosition.x + camera.numTilesX);
		int maxZ = (int) Math.ceil(camera.tileLocationPosition.z + camera.numTilesZ);
		
		float guiWidth = DisplayManager.width / (camera.numTilesX * 2 + 1);
		float guiHeight = DisplayManager.height / (camera.numTilesZ * 2 + 1);
		Vector2f guiDim = new Vector2f(guiWidth,  guiHeight);
		
		int airTileTexture = ItemData.getTextureFromItemId(ItemData.getIdFromName("Air"));
		
		int darknessTexture = ItemData.getTextureFromItemId("Darkness");
		
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				Vector2f guiPos = new Vector2f(guiWidth * (x - minX), guiHeight * (z - minZ)); 
				int candidateHeight = originalHeight; //Find the highest height <= camera height, in the rendering style of DF
				LocalTile tile;
				while (candidateHeight > 0) {
					tile = activeGrid.getTile(new Vector3i(x,z,candidateHeight));
					if (tile != null) { //if (activeGrid.tileIsOccupied(tile.coords))
						/*
						if (!tile.exposedToAir) {
							listGuis.add(new GuiQuad(darknessTexture, guiPos, guiDim));
						}
						*/
						
						/*
						if (tile.tileBlockId == ItemData.ITEM_EMPTY_ID) {
							candidateHeight--;
							continue;
						}
						*/

						if (tile.tileBlockId != ItemData.ITEM_EMPTY_ID) {
							int tileTexture = ItemData.getTextureFromItemId(tile.tileBlockId);
							listGuis.add(new GuiQuad(tileTexture, guiPos, guiDim));
			 			}
						else if (tile.tileFloorId != ItemData.ITEM_EMPTY_ID) {
							int tileTexture = ItemData.getTextureFromItemId(tile.tileFloorId);
							listGuis.add(new GuiQuad(tileTexture, guiPos, guiDim));
						}
						if (tile.itemsOnFloor.size() > 0) {
							TextBox buildingTextBox = getDefaultTextBoxGui(guiDefaultTexture, "I", "", guiPos.x, guiPos.y, guiWidth, guiHeight);
							listTexts.add(buildingTextBox);
						}
						if (tile.getPeople() != null && tile.getPeople().size() > 0) {
							TextBox buildingTextBox = getDefaultTextBoxGui(guiDefaultTexture, "H!", "", guiPos.x, guiPos.y, guiWidth, guiHeight);
							listTexts.add(buildingTextBox);
						}
						
						if (candidateHeight != originalHeight) { //Air overlay for looking at lower heights with air on top
							listGuis.add(new GuiQuad(airTileTexture, guiPos, guiDim));
						}
						break;
					}
					
					candidateHeight--;
				}
			}
		}
		
		for (LocalBuilding building: activeGrid.getAllBuildings()) {
			int i = 0;
			for (Vector3i coords: building.calculatedLocations) {
				//int emptyHeight = activeGrid.findLowestEmptyHeight(coords.x, coords.y);
				
				if (coords.z <= originalHeight && coords.x >= minX && coords.x <= maxX && coords.y >= minZ && coords.y <= maxZ) {
					int blockId = building.buildingBlockIds.get(i);
					Vector2f guiPos = new Vector2f(guiWidth * (coords.x - minX), guiHeight * (coords.y - minZ)); 
					
					int tileTexture = ItemData.getTextureFromItemId(blockId);
					listGuis.add(new GuiQuad(tileTexture, guiPos, guiDim));
				}
				i++;
			}
		}
		
		allGuiQuad = listGuis;
		allGuiText = listTexts;
	}

	public List<GuiQuad> getAllNonTextUI() {
		if (allGuiQuad == null) {
			updateUI();
		}
		return allGuiQuad;
	}

	public List<TextBox> getAllTextUI() {
		if (allGuiText == null) {
			updateUI();
		}
		return allGuiText;
	}

	public static TextBox getDefaultTextButton(int texture, String command, String display, String tooltip, float a, float b,
			float c, float d) {
		Button temp = new Button(12, null, display.length(), true, texture, command, display, tooltip, a, b, c, d);
		return temp;
	}

	public static TextBox getDefaultTextBoxGui(int texture, String display, String tooltip, float a, float b, float c,
			float d) {
		TextBox temp = new TextBox(12, null, display.length(), true, texture, display, tooltip, a, b, c, d);
		return temp;
	}

	@Override
	public void tick() {
		if (gameLauncher.inputSystem.lastMoving || gameLauncher.inputSystem.moving) {
			updateUI();
		}
	}

	public class GuiTextureMix {
		public List<GuiQuad> guis;
		public List<TextBox> texts;

		public GuiTextureMix(List<GuiQuad> guis, List<TextBox> texts) {
			this.guis = guis;
			this.texts = texts;
		}
	}

}
