package io.github.dantetam.lwjglEngine.fontRendering;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import io.github.dantetam.lwjglEngine.fontMesh.FontType;
import io.github.dantetam.lwjglEngine.fontMesh.TextMeshData;
import io.github.dantetam.lwjglEngine.render.VBOLoader;
import io.github.dantetam.render.Menu;
import io.github.dantetam.render.TextBox;
import io.github.dantetam.system.GuiSystem;
import io.github.dantetam.system.MenuSystem;

public class TextMaster {

	private static Map<FontType, List<TextBox>> texts = new HashMap<FontType, List<TextBox>>();
	private static FontRenderer renderer;

	public static FontType defaultFont;

	public static boolean init = false;

	public static void init() {
		if (!init) {
			init = true;
			renderer = new FontRenderer();
			defaultFont = new FontType(VBOLoader.loadTexture("fonts/dejavusans", false, false),
					new File("res/fonts/dejavusans.fnt"));
		}
	}

	public static void render() {
		renderer.render(texts);
	}

	public static void update(MenuSystem menuSystem, GuiSystem guiSystem) {
		for (int i = 0; i < menuSystem.menus.size(); i++) {
			for (int j = 0; j < menuSystem.menus.get(i).buttons.size(); j++) {
				TextBox text = menuSystem.menus.get(i).buttons.get(j);
				// if ((text.active || menuSystem.menus.get(i).active()) && text.textMeshVao <=
				// 0) //needs to be loaded and not already loaded
				if (menuSystem.menus.get(i).active() && text.textMeshVao <= 0) {
					// System.out.println("loading");
					loadText(text);
				}
				// else if ((!text.active && !menuSystem.menus.get(i).active()) &&
				// text.textMeshVao > 0) //needs to be unloaded and already loaded
				else if (!menuSystem.menus.get(i).active() && text.textMeshVao > 0) {
					// System.out.println("removing");
					removeText(text);
					text.textMeshVao = -1;
				}
			}
		}
		for (TextBox text : menuSystem.textboxes) {
			if (text.active && text.textMeshVao <= 0) {
				loadText(text);
			} else if (!text.active && text.textMeshVao > 0) {
				removeText(text);
				text.textMeshVao = -1;
			}
		}
		for (TextBox text : guiSystem.getAllTextUI()) {
			if (text.active && text.textMeshVao <= 0) {
				loadText(text);
			} else if (!text.active && text.textMeshVao > 0) {
				removeText(text);
				text.textMeshVao = -1;
			}
		}

		ArrayList<TextBox> allGuis = new ArrayList<TextBox>();
		for (Menu menu : menuSystem.menus)
			if (menu.active())
				for (TextBox textBox : menu.buttons)
					allGuis.add(textBox);
		for (TextBox textBox : menuSystem.textboxes)
			allGuis.add(textBox);
		for (TextBox textBox : guiSystem.getAllTextUI())
			allGuis.add(textBox);

		for (Entry<FontType, List<TextBox>> en : texts.entrySet()) {
			List<TextBox> guis = en.getValue();
			for (int i = guis.size() - 1; i >= 0; i--) // Backwards for arraylist trap
				if (!allGuis.contains(guis.get(i)))
					removeText(guis.get(i));
			// texts.put(en.getKey(), guis);
		}
	}

	// Lots of code duplication but this is the most simple way to avoid null
	// pointer because of free floating textbox
	/*
	 * public static void loadTextBox(TextBox text) { if ((text.active ||
	 * text.menu.active()) && text.textMeshVao <= 0) //needs to be loaded and not
	 * already loaded loadText(text); else if ((!text.active && !text.menu.active())
	 * && text.textMeshVao > 0) //needs to be unloaded and already loaded {
	 * removeText(text); text.textMeshVao = -1; } }
	 */

	public static void loadText(TextBox text) {
		if (text.font == null)
			text.font = defaultFont;

		/*
		 * if (text.lineMaxSize <= 1) { for (int i = 0; i < text.display.size(); i++) if
		 * (text.lineMaxSize < text.display.get(i).length()) text.lineMaxSize =
		 * text.display.get(i).length(); }
		 */
		TextMeshData data = text.font.loadText(text);
		// System.out.println(data.getVertexPositions() + " " +
		// data.getTextureCoords());
		int vao = VBOLoader.loadToVAO(data.getVertexPositions(), data.getTextureCoords());
		text.textMeshVao = vao;
		text.vertexCount = data.getVertexCount();
		List<TextBox> textBatch = texts.get(text.font);
		if (textBatch == null) {
			textBatch = new ArrayList<TextBox>();
			texts.put(text.font, textBatch);
		}
		textBatch.add(text);
	}

	public static void removeText(TextBox text) {
		List<TextBox> textBatch = texts.get(text.font);
		textBatch.remove(text);
		if (textBatch.isEmpty()) {
			texts.put(text.font, null);
		}
	}

	public static void cleanUp() {
		renderer.cleanUp();
	}

}
