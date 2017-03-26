package graphics.flat;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.joml.Vector3f;

import engine.game.state.Material;
import graphics.core.Model;
import graphics.core.Texture;

public class Font {
    
    private static final float ZPOS = 0.0f;

    private static final int VERTICES_PER_QUAD = 4;
    
    private static final int SHEET_ROWS = 16;

    private static final int SHEET_COLUMNS = 16;

    private static final String FONT_SHEET = "textures/sprites/font_texture.png";
    
    private Material font;
    
    private final Vector3f color = new Vector3f(1, 1, 1);

    public Font() throws Exception {
	this.font = new Material();
	font.setTextureFile(FONT_SHEET);
	font.setColor(color);
    }
    
    public void setColor(int r, int g, int b) {
	color.set(r, g, b);
    }

    public Model buildMesh(String text) throws Exception {
	byte[] chars = text.getBytes(Charset.forName("ISO-8859-1"));
	int numChars = chars.length;

	List<Float> positions = new ArrayList<>();
	List<Float> textCoords = new ArrayList<>();
	float[] normals   = new float[0];
	List<Integer> indices   = new ArrayList<>();

	//Create texture in openGL land, just to get the h & w
	//TODO: See if we can do this smarter
	Texture texture = new Texture(FONT_SHEET);

	float tileWidth = (float)texture.getWidth() / (float)SHEET_COLUMNS;
	float tileHeight = (float)texture.getHeight() / (float)SHEET_ROWS;

	texture.cleanup();

	for(int i=0; i<numChars; i++) {
	    byte currChar = chars[i];
	    int col = currChar % SHEET_COLUMNS;
	    int row = currChar / SHEET_COLUMNS;

	    // Build a character tile composed by two triangles

	    // Left Top vertex
	    positions.add((float)i*tileWidth); // x
	    positions.add(0.0f); //y
	    positions.add(ZPOS); //z
	    textCoords.add((float)col / (float)SHEET_COLUMNS );
	    textCoords.add((float)row / (float)SHEET_ROWS );
	    indices.add(i*VERTICES_PER_QUAD);

	    // Left Bottom vertex
	    positions.add((float)i*tileWidth); // x
	    positions.add(tileHeight); //y
	    positions.add(ZPOS); //z
	    textCoords.add((float)col / (float)SHEET_COLUMNS );
	    textCoords.add((float)(row + 1) / (float)SHEET_ROWS );
	    indices.add(i*VERTICES_PER_QUAD + 1);

	    // Right Bottom vertex
	    positions.add((float)i*tileWidth + tileWidth); // x
	    positions.add(tileHeight); //y
	    positions.add(ZPOS); //z
	    textCoords.add((float)(col + 1)/ (float)SHEET_COLUMNS );
	    textCoords.add((float)(row + 1) / (float)SHEET_ROWS );
	    indices.add(i*VERTICES_PER_QUAD + 2);

	    // Right Top vertex
	    positions.add((float)i*tileWidth + tileWidth); // x
	    positions.add(0.0f); //y
	    positions.add(ZPOS); //z
	    textCoords.add((float)(col + 1)/ (float)SHEET_COLUMNS );
	    textCoords.add((float)row / (float)SHEET_ROWS );
	    indices.add(i*VERTICES_PER_QUAD + 3);

	    // Add indices por left top and bottom right vertices
	    indices.add(i*VERTICES_PER_QUAD);
	    indices.add(i*VERTICES_PER_QUAD + 2);
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
	Model mesh = new Model(posArr, textCoordsArr, normals, indicesArr);
	mesh.setMaterialAndBindTexture(font);
	return mesh;
    }
}
