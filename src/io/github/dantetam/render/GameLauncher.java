package io.github.dantetam.render;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import io.github.dantetam.localdata.ConstantData;
import io.github.dantetam.lwjglEngine.entities.Gui2DCamera;
import io.github.dantetam.lwjglEngine.render.DisplayManager;
import io.github.dantetam.lwjglEngine.tests.MainGameLoop;
import io.github.dantetam.system.*;
import io.github.dantetam.toolbox.StringUtil;
import io.github.dantetam.vector.Vector2i;
import io.github.dantetam.world.civilization.SkillBook;
import io.github.dantetam.world.dataparse.WorldCsvParser;
import io.github.dantetam.world.grid.LocalGrid;
import io.github.dantetam.world.grid.WorldGrid;
import io.github.dantetam.world.worldgen.WorldGridInstantiate;

public class GameLauncher {

	public List<BaseSystem> systems;
	// private RenderSystem renderSystem = new RenderSystem(this);
	public MainGameLoop lwjglSystem;
	public Gui2DCamera camera;

	public static float[] screenXValues = { 768, 960, 1280, 1366, 1600, 1920, 2304, 2560, 3200, 3840, 4096 };
	public static float[] screenYValues = { 432, 540, 720, 768, 900, 1080, 1296, 1440, 1800, 2160, 2304 };
	public static int screenResolutionIndex = 5;
	public static float WIDTH = screenXValues[screenResolutionIndex], HEIGHT = screenYValues[screenResolutionIndex];
	public static float centerX = WIDTH / 2, centerY = HEIGHT / 2; // for rendering purposes, to determine how the
																	// position of the mouse affects the camera

	public WorldGrid worldGrid;
	public LocalGrid activeLocalGrid;
	
	public MenuSystem menuSystem = new MenuSystem(this);
	public InputSystem inputSystem = new InputSystem(this);
	public CivilizationSystem civilizationSystem = new CivilizationSystem(this);
	public RenderSystem renderSystem = new RenderSystem(this);
	public GuiSystem guiSystem = new GuiSystem(this);

	public GameLauncher() {
		systems = new ArrayList<>();
		systems.add(civilizationSystem);
		systems.add(menuSystem);
		systems.add(inputSystem);
		systems.add(guiSystem);
		systems.add(renderSystem);
		setup();
	}
	
	public static void main(String[] args) {
		GameLauncher newGame = new GameLauncher();
	}

	public void setup() {
		try {
			cycleScreenResolution(0);
			
			System.setProperty("org.lwjgl.librarypath", "lib/natives");

			DisplayManager.createDisplay(this);
			DisplayManager.main = this;
			// setMouseCallback();
			// setKeyCallback();
			GLFW.glfwShowWindow(DisplayManager.window);

			WorldCsvParser.init();
			SkillBook.init();
			StringUtil.init();
			
			worldGrid = new WorldGridInstantiate(ConstantData.WORLD_NUM_GRIDS).setupGrid();
			activeLocalGrid = worldGrid.getLocalGrid(new Vector2i(0, 0));
			
			menuSystem.setupLoader();
			guiSystem.setupLoader();

			camera = new Gui2DCamera();

			lwjglSystem = new MainGameLoop(this);

			inputSystem.on = false;
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static float[] cycleScreenResolution(int offset) {
		screenResolutionIndex = Math.floorMod(screenResolutionIndex + offset, screenXValues.length); 
		// Cyclical mod (i.e. no negatives)
		
		WIDTH = screenXValues[screenResolutionIndex];
		HEIGHT = screenYValues[screenResolutionIndex];
		centerX = WIDTH / 2;
		centerY = HEIGHT / 2; // for rendering purposes, to determine how the position of
								// the mouse affects the camera
		return new float[] { WIDTH, HEIGHT };
	}

}
