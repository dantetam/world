package io.github.dantetam.lwjglEngine.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Matrix4f;

import io.github.dantetam.lwjglEngine.entities.*;
import io.github.dantetam.lwjglEngine.fontRendering.TextMaster;
import io.github.dantetam.lwjglEngine.gui.GuiRenderer;
import io.github.dantetam.lwjglEngine.models.TexturedModel;
import io.github.dantetam.lwjglEngine.shaders.ShaderProgram;
import io.github.dantetam.lwjglEngine.shaders.StaticShader;
import io.github.dantetam.lwjglEngine.terrain.Terrain;
import io.github.dantetam.lwjglEngine.toolbox.MousePicker;
import io.github.dantetam.system.MenuSystem;

public class MasterRenderer {

	private StaticShader shader = new StaticShader();
	public GuiRenderer guiRenderer;
	//private TextMaster textMaster;

	public Matrix4f projectionMatrix;

	public MasterRenderer() 
	{	
		//Back culling; do not render faces that are hidden from camera
		enableCulling();

		//Create the transformation matrix only once and parse it to other renderers
		createProjectionMatrix();

		guiRenderer = new GuiRenderer();
		//textMaster = new TextMaster();
		//TextMaster.init(loader);
		GL11.glDisable(GL11.GL_CULL_FACE);
	}

	public static void enableCulling()
	{
		//Perhaps checking the culling would be a nice way to find the correct normals
		/*GL11.glEnable(GL11.GL_CULL_FACE); 
		GL11.glCullFace(GL11.GL_BACK);*/
	}

	public static void disableCulling()
	{
		//GL11.glDisable(GL11.GL_CULL_FACE);
	}

	//Return a new MousePicker 
	public MousePicker setupMousePicker(Camera c)
	{
		MousePicker temp = new MousePicker(projectionMatrix, c);
		return temp;
	}

	public void render(Light light, Camera camera, MousePicker mp)
	{
		prepare();

		shader.start();
		shader.loadLight(light);
		shader.loadViewMatrix(camera);
		//renderer.render(entities);
		shader.stop();
	}

	public void prepare()
	{
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		GL11.glClearColor(150F/255F,225F/255F,255F/255F,0);
	}

	public void cleanUp()
	{
		shader.cleanUp();
		guiRenderer.cleanUp();
	}

	private static final float FOV = 70, NEAR_PLANE = 0.1f, FAR_PLANE = 3000f;
	private void createProjectionMatrix()
	{
		float ar = (float)DisplayManager.width/(float)DisplayManager.height;
		float yScale = (float)(1f/Math.tan(Math.toRadians(FOV/2f)))*ar;
		float xScale = yScale/ar;
		float frustumLength = FAR_PLANE - NEAR_PLANE;

		//Set up the projection matrix by declaring discrete values
		//These values are calculated by matrix math
		projectionMatrix = new Matrix4f(); //Initialized to zeroes, not identity
		projectionMatrix.m00 = xScale;
		projectionMatrix.m11 = yScale;
		projectionMatrix.m22 = (FAR_PLANE + NEAR_PLANE)/-frustumLength;
		projectionMatrix.m23 = -1;
		projectionMatrix.m32 = -(2*FAR_PLANE*NEAR_PLANE / frustumLength);
		projectionMatrix.m33 = 0;
	}

}
