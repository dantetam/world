package io.github.dantetam.system;

import java.util.ArrayList;
import java.util.List;

import io.github.dantetam.localdata.ConstantData;
import io.github.dantetam.lwjglEngine.fontRendering.TextMaster;
import io.github.dantetam.lwjglEngine.render.VBOLoader;
import io.github.dantetam.render.Button;
import io.github.dantetam.render.GameLauncher;
import io.github.dantetam.render.Menu;
import io.github.dantetam.render.TextBox;

public class MenuSystem extends BaseSystem {

	public List<Menu> menus;
	public List<TextBox> textboxes;

	private List<Click> clicks;

	// The menuActivated field is updated whenever a user clicks on any active menu.
	// This signals to systems after, that the user's input should be restricted to
	// menus only.
	public boolean menuActivated = false, menuHighlighted = false;

	private int guiDefaultTexture;

	public Button[] shortcuts = new Button[10];

	// Done in this function ideally after GL context has been set up
	public void setupLoader() {
		guiDefaultTexture = VBOLoader.loadTexture("guiDefaultTexture");
	}

	public MenuSystem(GameLauncher civGame) {
		super(civGame);
		menus = new ArrayList<Menu>();
		textboxes = new ArrayList<TextBox>();
		clicks = new ArrayList<Click>();
	}

	public void setupMenus() {

	}

	public void tick() {
		menuActivated = false; // Update as default false, i.e. user clicks should propogate, and then
								// determine if the user clicked on a menu
		menuHighlighted = false;
		for (int menu = 0; menu < menus.size(); menu++) {
			// if (!main.enabled) break;
			if (menus.get(menu).active()) {
				if (!menus.get(menu).noShortcuts) {
					makeShortcut(menus.get(menu));
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

	public void displayMenu(int menu) {

	}

	public void forceFullUIUpdate() {
		System.out.println("force update");
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
