package graphics.flat;

import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import engine.game.state.Position;
import graphics.Renderer;
import graphics.ResourceLoader;
import graphics.core.Model;
import graphics.core.ShaderProgram;
import graphics.flat.FlatRenderable;

public class FlatRenderer implements Renderer {
    
    private ShaderProgram hudShaderProgram;

    private int windowHeightPx;
    
    private int windowWidthPx;
    
    private List<? extends FlatRenderable> scene;

    private Matrix4f orthoMatrix;
    
    private Matrix4f modelViewMatrix = new Matrix4f();
    
    @Override
    public void loadShaders() throws Exception {
	orthoMatrix = new Matrix4f();
	
        hudShaderProgram = new ShaderProgram();
        ResourceLoader resLoader = new ResourceLoader();
        hudShaderProgram.createVertexShader(resLoader.loadToString("shaders/hud/hud_vertex.vs"));
        hudShaderProgram.createFragmentShader(resLoader.loadToString("shaders/hud/hud_fragment.fs"));
        hudShaderProgram.link();
        
        // Create uniforms for orthographic-model projection matrix and base color
        hudShaderProgram.createUniform("projModelMatrix");
        hudShaderProgram.createUniform("modelViewMatrix");
        hudShaderProgram.createUniform("color");
    }

    @Override
    public void render() {
        hudShaderProgram.bind();
        
        //TODO: we only need to make this call when the window size changes
        Matrix4f ortho = getOrthoProjectionMatrix(0, windowWidthPx, windowHeightPx, 0);
        for (FlatRenderable item : scene) {
            Model mesh = item.getModel();
            // Set orthographic and model matrix for this HUD item
            Matrix4f projModelMatrix = getOrthoProjModelMatrix(item.getPosition(), ortho);
            hudShaderProgram.setUniform("modelViewMatrix", modelViewMatrix);
            hudShaderProgram.setUniform("projModelMatrix", projModelMatrix);
            hudShaderProgram.setUniform("color", item.getModel().getMaterial().getColor());

            // Render the mesh for this HUD item
            mesh.render();
        }

        hudShaderProgram.unbind();
    }

    @Override
    public void cleanupShaders() {
        if (hudShaderProgram != null) {
            hudShaderProgram.cleanup();
        }
    }
    
    public void setWindowHeightPx(int windowHeightPx) {
        this.windowHeightPx = windowHeightPx;
    }

    public void setWindowWidthPx(int windowWidthPx) {
        this.windowWidthPx = windowWidthPx;
    }
    
    public void setScene(List<? extends FlatRenderable> scene) {
	this.scene = scene;
    }
    
    public void moveCamera(Vector3f pos) {
	modelViewMatrix.translate(pos.negate());
    }
    
    private Matrix4f getOrthoProjectionMatrix(float left, float right, float bottom, float top) {
        orthoMatrix.identity();
        orthoMatrix.setOrtho2D(left, right, bottom, top);
        return orthoMatrix;
    }
    
    private Matrix4f getOrthoProjModelMatrix(Position gameItem, Matrix4f orthoMatrix) {
        Vector3f rotation = gameItem.getRotation();
        Matrix4f modelMatrix = new Matrix4f();
        modelMatrix.identity().translate(gameItem.getCoordinates()).
                rotateX((float)Math.toRadians(-rotation.x)).
                rotateY((float)Math.toRadians(-rotation.y)).
                rotateZ((float)Math.toRadians(-rotation.z)).
                scale(gameItem.getScale());
        Matrix4f orthoMatrixCurr = new Matrix4f(orthoMatrix);
        orthoMatrixCurr.mul(modelMatrix);
        return orthoMatrixCurr;
    }
}
