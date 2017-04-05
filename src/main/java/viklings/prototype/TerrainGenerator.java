package viklings.prototype;

import java.util.ArrayList;
import java.util.List;

import engine.game.state.Material;
import engine.game.state.Position;
import graphics.core.Model;
import graphics.core.Texture;
import graphics.core.scene.Camera;
import graphics.flat.FlatRenderable;

public class TerrainGenerator {

    private final Camera camera;

    public TerrainGenerator(Camera camera) {
	this.camera = camera;
    }

    //Based on the camera position we will generate terrain for a chunk.
    public FlatRenderable generateTerrain() throws Exception {
	return new Terrain();
    }

    public static class Terrain implements FlatRenderable {
	private String textureFile = "textures/grass.png";

	private final Position position;
	private final Model model;

	private Terrain() throws Exception {
	    position = new Position();
	    position.setCoordinates(200f, 200f, 0.01f);
	    
	    Material material = new Material();
	    material.setTextureFile(textureFile);

	    List<Float> positions = new ArrayList<>();
	    List<Float> textCoords = new ArrayList<>();
	    float[] normals   = new float[0];
	    List<Integer> indices   = new ArrayList<>();

	    //Create texture in openGL land, just to get the h & w
	    Texture texture = new Texture(material.getTextureFile());

	    float tileWidth = texture.getHeight();
	    float tileHeight = texture.getWidth();

	    texture.cleanup();

	    // Build a character tile composed by two triangles

	    // Left Top vertex
	    positions.add((float)tileWidth); // x
	    positions.add(0.0f); //y
	    positions.add(0f); //z
	    textCoords.add(0f );
	    textCoords.add(0f );
	    indices.add(0);

	    // Left Bottom vertex
	    positions.add((float)tileWidth); // x
	    positions.add(tileHeight); //y
	    positions.add(0f); //z
	    textCoords.add(0f );
	    textCoords.add(1f );
	    indices.add(1);

	    // Right Bottom vertex
	    positions.add((float)tileWidth + tileWidth); // x
	    positions.add(tileHeight); //y
	    positions.add(0f); //z
	    textCoords.add(1f );
	    textCoords.add(1f );
	    indices.add(2);

	    // Right Top vertex
	    positions.add((float)tileWidth + tileWidth); // x
	    positions.add(0.0f); //y
	    positions.add(0f); //z
	    textCoords.add(1f );
	    textCoords.add(0f );
	    indices.add(3);

	    // Add indices for left top and bottom right vertices
	    indices.add(0);
	    indices.add(2);


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

    }
}
