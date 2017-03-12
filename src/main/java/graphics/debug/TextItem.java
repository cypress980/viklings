package graphics.debug;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import engine.GameComponent;
import graphics.Position;
import graphics.Material;
import graphics.Model;
import graphics.Texture;

public class TextItem implements DebugRenderable {

    private static final float ZPOS = 0.0f;

    private static final int VERTICES_PER_QUAD = 4;

    private String text;
    
    private final int numCols;
    
    private final int numRows;
    
    private Model model;
    
    private Position position;
    
    public TextItem(String text, String fontFileName, int numCols, int numRows) throws Exception {
        super();
        this.text = text;
        this.numCols = numCols;
        this.numRows = numRows;
        Texture texture = new Texture(fontFileName);
        this.model = buildMesh(texture, numCols, numRows);
        this.position = new Position();
    }

    private Model buildMesh(Texture texture, int numCols, int numRows) {
        byte[] chars = text.getBytes(Charset.forName("ISO-8859-1"));
        int numChars = chars.length;

        List<Float> positions = new ArrayList<>();
        List<Float> textCoords = new ArrayList<>();
        float[] normals   = new float[0];
        List<Integer> indices   = new ArrayList<>();
        
        float tileWidth = (float)texture.getWidth() / (float)numCols;
        float tileHeight = (float)texture.getHeight() / (float)numRows;

        for(int i=0; i<numChars; i++) {
            byte currChar = chars[i];
            int col = currChar % numCols;
            int row = currChar / numCols;
            
            // Build a character tile composed by two triangles
            
            // Left Top vertex
            positions.add((float)i*tileWidth); // x
            positions.add(0.0f); //y
            positions.add(ZPOS); //z
            textCoords.add((float)col / (float)numCols );
            textCoords.add((float)row / (float)numRows );
            indices.add(i*VERTICES_PER_QUAD);
                        
            // Left Bottom vertex
            positions.add((float)i*tileWidth); // x
            positions.add(tileHeight); //y
            positions.add(ZPOS); //z
            textCoords.add((float)col / (float)numCols );
            textCoords.add((float)(row + 1) / (float)numRows );
            indices.add(i*VERTICES_PER_QUAD + 1);

            // Right Bottom vertex
            positions.add((float)i*tileWidth + tileWidth); // x
            positions.add(tileHeight); //y
            positions.add(ZPOS); //z
            textCoords.add((float)(col + 1)/ (float)numCols );
            textCoords.add((float)(row + 1) / (float)numRows );
            indices.add(i*VERTICES_PER_QUAD + 2);

            // Right Top vertex
            positions.add((float)i*tileWidth + tileWidth); // x
            positions.add(0.0f); //y
            positions.add(ZPOS); //z
            textCoords.add((float)(col + 1)/ (float)numCols );
            textCoords.add((float)row / (float)numRows );
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
        mesh.setMaterial(new Material(texture));
        return mesh;
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
        Texture texture = model.getMaterial().getTexture();
        model.deleteBuffers();
        model = buildMesh(texture, numCols, numRows);
    }

    public void setPosition(float x, float y) {
        this.position.setPosition(x, y, 0);
    }
    
    public Model getModel() {
	return model;
    }

    @Override
    public Position getPosition() {
	return position;
    }
}