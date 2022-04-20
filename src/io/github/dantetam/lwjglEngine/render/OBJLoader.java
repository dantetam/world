package io.github.dantetam.lwjglEngine.render;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import io.github.dantetam.lwjglEngine.entities.Entity;
import io.github.dantetam.lwjglEngine.entities.Group;
import io.github.dantetam.lwjglEngine.models.RawModel;
import io.github.dantetam.lwjglEngine.models.TexturedModel;
import io.github.dantetam.lwjglEngine.textures.LoadedIdTexture;
import io.github.dantetam.toolbox.log.CustomLog;
import io.github.dantetam.vector.CustomVector3f;

/**
 * OBJLoader parser intended for use on OBJ files in combination with MTL files.
 * The .obj files should contain simple geometry with vertices and triangles,
 * and should be in the same directory as the optional .mtl files and
 * accompanying assets.
 * 
 * @author Dante
 *
 */

public class OBJLoader {

	public static Group loadObjModelWithMaterial(String fullObjFileName) {
		File objFile = new File(fullObjFileName);
		File objDirectory = objFile.getParentFile();
		FileReader fr = null;
		try {
			fr = new FileReader(objFile);
		} catch (FileNotFoundException e) {
			CustomLog.outPrintln("Could not load material OBJ model from file: " + fullObjFileName);
			e.printStackTrace();
			return null;
		}

		BufferedReader objReaderFirstPass = new BufferedReader(fr);
		String line;
		File mtlFile = null;
		try {
			line = objReaderFirstPass.readLine();
			while (line != null) {
				String[] currentLine = line.split(" ");
				if (currentLine[0].equals("mtllib")) {
					mtlFile = new File(objDirectory.getPath() + "\\" + currentLine[1]);
				}
				line = objReaderFirstPass.readLine();
			}
			objReaderFirstPass.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (mtlFile == null) {
			throw new IllegalArgumentException("Obj contains a missing mtl file reference");
		}

		Map<String, String> basicMatLocalName = new HashMap<>();
		try {
			fr = new FileReader(mtlFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
		BufferedReader mtlReader = new BufferedReader(fr);
		try {
			line = mtlReader.readLine();
			String materialKey = null;
			while (line != null) {
				if (line.startsWith("newmtl ")) {
					materialKey = line.substring(7);
				} else if (line.startsWith("map_Kd ")) {
					basicMatLocalName.put(materialKey, line.substring(7));
					materialKey = null;
				}
				line = mtlReader.readLine();
			}
			mtlReader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			fr = new FileReader(objFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
		BufferedReader objReaderSecondPass = new BufferedReader(fr);
		List<Vector3f> vertices = new ArrayList<Vector3f>();
		List<Vector2f> textures = new ArrayList<Vector2f>();
		List<Vector3f> normals = new ArrayList<Vector3f>();
		List<Integer> indices = new ArrayList<Integer>();
		float[] verticesArray;
		List<Vector3f> singleModelNormals;
		List<Vector2f> singleModelTextures;

		List<Entity> allObjParts = new ArrayList<>();

		String currentMtlLocalFile = "";
		String currentObjGroup = null;
		try {
			while (true) {
				line = objReaderSecondPass.readLine();
				String[] currentLine = line.split(" ");
				if (line.startsWith("v ")) // vertex position
				{
					Vector3f vertex = new Vector3f(Float.parseFloat(currentLine[1]), Float.parseFloat(currentLine[2]),
							Float.parseFloat(currentLine[3]));
					vertices.add(vertex);
				} else if (line.startsWith("vt ")) // texture coordinate
				{
					Vector2f texture = new Vector2f(Float.parseFloat(currentLine[1]), Float.parseFloat(currentLine[2]));
					textures.add(texture);
				} else if (line.startsWith("vn ")) // normal
				{
					Vector3f vertex = new Vector3f(Float.parseFloat(currentLine[1]), Float.parseFloat(currentLine[2]),
							Float.parseFloat(currentLine[3]));
					normals.add(vertex);
				} else if (line.startsWith("f ") || line.startsWith("g ")) // face object
				{
					// All the v, vt, vn lines have been passed, end the loop
					singleModelTextures = new ArrayList<>();
					singleModelNormals = new ArrayList<>();
					if (line.startsWith("g ")) {
						currentObjGroup = line.substring(2);
					}
					break;
				}
			}

			CustomLog.outPrintln("------------------");
			for (Vector2f texture : textures) {
				CustomLog.outPrintln(texture.toString());
			}

			verticesArray = new float[vertices.size() * 3]; // Convert lists to array
			int vertexPointer = 0;
			for (Vector3f vertex : vertices) {
				verticesArray[vertexPointer++] = vertex.x;
				verticesArray[vertexPointer++] = vertex.y;
				verticesArray[vertexPointer++] = vertex.z;
			}

			while (true) {
				// Make sure a face line is being read
				line = objReaderSecondPass.readLine();
				if (line == null) {
					if (currentObjGroup != null) {
						String fullTextureName = objDirectory.getPath() + "\\" + currentMtlLocalFile;
						Entity objPartEntity = createNewEntityFromFloatData(verticesArray, singleModelTextures,
								singleModelNormals, indices, fullTextureName);
						allObjParts.add(objPartEntity);

						singleModelTextures.clear();
						singleModelNormals.clear();
						indices.clear();
					}
					break;
				}
				if (!line.startsWith("f ")) {
					if (line.startsWith("g ")) {
						if (currentObjGroup != null) {
							String fullTextureName = objDirectory.getPath() + "\\" + currentMtlLocalFile;
							Entity objPartEntity = createNewEntityFromFloatData(verticesArray, singleModelTextures,
									singleModelNormals, indices, fullTextureName);
							allObjParts.add(objPartEntity);

							singleModelTextures.clear();
							singleModelNormals.clear();
							indices.clear();
						}
						currentObjGroup = line.substring(2);
					} else if (line.startsWith("usemtl ")) {
						currentMtlLocalFile = basicMatLocalName.get(line.substring(7));
					}
				} else {
					// A face is in the from f x/y/z a/b/c d/e/f
					// Split into these 4 sections
					// and then split the sections by slashes to get the numbers x, y, z, etc.
					String[] currentLine = line.split(" ");
					String[] vertex1 = currentLine[1].split("/");
					String[] vertex2 = currentLine[2].split("/");
					String[] vertex3 = currentLine[3].split("/");

					processVertex(vertex1, indices, textures, normals, singleModelTextures, singleModelNormals);
					processVertex(vertex2, indices, textures, normals, singleModelTextures, singleModelNormals);
					processVertex(vertex3, indices, textures, normals, singleModelTextures, singleModelNormals);
				}
			}

			objReaderSecondPass.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		Group objGroup = new Group(allObjParts);
		return objGroup;
	}

	public static Entity createNewEntityFromFloatData(float[] verticesArray, List<Vector2f> singleModelTextures,
			List<Vector3f> singleModelNormals, List<Integer> indices, String fullTextureName) {
		int[] indicesArray = new int[indices.size()];
		for (int i = 0; i < indices.size(); i++) {
			indicesArray[i] = indices.get(i);
		}

		float[] texturesArray = new float[singleModelTextures.size() * 2];
		float[] normalsArray = new float[singleModelNormals.size() * 3];
		for (int i = 0; i < singleModelTextures.size(); i++) {
			texturesArray[i * 2] = singleModelTextures.get(i).x;
			texturesArray[i * 2 + 1] = singleModelTextures.get(i).y;
			normalsArray[i * 3] = singleModelNormals.get(i).x;
			normalsArray[i * 3 + 1] = singleModelNormals.get(i).y;
			normalsArray[i * 3 + 2] = singleModelNormals.get(i).z;
		}

		RawModel model = VBOLoader.loadToVAO(verticesArray, texturesArray, normalsArray, indicesArray);
		singleModelTextures.clear();
		singleModelNormals.clear();
		indices.clear();

		Entity objPartEntity = newObjectFromModel(new CustomVector3f(0), new CustomVector3f(0), new CustomVector3f(3),
				3, model, fullTextureName);
		return objPartEntity;
	}

	public static Entity newObjectFromModel(CustomVector3f position, CustomVector3f rotation, CustomVector3f size,
			float scale, RawModel model, String textureName) {
		LoadedIdTexture texture = new LoadedIdTexture(VBOLoader.loadTexture(textureName, textureName));
		TexturedModel texturedModel = new TexturedModel(model, texture);
		Entity entity = new Entity(texturedModel, position, rotation.x, rotation.y, rotation.z, 1);
		entity.scale = scale;
		return entity;
	}

	public static Entity newObjectFromModel(CustomVector3f position, CustomVector3f rotation, CustomVector3f size,
			float scale, String objFile, String textureName) {
		RawModel model = OBJLoader.loadObjModel(objFile);
		return newObjectFromModel(position, rotation, size, scale, model, textureName);
	}

	public static RawModel loadObjModel(String fileName) {
		FileReader fr = null;
		try {
			fr = new FileReader(new File(fileName));
		} catch (FileNotFoundException e) {
			CustomLog.outPrintln("Could not load simple OBJ model from file: " + fileName);
			e.printStackTrace();
			return null;
		}

		BufferedReader reader = new BufferedReader(fr);
		String line;
		List<Vector3f> vertices = new ArrayList<Vector3f>();
		List<Vector2f> textures = new ArrayList<Vector2f>();
		List<Vector3f> normals = new ArrayList<Vector3f>();
		List<Integer> indices = new ArrayList<Integer>();
		float[] verticesArray, normalsArray = null, textureArray = null;
		int[] indicesArray;
		try {
			while (true) {
				line = reader.readLine();
				String[] currentLine = line.split(" ");
				if (line.startsWith("v ")) // vertex position
				{
					Vector3f vertex = new Vector3f(Float.parseFloat(currentLine[1]), Float.parseFloat(currentLine[2]),
							Float.parseFloat(currentLine[3]));
					vertices.add(vertex);
				} else if (line.startsWith("vt ")) // texture coordinate
				{
					Vector2f texture = new Vector2f(Float.parseFloat(currentLine[1]), Float.parseFloat(currentLine[2]));
					textures.add(texture);
				} else if (line.startsWith("vn ")) // normal
				{
					Vector3f vertex = new Vector3f(Float.parseFloat(currentLine[1]), Float.parseFloat(currentLine[2]),
							Float.parseFloat(currentLine[3]));
					normals.add(vertex);
				} else if (line.startsWith("f ")) // face object
				{
					// All the v, vt, vn lines have been passed, end the loop
					textureArray = new float[vertices.size() * 2];
					normalsArray = new float[vertices.size() * 3];
					break;
				}
			}

			while (line != null) {
				// Make sure a face line is being read
				if (!line.startsWith("f ")) {
					line = reader.readLine();
					continue;
				}
				// A face is in the from f x/y/z a/b/c d/e/f
				// Split into these 4 sections
				// and then split the sections by slashes to get the numbers x, y, z, etc.
				String[] currentLine = line.split(" ");
				String[] vertex1 = currentLine[1].split("/");
				String[] vertex2 = currentLine[2].split("/");
				String[] vertex3 = currentLine[3].split("/");

				processVertex(vertex1, indices, textures, normals, textureArray, normalsArray);
				processVertex(vertex2, indices, textures, normals, textureArray, normalsArray);
				processVertex(vertex3, indices, textures, normals, textureArray, normalsArray);
				line = reader.readLine();
			}
			reader.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

		verticesArray = new float[vertices.size() * 3]; // Convert lists to array
		indicesArray = new int[indices.size()];
		int vertexPointer = 0;
		for (Vector3f vertex : vertices) {
			verticesArray[vertexPointer++] = vertex.x;
			verticesArray[vertexPointer++] = vertex.y;
			verticesArray[vertexPointer++] = vertex.z;
		}
		for (int i = 0; i < indices.size(); i++) {
			indicesArray[i] = indices.get(i);
		}
		return VBOLoader.loadToVAO(verticesArray, textureArray, normalsArray, indicesArray);
	}

	private static void processVertex(String[] vertexData, List<Integer> indices, List<Vector2f> textures,
			List<Vector3f> normals, float[] textureArray, float[] normalsArray) {
		int currentVertex = Integer.parseInt(vertexData[0]) - 1;
		indices.add(currentVertex);

		Vector2f currentTex = textures.get(Integer.parseInt(vertexData[1]) - 1);
		textureArray[currentVertex * 2] = currentTex.x;
		textureArray[currentVertex * 2 + 1] = 1 - currentTex.y; // Blender and OpenGL convention about xy coordinate
																// system

		Vector3f currentNorm = normals.get(Integer.parseInt(vertexData[2]) - 1);
		normalsArray[currentVertex * 3] = currentNorm.x;
		normalsArray[currentVertex * 3 + 1] = currentNorm.y;
		normalsArray[currentVertex * 3 + 2] = currentNorm.z;
	}

	private static void processVertex(String[] vertexData, List<Integer> indices, List<Vector2f> textures,
			List<Vector3f> normals, List<Vector2f> textureSubset, List<Vector3f> normalsSubset) {
		int currentVertex = Integer.parseInt(vertexData[0]) - 1;
		indices.add(currentVertex);

		Vector2f currentTex = textures.get(Integer.parseInt(vertexData[1]) - 1);
		Vector2f adjTex = new Vector2f(currentTex.x, 1 - currentTex.y);
		textureSubset.add(adjTex);

		Vector3f currentNorm = normals.get(Integer.parseInt(vertexData[2]) - 1);
		normalsSubset.add(currentNorm);
	}

}
