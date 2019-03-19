package io.github.dantetam.system;

import java.util.ArrayList;
import java.util.List;

import io.github.dantetam.lwjglEngine.gui.GuiQuad;
import io.github.dantetam.lwjglEngine.render.VBOLoader;
import io.github.dantetam.lwjglEngine.toolbox.MousePicker;
import io.github.dantetam.render.Button;
import io.github.dantetam.render.GameLauncher;
import io.github.dantetam.render.TextBox;

public class GuiSystem extends BaseSystem {

	public MousePicker mousePicker;

	// private Map<City, List<GuiQuad>> cityUIPictures;

	private List<GuiQuad> allGuiQuad;
	private List<TextBox> allGuiText;

	private int guiDefaultTexture;

	// Done in this function ideally after GL context has been set up
	public void setupLoader() {
		guiDefaultTexture = VBOLoader.loadTexture("guiDefaultTexture");
	}

	public GuiSystem(GameLauncher civGame) {
		super(civGame);
		// cityUIPictures = new HashMap<>();
	}

	public void updateUI() {
		// cityUIPictures.entrySet().removeIf(entry -> entry.getKey().location == null);
		// Put stuff into cityUIPictures

		updateNonTextUI();
		updateTextUI();
	}

	public List<GuiQuad> getAllNonTextUI() {
		if (allGuiQuad == null) {
			updateNonTextUI();
		}
		return allGuiQuad;
	}

	public List<TextBox> getAllTextUI() {
		if (allGuiText == null) {
			updateTextUI();
		}
		return allGuiText;
	}

	private void updateNonTextUI() {
		List<GuiQuad> listGuiQuad = new ArrayList<>();
		// cityUIPictures
		this.allGuiQuad = listGuiQuad;
	}

	private void updateTextUI() {
		List<TextBox> listTextBox = new ArrayList<>();

		this.allGuiText = listTextBox;
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
		if (main.inputSystem.lastMoving || main.inputSystem.moving) {
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
