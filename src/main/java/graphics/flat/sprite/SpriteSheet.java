package graphics.flat.sprite;

import java.util.ArrayList;
import java.util.List;

import engine.game.state.Material;
import graphics.core.Model;
import graphics.core.Texture;

public class SpriteSheet {

    private static final float ZPOS = 0.0f;

    private static final int VERTICES_PER_QUAD = 4;

    private final int numCols;

    private final int numRows;

    private Model model;

    private int frame = 0;

    public SpriteSheet(String fileName, int numCols, int numRows) throws Exception {
	this.numCols = numCols;
	this.numRows = numRows;
	Material material = new Material(fileName);
	this.model = buildMesh(material, numCols, numRows);
    }

    private Model buildMesh(Material material, int numCols, int numRows) throws Exception {

	List<Float> positions = new ArrayList<>();
	List<Float> textCoords = new ArrayList<>();
	float[] normals   = new float[0];
	List<Integer> indices   = new ArrayList<>();

	//Create texture in openGL land, just to get the h & w
	Texture texture = new Texture(material.getTextureFile());

	float tileWidth = (float)texture.getWidth() / (float)numCols;
	float tileHeight = (float)texture.getHeight() / (float)numRows;

	texture.cleanup();


	int col = frame % numCols;
	int row = frame / numCols;

	// Build a character tile composed by two triangles

	// Left Top vertex
	positions.add((float)tileWidth); // x
	positions.add(0.0f); //y
	positions.add(ZPOS); //z
	textCoords.add((float)col / (float)numCols );
	textCoords.add((float)row / (float)numRows );
	indices.add(0);

	// Left Bottom vertex
	positions.add((float)tileWidth); // x
	positions.add(tileHeight); //y
	positions.add(ZPOS); //z
	textCoords.add((float)col / (float)numCols );
	textCoords.add((float)(row + 1) / (float)numRows );
	indices.add(1);

	// Right Bottom vertex
	positions.add((float)tileWidth + tileWidth); // x
	positions.add(tileHeight); //y
	positions.add(ZPOS); //z
	textCoords.add((float)(col + 1)/ (float)numCols );
	textCoords.add((float)(row + 1) / (float)numRows );
	indices.add(2);

	// Right Top vertex
	positions.add((float)tileWidth + tileWidth); // x
	positions.add(0.0f); //y
	positions.add(ZPOS); //z
	textCoords.add((float)(col + 1)/ (float)numCols );
	textCoords.add((float)row / (float)numRows );
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
	Model mesh = new Model(posArr, textCoordsArr, normals, indicesArr);
	mesh.setMaterialAndBindTexture(material);
	return mesh;
    }

    public int getFrame() {
	return frame;
    }

    public void setFrame(int frame) throws Exception {
	this.frame = frame;
	Material material = model.getMaterial();
	model.deleteBuffers();
	model = buildMesh(material, numCols, numRows);
    }

    public Model getModel() {
	return model;
    }
}