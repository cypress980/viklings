package graphics;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;

import java.util.ArrayList;
import java.util.List;

/**
 * Pump all graphics components of the game to the graphics card and render them
 * 
 * @author cypress980
 *
 */
public class GraphicsEngine {
    
    private final List<Renderer> renderers = new ArrayList<>();
    
    public void addRenderer(Renderer renderer) throws Exception {
	// Register renderer - openGL is a state machine, so we must do setup and tear-down of state
	try {
	    renderer.loadShaders();
	    renderers.add(renderer);
	} catch (Exception e) {
	    //TODO: Add legitimate logging
	    e.printStackTrace();
	    throw e;
	}
    }
    
    public void removeRenderer(Renderer renderer) {
	renderer.cleanupShaders();
	renderers.remove(renderer);
    }
    
    public void render() {
	glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
	for (Renderer renderer : renderers) {
	    renderer.render();
	}
    }

    public void removeAllRenderers() {
	for(Renderer r : renderers) {
	    r.cleanupShaders();
	}
	renderers.clear();
    }
}
