package io.github.dantetam.system;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.dantetam.localdata.ConstantData;
import io.github.dantetam.lwjglEngine.fontRendering.TextMaster;
import io.github.dantetam.lwjglEngine.gui.GuiQuad;
import io.github.dantetam.lwjglEngine.render.DisplayManager;
import io.github.dantetam.lwjglEngine.render.VBOLoader;
import io.github.dantetam.render.Button;
import io.github.dantetam.render.GameLauncher;
import io.github.dantetam.render.Menu;
import io.github.dantetam.render.TextBox;
import io.github.dantetam.toolbox.MapUtil;
import io.github.dantetam.toolbox.MousePicker;
import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.dataparse.ItemData;
import io.github.dantetam.world.grid.LocalTile;
import io.github.dantetam.world.life.LivingEntity;

public class MenuSystem extends BaseSystem {

	public List<Menu> menus;
	public Map<String, TextBox> textboxes;

	private List<Click> clicks;

	// The menuActivated field is updated whenever a user clicks on any active menu.
	// This signals to systems after, that the user's input should be restricted to
	// menus only.
	public boolean menuActivated = false, menuHighlighted = false;

	private int guiDefaultTexture;

	public Button[] shortcuts = new Button[10];
	
	public Vector3i mouseHighlighted;

	public MousePicker mousePicker;
	
	// Done in this function ideally after GL context has been set up
	public void setupLoader() {
		guiDefaultTexture = VBOLoader.loadTexture("guiDefaultTexture");
	}

	public MenuSystem(GameLauncher civGame) {
		super(civGame);
		menus = new ArrayList<Menu>();
		textboxes = new HashMap<>();
		clicks = new ArrayList<Click>();
		setupMenus();
	}

	public void setupMenus() {
		float posX, posY, sizeX, sizeY;
		float w = DisplayManager.width, h = DisplayManager.height;
		
		sizeX = w * 1 / 5; sizeY = 300;
		posX = w * 4 / 5; posY = h - sizeY;
		textboxes.put("TileHint", GuiSystem.getDefaultTextButton(guiDefaultTexture, "", "", "", 
				posX, posY, sizeX, sizeY));
	}
	
	public void updateMenus() {
		Vector3i highlightCoords = mousePicker.calculateWorldCoordsFromMouse();
		
		int candidateHeight = highlightCoords.z; //Find the highest height <= camera height, in the rendering style of DF
		LocalTile tile = null;
		while (candidateHeight > 0) {
			tile = gameLauncher.worldGrid.activeLocalGrid.getTile(new Vector3i(highlightCoords.x,highlightCoords.y,candidateHeight));
			if (tile != null) { //if (activeGrid.tileIsOccupied(tile.coords))
				if (tile.tileBlockId != ItemData.ITEM_EMPTY_ID || tile.tileFloorId != ItemData.ITEM_EMPTY_ID || 
						tile.itemsOnFloor.size() > 0 || (tile.getPeople() != null && tile.getPeople().size() > 0)) {
					break;
				}
			}
			candidateHeight--;
		}
		
		this.mouseHighlighted = highlightCoords;
		
		List<String> texts;
		
		texts = new ArrayList<>();
		if (this.mouseHighlighted != null) {
			texts.add(this.mouseHighlighted.toString());
			
			if (tile != null) {
				texts.add("Block: " + ItemData.getNameFromId(tile.tileBlockId));
				texts.add("Floor: " + ItemData.getNameFromId(tile.tileFloorId));
				
				if (tile.itemsOnFloor.size() > 0) {
					texts.add("Items on Floor: " + tile.itemsOnFloor.toString());
				}
				else {
					texts.add("No items on floor");
				}
				
				if (tile.building == null) {
					texts.add("Building: null");
				} else {
					texts.add("Building: " + tile.building.name);
					String buildingBlocks = "Building Parts: ";
					for (int id: tile.building.buildingBlockIds) {
						buildingBlocks += ItemData.getNameFromId(id) + " /";
					}
					texts.add(buildingBlocks);
					
					if (tile.building.inventory.size() > 0) {
						String allItems = "Building Items: " + tile.building.inventory.toUniqueItemsMap();
						List<String> itemsWordWrapped = MapUtil.wrapString(allItems, 40);
						for (String line: itemsWordWrapped)
							texts.add(line);
					} else {
						texts.add("No building items");
					}
				}
				
				if (tile.getPeople() == null || tile.getPeople().size() == 0) {
					texts.add("No people");
				} else {
					for (LivingEntity being: tile.getPeople()) {
						if (being.inventory.size() > 0) {
							texts.add(being.name);
							List<String> humanInvWrapped = MapUtil.wrapString(being.inventory.toString(), 40);
							for (String text: humanInvWrapped) 
								texts.add(text);
						} else {
							texts.add(being.name + ", no inventory / ");
						}
						
						//texts.add("\n");
						texts.add(" ");
						texts.add("Process: " + (being.processProgress != null ? being.processProgress.name : null));
						if (being.jobProcessProgress != null) {
							texts.add("Job: " + being.jobProcessProgress.jobWorkProcess.name + 
									", under boss: " + being.jobProcessProgress.boss.name);
						}
						if (being.processBuilding != null)
							texts.add("Used Proc Build: " + being.processBuilding.name + ", at " + being.processBuilding.getPrimaryLocation());
						
						if (being.targetTile != null) {
							texts.add("Used Proc Tile (Target): " + ItemData.getNameFromId(being.targetTile.tileBlockId) + ", at " + being.targetTile.coords);
						}
						else if (being.processTile != null) {
							texts.add("Used Proc Tile: " + ItemData.getNameFromId(being.processTile.tileBlockId) + ", at " + being.processTile.coords);
						}
						
						if (being.activePriority != null)
							texts.add("Current Priority: " + being.activePriority.getClass().getSimpleName());
						
						if (being.currentQueueTasks != null)
							texts.add("Current Task: " + being.currentQueueTasks.toString()); 
					}
				}
			}
		}
		textboxes.get("TileHint").setDisplayText(texts);
	}

	public void tick() {
		menuActivated = false; // Update as default false, i.e. user clicks should propogate, and then
								// determine if the user clicked on a menu
		menuHighlighted = false;
		for (int menu = 0; menu < menus.size(); menu++) {
			// if (!main.enabled) break;
			if (menus.get(menu).active()) {
				if (!menus.get(menu).noShortcuts) {
					//makeShortcut(menus.get(menu));
				}
				for (int i = clicks.size() - 1; i >= 0; i--) {
					Click click = clicks.get(i);
					String command = menus.get(menu).click(click);
					if (command != null && command.equals(ConstantData.MOUSE_HIGHLIGHT_NO_CLICK)) {
						menuHighlighted = true;
					} else if (command != null && !command.equals("")) {
						menuActivated = true;
						// Replace with function that returns true if the menu resetting should happen
						if (executeAction(command)) {
							forceFullUIUpdate();
						}
					}
				}
			}
		}
		clicks.clear();
	}

	// TODO: Possibly sort shortcuts; higher buttons get lower numbers for shortcuts
	/*
	public void makeShortcut(Menu menu) {
		int iter = 1;
		for (int i = 0; i < menu.buttons.size(); i++) {
			TextBox b = menu.buttons.get(i);
			if (b instanceof Button && b.shortcut) {
				shortcuts[iter] = (Button) b;
				if (iter == 9) // Loop from 1 to 9 to 0 for shortcut keys
					iter = 0;
				else if (iter == 0)
					break;
				else
					iter++;
			}
		}
	}
	*/

	public void forceFullUIUpdate() {
		//System.out.println("force update");
		updateMenus();
		gameLauncher.guiSystem.updateUI();
		TextMaster.update(this, gameLauncher.guiSystem);
		gameLauncher.lwjglSystem.renderer.guiRenderer.update(this, gameLauncher.guiSystem);
	}

	public class Click {
		public float mouseX, mouseY;
		public boolean isActualClick; // as opposed to a hover or 'mouse pass'

		public Click(boolean click, float x, float y) {
			this.isActualClick = click;
			mouseX = x;
			mouseY = y;
		}
	}

	public void queueClick(float mouseX, float mouseY) {
		clicks.add(0, new Click(true, mouseX, mouseY));
	}

	public void queueMousePass(float mouseX, float mouseY) {
		clicks.add(0, new Click(false, mouseX, mouseY));
	}

	/*
	 * Returns true if the command was ineffective or not successful
	 */
	public boolean executeAction(String command) {
		return true;
	}

	public void executeShortcut(int n) {
		if (shortcuts[n] != null) {
			if (executeAction(shortcuts[n].command)) {
				forceFullUIUpdate();
			}
		}
	}

	public void closeMenus() {

	}

	public TextBox findButtonWithin(float mouseX, float mouseY) {
		for (int i = 0; i < menus.size(); i++) {
			Menu m = menus.get(i);
			if (m.active()) {
				for (int j = 0; j < m.buttons.size(); j++) {
					TextBox b = m.within(mouseX, mouseY);
					if (b != null)
						return b;
				}
			}
		}
		return null;
	}
}
