package io.github.dantetam.lwjglEngine.tests;

import org.lwjgl.util.vector.Vector3f;

import io.github.dantetam.lwjglEngine.entities.Light;
import io.github.dantetam.lwjglEngine.fontRendering.TextMaster;
import io.github.dantetam.lwjglEngine.render.DisplayManager;
import io.github.dantetam.lwjglEngine.render.MasterRenderer;
import io.github.dantetam.lwjglEngine.render.VBOLoader;
import io.github.dantetam.lwjglEngine.toolbox.MousePicker;
import io.github.dantetam.render.GameLauncher;

public class MainGameLoop {

	public GameLauncher main;
	private boolean stopGameLoop;

	public int frameCount = 0;

	private Light light;
	public MousePicker mousePicker;
	public MasterRenderer renderer;

	public MainGameLoop(GameLauncher game) {
		try {
			main = game;
			main.lwjglSystem = this;

			TextMaster.init();
			main.menuSystem.setupMenus(); // Set up menus once loader is not null

			light = new Light(new Vector3f(500, 500, 500), new Vector3f(1, 1, 1));

			// Keep updating the display until the user exits
			renderer = new MasterRenderer();
			mousePicker = renderer.setupMousePicker(main.camera);
			main.renderSystem.mousePicker = mousePicker;
			main.guiSystem.mousePicker = mousePicker;

			main.menuSystem.forceFullUIUpdate(); // Set everything up correctly initially

			runGameFrames();

			stop();

		} catch (Exception e) {
			e.printStackTrace();
		} // LWJGL seems to not catch errors for some reason
			// Probably has to do with the fact that's as close to C++ as possible
	}

	public void stop() {
		// TODO: Remember to stop
		// Do some clean up of all data
		TextMaster.cleanUp();
		renderer.cleanUp();
		VBOLoader.cleanData();
		DisplayManager.closeDisplay();
	}

	public void runGameFrames() {
		while (true) {
			if (DisplayManager.requestClose()) {
				stopGameLoop = true;
				break;
			}
			if (stopGameLoop)
				break;

			for (int i = 0; i < main.systems.size(); i++) {
				main.systems.get(i).tick();
			}

			renderer.render(light, main.camera, mousePicker);
			renderer.guiRenderer.render();

			TextMaster.render();

			DisplayManager.updateDisplay();
			frameCount++;
		}
	}

}
