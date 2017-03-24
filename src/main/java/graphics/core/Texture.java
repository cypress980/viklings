package graphics.core;

import de.matthiasmann.twl.utils.PNGDecoder;
import de.matthiasmann.twl.utils.PNGDecoder.Format;

import java.nio.ByteBuffer;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;

public class Texture {

    private final int id;

    private final int width;

    private final int height;
    
    //TODO: in text items we need to know the h/w to map the texture to the model
    // The model takes a texture file name, and binds it itself. So, what we do is
    // we create and bind texture here, then we create the model, then we create and bind the model again
    // I want to have a clean interface from the core for how to bind state to openGL, and that is not clean.
    // Not sure quite the best fix, but I should fix it.
    public Texture(String fileName) throws Exception {
        // Load Texture file
	ClassLoader classLoader = Texture.class.getClassLoader();
        PNGDecoder decoder = new PNGDecoder(classLoader.getResourceAsStream(fileName));
        
        // Load texture contents into a byte buffer
        ByteBuffer buf = ByteBuffer.allocateDirect(
                4 * decoder.getWidth() * decoder.getHeight());
        decoder.decode(buf, decoder.getWidth() * 4, Format.RGBA);
        buf.flip();

        // Create a new OpenGL texture 
        int textureId = glGenTextures();
        // Bind the texture
        glBindTexture(GL_TEXTURE_2D, textureId);

        // Tell OpenGL how to unpack the RGBA bytes. Each component is 1 byte size
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        
        // Upload the texture data
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, decoder.getWidth(), decoder.getHeight(), 0,
                GL_RGBA, GL_UNSIGNED_BYTE, buf);
        
        // Generate MipMap
        glGenerateMipmap(GL_TEXTURE_2D);
        
        this.id = textureId;
        this.width = decoder.getWidth();
        this.height = decoder.getHeight();
    }
    
    public void bind() {
        glBindTexture(GL_TEXTURE_2D, id);
    }

    public int getId() {
        return id;
    }
    
    public int getHeight() {
    	return height;
    }
    
    public int getWidth() {
    	return width;
    }
    
    public void cleanup() {
        glDeleteTextures(id);
    }
}