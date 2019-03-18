package io.github.dantetam.lwjglEngine.gui;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

import io.github.dantetam.lwjglEngine.render.DisplayManager;
import io.github.dantetam.render.TextBox;

//Completely scrap t

public class GuiQuad {

	public int texture;
	public Vector2f pos, size;
	public Vector2f origPos;
	public Vector2f pixelPos, pixelSize;
	public boolean active = true;
	public Vector4f color = new Vector4f(0, 0, 0, 255);

	public GuiQuad(int t, Vector2f p, Vector2f s) {
		texture = t;
		pixelPos = p;
		pixelSize = s;
		origPos = p;
		pos = new Vector2f(p.x / DisplayManager.width, p.y / DisplayManager.height);
		size = new Vector2f(s.x / DisplayManager.width, s.y / DisplayManager.height);
		active = true;
	}

	public boolean within(float x, float y) {
		return x > pixelPos.x && x < pixelPos.x + pixelSize.x && y > pixelPos.y && y < pixelPos.y + pixelSize.y;
	}
	
	public void move(float x, float y) {
		pos.x = x / DisplayManager.width;
		pos.y = y / DisplayManager.height;
		origPos = new Vector2f(x, y);
		pixelPos.x = x;
		pixelPos.y = y;
	}
	
	public GuiQuad center() {
		this.move(pixelPos.x - pixelSize.x / 2, pixelPos.y - pixelSize.y / 2);
		return this;
	}

}
