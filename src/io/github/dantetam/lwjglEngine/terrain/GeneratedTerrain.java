package io.github.dantetam.lwjglEngine.terrain;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import io.github.dantetam.lwjglEngine.models.RawModel;
import io.github.dantetam.lwjglEngine.render.VBOLoader;
import io.github.dantetam.lwjglEngine.textures.TerrainTexture;
import io.github.dantetam.lwjglEngine.textures.TerrainTexturePack;
import io.github.dantetam.toolbox.BicubicInterpolator;

public class GeneratedTerrain extends Terrain {

	public TerrainTexture territoryOwnerBlendMap;

	public GeneratedTerrain(int gridX, int gridZ, TerrainTexturePack texturePack, TerrainTexture blendMap,
			TerrainTexture blendMap2, double[][] heightMap) {
		super(gridX, gridZ, texturePack, blendMap, null);
		this.territoryOwnerBlendMap = blendMap2;
		super.model = generateTerrain(heightMap, 1);
	}

	protected RawModel generateTerrain(double[][] terrain, float multiply) {
		int VERTEX_COUNT = terrain.length * (int) multiply;

		int count = VERTEX_COUNT * VERTEX_COUNT;
		float[] vertices = new float[count * 3];
		float[] normals = new float[count * 3];
		float[] textureCoords = new float[count * 2];
		int[] indices = new int[6 * (VERTEX_COUNT - 1) * (VERTEX_COUNT * 1)];
		int vertexPointer = 0;
		for (int i = 0; i < VERTEX_COUNT; i++) {
			for (int j = 0; j < VERTEX_COUNT; j++) {
				vertices[vertexPointer * 3] = (float) j / ((float) VERTEX_COUNT - 1) * SIZE;
				vertices[vertexPointer * 3 + 1] = getTerrainHeight(terrain,
						(float) (j + (j % multiply)) / (float) multiply,
						(float) (i + (i % multiply)) / (float) multiply) * 2;
				vertices[vertexPointer * 3 + 2] = (float) i / ((float) VERTEX_COUNT - 1) * SIZE;
				Vector3f normal = calculateNormal(terrain, j, i);
				normals[vertexPointer * 3] = normal.x;
				normals[vertexPointer * 3 + 1] = normal.y;
				normals[vertexPointer * 3 + 2] = normal.z;
				textureCoords[vertexPointer * 2] = (float) j / ((float) VERTEX_COUNT - 1);
				textureCoords[vertexPointer * 2 + 1] = (float) i / ((float) VERTEX_COUNT - 1);
				vertexPointer++;
			}
		}
		int pointer = 0;
		for (int gz = 0; gz < VERTEX_COUNT - 1; gz++) {
			for (int gx = 0; gx < VERTEX_COUNT - 1; gx++) {
				int topLeft = (gz * VERTEX_COUNT) + gx;
				int topRight = topLeft + 1;
				int bottomLeft = ((gz + 1) * VERTEX_COUNT) + gx;
				int bottomRight = bottomLeft + 1;
				indices[pointer++] = topLeft;
				indices[pointer++] = bottomLeft;
				indices[pointer++] = topRight;
				indices[pointer++] = topRight;
				indices[pointer++] = bottomLeft;
				indices[pointer++] = bottomRight;
			}
		}
		return VBOLoader.loadToVAO(vertices, textureCoords, normals, indices);
	}

	protected RawModel generateTerrain(Vector2f[][] terrain, float multiply) {
		int VERTEX_COUNT = terrain.length;

		int count = VERTEX_COUNT * VERTEX_COUNT;
		float[] vertices = new float[count * 3];
		float[] normals = new float[count * 3];
		float[] textureCoords = new float[count * 2];
		int[] indices = new int[6 * (VERTEX_COUNT - 1) * (VERTEX_COUNT * 1)];
		int vertexPointer = 0;
		for (int i = 0; i < VERTEX_COUNT; i++) {
			for (int j = 0; j < VERTEX_COUNT; j++) {
				vertices[vertexPointer * 3] = (float) j / ((float) VERTEX_COUNT - 1) * SIZE; // (float)terrain[j][i].x;
				vertices[vertexPointer * 3 + 1] = getTerrainHeight(terrain, j, i);
				vertices[vertexPointer * 3 + 2] = (float) j / ((float) VERTEX_COUNT - 1) * SIZE; // (float)terrain[j][i].z;
				Vector3f normal = calculateNormal(terrain, j, i);
				normals[vertexPointer * 3] = normal.x;
				normals[vertexPointer * 3 + 1] = normal.y;
				normals[vertexPointer * 3 + 2] = normal.z;
				textureCoords[vertexPointer * 2] = (float) j / ((float) VERTEX_COUNT - 1);
				textureCoords[vertexPointer * 2 + 1] = (float) i / ((float) VERTEX_COUNT - 1);
				vertexPointer++;
			}
		}
		int pointer = 0;
		for (int gz = 0; gz < VERTEX_COUNT - 1; gz++) {
			for (int gx = 0; gx < VERTEX_COUNT - 1; gx++) {
				int topLeft = (gz * VERTEX_COUNT) + gx;
				int topRight = topLeft + 1;
				int bottomLeft = ((gz + 1) * VERTEX_COUNT) + gx;
				int bottomRight = bottomLeft + 1;
				indices[pointer++] = topLeft;
				indices[pointer++] = bottomLeft;
				indices[pointer++] = topRight;
				indices[pointer++] = topRight;
				indices[pointer++] = bottomLeft;
				indices[pointer++] = bottomRight;
			}
		}
		return VBOLoader.loadToVAO(vertices, textureCoords, normals, indices);
	}

	protected Vector3f calculateNormal(double[][] terrain, double x, double y) {
		float hl = getTerrainHeight(terrain, x - 1, y);
		float hr = getTerrainHeight(terrain, x + 1, y);
		float hd = getTerrainHeight(terrain, x, y - 1);
		float hu = getTerrainHeight(terrain, x, y + 1);
		Vector3f normal = new Vector3f(hl - hr, 2, hd - hu);
		normal.normalise();
		return normal;
	}

	protected Vector3f calculateNormal(Vector2f[][] points, double x, double y) {
		float hl = getTerrainHeight(points, x - 1, y);
		float hr = getTerrainHeight(points, x + 1, y);
		float hd = getTerrainHeight(points, x, y - 1);
		float hu = getTerrainHeight(points, x, y + 1);
		Vector3f normal = new Vector3f(hl - hr, 2, hd - hu);
		normal.normalise();
		return normal;
	}

	private BicubicInterpolator inter = new BicubicInterpolator();

	private float getTerrainHeight(double[][] terrain, double r, double c) {
		if (r < 0 || r >= terrain.length || c < 0 || c >= terrain.length)
			return 0;
		return (float) inter.getValue(terrain, r, c);
	}

	private float getTerrainHeight(Vector2f[][] points, double r, double c) {
		if (r < 0 || r >= points.length || c < 0 || c >= points[0].length)
			return 0;
		return (float) points[(int) r][(int) c].y;
	}

}
