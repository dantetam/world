package io.github.dantetam.lwjglEngine.gui;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

import io.github.dantetam.lwjglEngine.models.RawModel;
import io.github.dantetam.lwjglEngine.render.DisplayManager;
import io.github.dantetam.lwjglEngine.render.VBOLoader;
import io.github.dantetam.render.Menu;
import io.github.dantetam.system.GuiSystem;
import io.github.dantetam.system.MenuSystem;
import io.github.dantetam.toolbox.MatrixMathUtil;

public class GuiRenderer {

	private final RawModel quad; // Same model, will be moved and scaled across screen
	private GuiShader shader;
	// private UnicodeFont unicodeFont;

	public List<GuiQuad> guisActive = new ArrayList<GuiQuad>();

	public GuiRenderer() {
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		float[] positions = { -1, 1, -1, -1, 1, 1, 1, -1 };
		quad = VBOLoader.loadToVAO(positions);
		shader = new GuiShader();
	}

	public void render() {
		shader.start();
		GL30.glBindVertexArray(quad.vaoID);
		GL20.glEnableVertexAttribArray(0);
		for (GuiQuad gui : guisActive) {
			GL13.glActiveTexture(GL13.GL_TEXTURE0);
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, gui.texture);
			Matrix4f matrix = MatrixMathUtil.createTransformationMatrix(
					normalize(new Vector2f(gui.pixelPos.x + gui.pixelSize.x / 2,
							DisplayManager.height - (gui.pixelPos.y + gui.pixelSize.y / 2))),
					normalizeSize(gui.pixelSize));
			shader.loadColor(
					new Vector4f(gui.color.x / 255f, gui.color.y / 255f, gui.color.z / 255f, gui.color.w / 255f));
			shader.loadTransformation(matrix);
			GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, quad.vertexCount);
		}
		GL20.glDisableVertexAttribArray(0);
		GL30.glBindVertexArray(0);
		shader.stop();
	}

	private Vector2f normalize(Vector2f v) {
		return new Vector2f(v.x * 2 / DisplayManager.width - 1, v.y * 2 / DisplayManager.height - 1);
	}

	private Vector2f normalizeSize(Vector2f v) {
		return new Vector2f(v.x / DisplayManager.width, v.y / DisplayManager.height);
	}

	public void update(MenuSystem menuSystem, GuiSystem guiSystem) {
		guisActive.clear();
		for (Menu menu : menuSystem.menus) {
			if (menu.active()) {
				for (GuiQuad gui : menu.buttons) {
					guisActive.add(gui);
				}
			}
		}
		for (GuiQuad gui : menuSystem.textboxes) {
			if (gui.active) {
				guisActive.add(gui);
			}
		}
		for (GuiQuad gui : guiSystem.getAllNonTextUI()) {
			if (gui.active) {
				guisActive.add(gui);
			}
		}
		for (GuiQuad gui : guiSystem.getAllTextUI()) { // Text elements must be processed too for the VAO texture id in
														// the background
			if (gui.active) {
				guisActive.add(gui);
			}
		}
	}

	public int getFontHeight() {
		return 12;
	}

	public int getLineOffset() {
		return 0;
	}

	public void cleanUp() {
		shader.cleanUp();
	}

}
