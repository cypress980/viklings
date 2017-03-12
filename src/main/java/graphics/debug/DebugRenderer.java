package graphics.debug;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import engine.GamePosition;
import graphics.Model;
import graphics.Renderer;
import graphics.ResourceLoader;
import graphics.ShaderProgram;
import graphics.debug.DebugRenderable;

public class DebugRenderer implements Renderer {
    
    private ShaderProgram hudShaderProgram;

    private int windowHeightPx;
    
    private int windowWidthPx;
    
    private DebugRenderable debug;

    private Matrix4f orthoMatrix;
    
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
        hudShaderProgram.createUniform("color");
    }

    @Override
    public void render() {
        hudShaderProgram.bind();
        
        //TODO: we only need to make this call when the window size changes
        Matrix4f ortho = getOrthoProjectionMatrix(0, windowWidthPx, windowHeightPx, 0);
        for (GamePosition item : debug.getGameItems()) {
            Model mesh = item.getModel();
            // Set orthographic and model matrix for this HUD item
            Matrix4f projModelMatrix = getOrthoProjModelMatrix(item, ortho);
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
    
    public void setHud(DebugRenderable hud) {
	this.debug = hud;
    }
    
    private Matrix4f getOrthoProjectionMatrix(float left, float right, float bottom, float top) {
        orthoMatrix.identity();
        orthoMatrix.setOrtho2D(left, right, bottom, top);
        return orthoMatrix;
    }
    
    private Matrix4f getOrthoProjModelMatrix(GamePosition gameItem, Matrix4f orthoMatrix) {
        Vector3f rotation = gameItem.getRotation();
        Matrix4f modelMatrix = new Matrix4f();
        modelMatrix.identity().translate(gameItem.getPosition()).
                rotateX((float)Math.toRadians(-rotation.x)).
                rotateY((float)Math.toRadians(-rotation.y)).
                rotateZ((float)Math.toRadians(-rotation.z)).
                scale(gameItem.getScale());
        Matrix4f orthoMatrixCurr = new Matrix4f(orthoMatrix);
        orthoMatrixCurr.mul(modelMatrix);
        return orthoMatrixCurr;
    }
}
