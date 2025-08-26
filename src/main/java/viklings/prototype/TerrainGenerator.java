package viklings.prototype;

import java.util.ArrayList;
import java.util.List;

import org.joml.Vector4f;

import engine.game.state.Material;
import engine.game.state.Position;
import graphics.core.Model;
import graphics.core.scene.Camera;
import graphics.flat.FlatRenderable;

public class TerrainGenerator {

    private final Camera camera;

    public TerrainGenerator(Camera camera) {
	this.camera = camera;
    }

    //Based on the camera position we will generate terrain for a chunk.
    public FlatRenderable generateTerrain() throws Exception {
	return new Terrain(camera);
    }

    public static class Terrain implements FlatRenderable {
	private String textureFile = "textures/grass.png";
	private float tileWidth = 32;
	private float tileHeight = 32;
	private final Position position;
	private final Model model;
	
	private Terrain(Camera camera) throws Exception {
	    //Determine size and position of terrain chunk - (minx, miny, maxx, maxy)
	    Vector4f chunkSize = getChunkRectangle(camera, tileHeight, tileWidth);
	    
	    //TODO: Don't set Z here, instead make scene object take care of that
	    position = new Position();
	    position.setCoordinates(chunkSize.x, chunkSize.y, 0.01f);
	    
	    //Number of rows of tiles to build = ceiling(
	    int numRows = (int) Math.ceil(chunkSize.z / tileWidth);
	    int numCols = (int) Math.ceil(chunkSize.w / tileHeight);
	    
	    Material material = new Material();
	    material.setTextureFile(textureFile);

	    //TODO: optimize this - we know ahead of time how many we'll need of each, so we can size these to start
	    // and avoid creating array lists
	    List<Float> positions = new ArrayList<>();
	    List<Float> textCoords = new ArrayList<>();
	    float[] normals   = new float[0];
	    List<Integer> indices   = new ArrayList<>();

	    for (int i = 0; i < numRows; i++) {
		for (int j = 0; j < numCols; j++) {
		    // Build a tile composed by two triangles

		    float xOffset = tileWidth * i;
		    float yOffset = tileHeight * j;
		    int idxOffset = 4 * ((j * numRows) + i);
		    
		    // Left Top vertex
		    positions.add(xOffset + 0.0f); // x
		    positions.add(yOffset + 0.0f); //y
		    positions.add(0f); //z
		    textCoords.add(0f );
		    textCoords.add(0f );
		    indices.add(idxOffset);

		    // Left Bottom vertex
		    positions.add(xOffset + 0.0f); // x
		    positions.add(yOffset + tileHeight); //y
		    positions.add(0f); //z
		    textCoords.add(0f );
		    textCoords.add(1f );
		    indices.add(idxOffset + 1);

		    // Right Bottom vertex
		    positions.add(xOffset + tileWidth); // x
		    positions.add(yOffset + tileHeight); //y
		    positions.add(0f); //z
		    textCoords.add(1f );
		    textCoords.add(1f );
		    indices.add(idxOffset + 2); //This has to be incremented by how many total tiles we've added * number of indices per tile

		    // Right Top vertex
		    positions.add(xOffset + tileWidth); // x
		    positions.add(yOffset + 0.0f); //y
		    positions.add(0f); //z
		    textCoords.add(1f );
		    textCoords.add(0f );
		    indices.add(idxOffset + 3);

		    // Add indices for left top and bottom right vertices
		    indices.add(idxOffset + 0);
		    indices.add(idxOffset + 2);
		}
	    }

	    float[] posArr = new float[positions.size()];
	    for (int i = 0; i < posArr.length; i++) {
		posArr[i] = positions.get(i);
	    }

	    float[] textCoordsArr = new float[textCoords.size()];
	    for (int i = 0; i < textCoordsArr.length; i++) {
		textCoordsArr[i] = textCoords.get(i);
	    }

	    int[] indicesArr = indices.stream().mapToInt(i->i).toArray();
	    model = new Model(posArr, textCoordsArr, normals, indicesArr);
	    model.setMaterialAndBindTexture(material);
	}
	
	

	@Override
	public Model getModel() {
	    return model;
	}

	@Override
	public Position getPosition() {
	    return position;
	}
	
	//TODO: Split game world up into chunks and only load the 9 at a time (the one where the player is, the 4 adjacent, and the 4 corners
	private Vector4f getChunkRectangle(Camera camera, float tileHeight, float tileWidth) {
	    return new Vector4f(0f, 0f, 600f, 480f);
	}

    }
}
