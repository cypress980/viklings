package graphics.core;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import org.lwjgl.system.MemoryUtil;

import engine.game.state.Material;

/**
 * This class loads the model into openGL land
 * 
 * @author cypress980
 *
 */
public class Model {

    private final int vaoId;

    private final List<Integer> vboIdList;

    private final int vertexCount;

    private Material material;
    
    private Texture texture;

    public Model(float[] positions, float[] textCoords, float[] normals, int[] indices) {
	FloatBuffer coordBuffer = null;
	FloatBuffer textCoordsBuffer = null;
	FloatBuffer vecNormalsBuffer = null;
	IntBuffer indicesBuffer = null;
	
	try {
	    vertexCount = indices.length;
	    vboIdList = new ArrayList<>();

	    vaoId = glGenVertexArrays();
	    glBindVertexArray(vaoId);

	    // Coordinate VBO
	    int vboId = glGenBuffers();
	    vboIdList.add(vboId);
	    coordBuffer = MemoryUtil.memAllocFloat(positions.length);
	    coordBuffer.put(positions).flip();
	    glBindBuffer(GL_ARRAY_BUFFER, vboId);
	    glBufferData(GL_ARRAY_BUFFER, coordBuffer, GL_STATIC_DRAW);
	    glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);

	    // Texture coordinates VBO
	    vboId = glGenBuffers();
	    vboIdList.add(vboId);
	    textCoordsBuffer = MemoryUtil.memAllocFloat(textCoords.length);
	    textCoordsBuffer.put(textCoords).flip();
	    glBindBuffer(GL_ARRAY_BUFFER, vboId);
	    glBufferData(GL_ARRAY_BUFFER, textCoordsBuffer, GL_STATIC_DRAW);
	    glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);

	    // Vertex normals VBO
	    vboId = glGenBuffers();
	    vboIdList.add(vboId);
	    vecNormalsBuffer = MemoryUtil.memAllocFloat(normals.length);
	    vecNormalsBuffer.put(normals).flip();
	    glBindBuffer(GL_ARRAY_BUFFER, vboId);
	    glBufferData(GL_ARRAY_BUFFER, vecNormalsBuffer, GL_STATIC_DRAW);
	    glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);

	    // Index VBO
	    vboId = glGenBuffers();
	    vboIdList.add(vboId);
	    indicesBuffer = MemoryUtil.memAllocInt(indices.length);
	    indicesBuffer.put(indices).flip();
	    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboId);
	    glBufferData(GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL_STATIC_DRAW);

	    glBindBuffer(GL_ARRAY_BUFFER, 0);
	    glBindVertexArray(0);
	} finally {
	    if (coordBuffer != null) {
		MemoryUtil.memFree(coordBuffer);
	    }
	    if (textCoordsBuffer != null) {
		MemoryUtil.memFree(textCoordsBuffer);
	    }
	    if (vecNormalsBuffer != null) {
		MemoryUtil.memFree(vecNormalsBuffer);
	    }
	    if (indicesBuffer != null) {
		MemoryUtil.memFree(indicesBuffer);
	    }
	}
    }

    public Material getMaterial() {
	return material;
    }
    
    //TODO: This here is a bit of a mess - trying to do too much
    public Texture getTexture() {
	return texture;
    }
    
    public void setMaterialAndBindTexture(Material material) throws Exception {
	this.material = material;
	this.texture = new Texture(material.getTextureFile());
    }
    
    public int getVaoId() {
	return vaoId;
    }

    public int getVertexCount() {
	return vertexCount;
    }

    public void render() {
	if (texture != null) {
	    // Activate first texture bank
	    glActiveTexture(GL_TEXTURE0);
	    // Bind the texture
	    glBindTexture(GL_TEXTURE_2D, texture.getId());
	}
	
	// Draw the mesh
	glBindVertexArray(getVaoId());
	glEnableVertexAttribArray(0);
	glEnableVertexAttribArray(1);
	glEnableVertexAttribArray(2);

	glDrawElements(GL_TRIANGLES, getVertexCount(), GL_UNSIGNED_INT, 0);

	// Restore state
	glDisableVertexAttribArray(0);
	glDisableVertexAttribArray(1);
	glDisableVertexAttribArray(2);
	glBindVertexArray(0);
	glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void cleanUp() {
	glDisableVertexAttribArray(0);

	// Delete the VBOs
	glBindBuffer(GL_ARRAY_BUFFER, 0);
	for (int vboId : vboIdList) {
	    glDeleteBuffers(vboId);
	}

	// Delete the texture
	if (texture != null) {
	    texture.cleanup();
	}

	// Delete the VAO
	glBindVertexArray(0);
	glDeleteVertexArrays(vaoId);
    }
    
    public void deleteBuffers() {
	glDisableVertexAttribArray(0);

	// Delete the VBOs
	glBindBuffer(GL_ARRAY_BUFFER, 0);
	for (int vboId : vboIdList) {
	    glDeleteBuffers(vboId);
	}

	// Delete the VAO
	glBindVertexArray(0);
	glDeleteVertexArrays(vaoId);
    }
}