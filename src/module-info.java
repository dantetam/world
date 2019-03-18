/**
 * 
 */
/**
 * @author Dante
 *
 */
module world {
	requires lwjgl;
	requires lwjgl.util;
	requires power.voronoi.diagram;
	requires java.desktop;
	
	exports io.github.dantetam.localdata;
	exports io.github.dantetam.lwjglEngine.entities;
	exports io.github.dantetam.lwjglEngine.fontMeshCreator;
	exports io.github.dantetam.lwjglEngine.fontRendering;
	exports io.github.dantetam.lwjglEngine.gui;
	exports io.github.dantetam.lwjglEngine.models;
	exports io.github.dantetam.lwjglEngine.render;
	exports io.github.dantetam.lwjglEngine.shaders;
	exports io.github.dantetam.lwjglEngine.terrain;
	exports io.github.dantetam.lwjglEngine.tests;
	exports io.github.dantetam.lwjglEngine.textures;
	exports io.github.dantetam.lwjglEngine.toolbox;
	exports io.github.dantetam.render;
	exports io.github.dantetam.system;
}