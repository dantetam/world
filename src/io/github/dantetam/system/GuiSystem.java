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
		
		int height = (int) Math.floor(camera.tileLocationPosition.y);
		int minX = (int) Math.floor(camera.tileLocationPosition.x - camera.numTilesX);
		int minZ = (int) Math.floor(camera.tileLocationPosition.z - camera.numTilesZ);
		int maxX = (int) Math.ceil(camera.tileLocationPosition.x + camera.numTilesX);
		int maxZ = (int) Math.ceil(camera.tileLocationPosition.z + camera.numTilesZ);
		
		float guiWidth = DisplayManager.width / (camera.numTilesX * 2 + 1);
		float guiHeight = DisplayManager.height / (camera.numTilesZ * 2 + 1);
		Vector2f guiDim = new Vector2f(guiWidth,  guiHeight);
		
		int id = ItemData.getIdFromName("Air");
		int airTileTexture = ItemData.getTextureFromItemId(id);
		
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				Vector2f guiPos = new Vector2f(guiWidth * (x - minX), guiHeight * (z - minZ)); 
				int candidateHeight = height; //Find the highest height <= camera height, in the rendering style of DF
				LocalTile tile;
				while (candidateHeight > 0) {
					tile = activeGrid.getTile(new Vector3i(x,z,candidateHeight));
					if (tile != null) {
						if (tile.isOccupied()) {
							if (candidateHeight != height) {
								int tileTexture = ItemData.getTextureFromItemId(tile.tileBlockId);
								listGuis.add(new GuiQuad(tileTexture, guiPos, guiDim));
								
								listGuis.add(new GuiQuad(airTileTexture, guiPos, guiDim));
								
								break;
							}
							
							if (tile.tileBlockId != ItemData.ITEM_EMPTY_ID) {
								int tileTexture = ItemData.getTextureFromItemId(tile.tileBlockId);
								listGuis.add(new GuiQuad(tileTexture, guiPos, guiDim));
							}
							else if (tile.tileFloorId != ItemData.ITEM_EMPTY_ID) {
								int tileTexture = ItemData.getTextureFromItemId(tile.tileFloorId);
								listGuis.add(new GuiQuad(tileTexture, guiPos, guiDim));
							}
							if (tile.itemOnFloor != null) {
								TextBox buildingTextBox = getDefaultTextBoxGui(guiDefaultTexture, "I", "", guiPos.x, guiPos.y, guiWidth, guiHeight);
								listTexts.add(buildingTextBox);
							}
							break;
						}
					}
					candidateHeight--;
				}
			}
		} 
		
		System.out.println(activeGrid.getAllBuildings().size());
		
		for (LocalBuilding building: activeGrid.getAllBuildings()) {
			for (int i = 0; i < building.calculatedLocations.size(); i++) {
				Vector3i coords = building.calculatedLocations.get(i);
				
				int emptyHeight = activeGrid.findLowestEmptyHeight(coords.x, coords.y);
				
				if (coords.z <= height && coords.x >= minX && coords.x <= maxX && coords.y >= minZ && coords.y <= maxZ) {
					int blockId = building.buildingBlockIds.get(i);
					Vector2f guiPos = new Vector2f(guiWidth * (coords.x - minX), guiHeight * (coords.y - minZ)); 
					
					int tileTexture = ItemData.getTextureFromItemId(blockId);
					listGuis.add(new GuiQuad(tileTexture, guiPos, guiDim));
				}
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

	public TextBox getDefaultTextButton(int texture, String command, String display, String tooltip, float a, float b,
			float c, float d) {
		Button temp = new Button(12, null, display.length(), true, texture, command, display, tooltip, a, b, c, d);
		return temp;
	}

	public TextBox getDefaultTextBoxGui(int texture, String display, String tooltip, float a, float b, float c,
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
