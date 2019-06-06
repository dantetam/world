package io.github.dantetam.system;

import java.util.ArrayList;
import java.util.HashMap;

import org.lwjgl.glfw.GLFW;

import io.github.dantetam.render.GameLauncher;

public class InputSystem extends BaseSystem {

	private ArrayList<Integer> keyPresses;
	public HashMap<Integer, String> keyPressBinds = new HashMap<Integer, String>();
	public HashMap<Character, String> keyHoldBinds = new HashMap<Character, String>();

	public boolean moving = false;
	public boolean lastMoving = false;

	public boolean on = true;

	public enum KeyPressBind {
		ADVANCE_TURN(GLFW.GLFW_KEY_ENTER, 0), ADVANCE_MULTIPLE_TURNS(GLFW.GLFW_KEY_RIGHT_ALT, 0), 
		PAUSE_UNPAUSE(GLFW.GLFW_KEY_SPACE, 0),
		TOGGLE_MINIMAP(GLFW.GLFW_KEY_M), TOGGLE_FOG(GLFW.GLFW_KEY_R),
		TOGGLE_TACTICAL(GLFW.GLFW_KEY_T), ZOOM_IN(GLFW.GLFW_KEY_I), ZOOM_OUT(GLFW.GLFW_KEY_O),
		CLOSE_ALL(GLFW.GLFW_KEY_X), FUNCTION_1(GLFW.GLFW_KEY_F1, GLFW.GLFW_KEY_1),
		FUNCTION_2(GLFW.GLFW_KEY_F2, GLFW.GLFW_KEY_2), FUNCTION_3(GLFW.GLFW_KEY_F3, GLFW.GLFW_KEY_3),
		FUNCTION_4(GLFW.GLFW_KEY_F4, GLFW.GLFW_KEY_4), FUNCTION_5(GLFW.GLFW_KEY_F5, GLFW.GLFW_KEY_5),
		FUNCTION_6(GLFW.GLFW_KEY_F6, GLFW.GLFW_KEY_6), FUNCTION_7(GLFW.GLFW_KEY_F7, GLFW.GLFW_KEY_7),
		FUNCTION_8(GLFW.GLFW_KEY_F8, GLFW.GLFW_KEY_8), FUNCTION_9(GLFW.GLFW_KEY_F9, GLFW.GLFW_KEY_9),
		FUNCTION_0(GLFW.GLFW_KEY_F10, GLFW.GLFW_KEY_0), CONSOLE('`', '~'),
		/*
		 * CONSOLE ('`', '~'), FUNCTION_1 ('1', 131), FUNCTION_2 ('2', 132), FUNCTION_3
		 * ('3', 133), FUNCTION_4 ('4', 134), FUNCTION_5 ('5', 135), FUNCTION_6 ('6',
		 * 136), FUNCTION_7 ('7', 137), FUNCTION_8 ('8', 138), FUNCTION_9 ('9', 139),
		 * FUNCTION_0 ('0', 140), TOGGLE_KEY_MENU (9, 0),
		 */
		;
		private KeyPressBind(char k1, char k2) {
			key1 = k1;
			key2 = k2;
		}

		private KeyPressBind(char k1) {
			key1 = k1;
			key2 = (char) 0;
		}

		private KeyPressBind(int k1) {
			key1 = k1;
			key2 = (char) 0;
		}

		private KeyPressBind(int k1, int k2) {
			key1 = (char) k1;
			key2 = (char) k2;
		}

		private KeyPressBind(char k1, int k2) {
			key1 = k1;
			key2 = (char) k2;
		}

		public int key1, key2;
	}

	public enum KeyHoldBind {
		PAN_LEFT('a'), PAN_RIGHT('d'), PAN_UP('w'), PAN_DOWN('s'),;
		private KeyHoldBind(char k1, char k2) {
			key1 = k1;
			key2 = k2;
		}

		private KeyHoldBind(char k1) {
			key1 = k1;
			key2 = (char) 0;
		}

		private KeyHoldBind(int k1, int k2) {
			key1 = (char) k1;
			key2 = (char) k2;
		}

		private KeyHoldBind(char k1, int k2) {
			key1 = k1;
			key2 = (char) k2;
		}

		public char key1, key2;
	}

	public InputSystem(GameLauncher main) {
		super(main);
		keyPresses = new ArrayList<Integer>();
		setKeyBinds();
	}

	public void setKeyBinds() {
		keyPressBinds.clear();
		keyHoldBinds.clear(); // reset any old key bindings
		for (KeyPressBind kb : KeyPressBind.values()) {
			keyPressBinds.put(kb.key1, kb.toString());
			if (kb.key2 != (char) 0)
				keyPressBinds.put(kb.key2, kb.toString());
		}
		/*
		 * for (KeyHoldBind kb: KeyHoldBind.values()) { keyHoldBinds.put(kb.key1,
		 * kb.toString()); if (kb.key2 != (char)0) keyHoldBinds.put(kb.key2,
		 * kb.toString()); }
		 */
	}

	// Goes through keys backwards to avoid arraylist trap
	public void tick() {
		moving = false;
		for (int i = keyPresses.size() - 1; i >= 0; i--) {
			executeAction(keyPresses.get(i));
			keyPresses.remove(i);
		}
		
		moving = gameLauncher.camera.move();

		if (moving || lastMoving) {
			gameLauncher.menuSystem.forceFullUIUpdate();
		}

		lastMoving = moving;
		if (gameLauncher.menuSystem.menuActivated) {
			clicks.clear();
			// main.menuSystem.menuActivated = false;
		} else { // Process the clicks remaining in the queue
			for (int i = clicks.size() - 1; i >= 0; i--) {
				Click c = clicks.get(i);
				if (c.type.equals("Left")) {
					processLeftMouseClick(c.mouseX, c.mouseY);
				} else if (c.type.equals("Right")) {
					processRightMouseClick(c.mouseX, c.mouseY);
				}
				clicks.remove(i);
			}
		}
	}

	// Stores which keys are being held (such as panning with WASD)
	public boolean[] keyHeld = new boolean[200];
	/*
	 * public void queueKey(char key) { if (key >= 97 && key <= 122) {
	 * keyHeld[key-97] = true; } keyPresses.add(0,key); }
	 */

	public void keyPressed(int key) {
		keyPresses.add(0, key);
	}

	public void keyReleased(char key) {
		if (key >= 97 && key <= 122) {
			keyHeld[key - 97] = false;
		}
	}

	public ArrayList<Click> clicks = new ArrayList<Click>();

	public class Click {
		String type;
		float mouseX, mouseY;

		Click(String t, float x, float y) {
			type = t;
			mouseX = x;
			mouseY = y;
		}
	}

	public void queueLeftClick(float mouseX, float mouseY) {
		clicks.add(0, new Click("Left", mouseX, mouseY));
	}

	public void queueRightClick(float mouseX, float mouseY) {
		clicks.add(0, new Click("Right", mouseX, mouseY));
	}

	// Make a system to cycle through units on a list
	// private ArrayList<GameEntity> lastList = null;
	// private int num = 0;
	public void processLeftMouseClick(float mouseX, float mouseY) {

	}

	public void processRightMouseClick(float mouseX, float mouseY) {

	}

	public void executeAction(String action) {
		System.out.println("InputSystem executed " + action);

		if (action.equals("ADVANCE_TURN")) {
			System.out.println("##### End turn #####");
			super.gameLauncher.worldGrid.tick();
		}
		else if (action.equals("ADVANCE_MULTIPLE_TURNS")) {
			System.out.println("##### End 10 turns #####");
			for (int i = 0; i < 10; i++)
				super.gameLauncher.worldGrid.tick();
		}
		else if (action.equals("PAUSE_UNPAUSE")) {
			System.out.println("##### Toggle game process #####");
				super.gameLauncher.worldGrid.currentlyTicking = 
						!super.gameLauncher.worldGrid.currentlyTicking;
		}
		
		gameLauncher.menuSystem.forceFullUIUpdate();
	}

	public void executeAction(int key) {
		String action = keyPressBinds.get(key);
		if (action == null)
			return;
		executeAction(action);
	}

}
