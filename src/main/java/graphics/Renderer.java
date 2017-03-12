package graphics;

public interface Renderer {

    void loadShaders() throws Exception;

    void cleanupShaders();

    void render();
    
}
