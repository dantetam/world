package io.github.dantetam.render;

import java.util.ArrayList;

import org.lwjgl.util.vector.Vector2f;

import io.github.dantetam.localdata.ConstantData;
import io.github.dantetam.lwjglEngine.fontMeshCreator.FontType;
import io.github.dantetam.system.MenuSystem.Click;

public class Menu {

	public ArrayList<TextBox> buttons;
	public String name;
	private boolean active;
	public boolean noShortcuts = false;
	public boolean clickOnly = true;

	public Menu(String name) {
		this.name = name;
		buttons = new ArrayList<TextBox>();
		active = false;
	}

	/*
	 * public TextBox addButton(String command, String display, String tooltip,
	 * float a, float b, float c, float d) { Button temp = new
	 * Button(12,null,display.length(),true,-1,command,display,tooltip,a,b,c,d);
	 * temp.menu = this; buttons.add(temp); return temp; }
	 */

	public TextBox addButton(int texture, String command, String display, String tooltip, float a, float b, float c,
			float d) {
		Button temp = new Button(12, null, display.length(), true, texture, command, display, tooltip, a, b, c, d);
		temp.menu = this;
		buttons.add(temp);
		return temp;
	}

	public TextBox addButton(int w, FontType x, int y, boolean z, int texture, String command, String display,
			String tooltip, float a, float b, float c, float d) {
		Button temp = new Button(w, x, y, z, texture, command, display, tooltip, a, b, c, d);
		temp.menu = this;
		buttons.add(temp);
		return temp;
	}

	public TextBox addButton(TextBox temp) {
		temp.menu = this;
		buttons.add(temp);
		return temp;
	}

	/*
	 * public void addButton(String command, ArrayList<String> display, String
	 * tooltip, float a, float b, float c, float d, int... n) { buttons.add(new
	 * TextBox(command,display,tooltip,a,b,c,d)); }
	 */

	public TextBox findButtonByCommand(String name) {
		for (int i = 0; i < buttons.size(); i++) {
			TextBox b = buttons.get(i);
			if (b instanceof Button)
				if (((Button) b).command.equals(name))
					return buttons.get(i);
		}
		return null;
	}

	public TextBox findButtonByName(String name) {
		for (int i = 0; i < buttons.size(); i++)
			if (buttons.get(i).name.equals(name))
				return buttons.get(i);
		return null;
	}

	public String click(boolean isActualClick, float mouseX, float mouseY) {
		//System.out.println("-----------------");
		//System.out.println(buttons.size());
		for (TextBox b : buttons) {
			/*
			System.out.println(b.getDisplay().get(0) + "; Pos: " + b.pixelPos + "; Size: " + b.pixelSize
					+ "; Bounding Box Edge: " + new Vector2f(b.pixelPos.x + b.pixelSize.x, b.pixelPos.y + b.pixelSize.y)
					+ "; Mouse: " + mouseX + "," + mouseY);
					*/
			if (b instanceof Button)
				if (b.within(mouseX, mouseY)) { // mouseX > b.pos.x && mouseX < b.pos.x+b.size.x && mouseY > b.pos.y &&
												// mouseY < b.pos.y+b.size.y
					if (isActualClick || (!clickOnly)) {
						return ((Button) b).command;
					}
				}
		}
		return null;
	}
	
	public String click(Click click) {
		for (TextBox b : buttons) {
			if (b instanceof Button)
				if (b.within(click.mouseX, click.mouseY)) { 
					if (click.isActualClick || (!clickOnly)) {
						return ((Button) b).command;
					}
					else {
						return ConstantData.MOUSE_HIGHLIGHT_NO_CLICK;
					}
				}
		}
		return null;
	}

	public TextBox within(float mouseX, float mouseY) {
		for (int i = 0; i < buttons.size(); i++) {
			TextBox b = buttons.get(i);
			if (b.within(mouseX, mouseY))
				return b;
		}
		return null;
	}

	public void pass(boolean[] activeMenus, float mouseX, float mouseY) {
		for (int i = 0; i < buttons.size(); i++) {
			TextBox b = buttons.get(i);
			boolean skip = false;
			if (b.noOrdersIfMenu != null) // Check if all the menus that can stop the button from acting are not active
				for (int j = 0; j < b.noOrdersIfMenu.length; j++)
					if (activeMenus[b.noOrdersIfMenu[j]]) {
						skip = true;
						break;
					}
		}
	}

	public boolean equals(Menu other) {
		return name.equals(other.name);
	}

	public boolean requestUpdate = false;

	public void activate(boolean yn) {
		System.out.println("Menu " + name + " was updated to be active: " + yn);
		active = yn;
		if (yn)
			requestUpdate = true;
	}

	public boolean active() {
		return active;
	}

}
