package graphics.scene;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import engine.GamePosition;
import graphics.Model;
import graphics.Renderer;
import graphics.ResourceLoader;
import graphics.ShaderProgram;
import static org.lwjgl.opengl.GL11.*;

import java.util.Collection;

public class SceneRenderer implements Renderer {

    /**
     * Field of View in Radians
     */
    private static final float FOV = (float) Math.toRadians(60.0f);

    private static final float Z_NEAR = 0.01f;

    private static final float Z_FAR = 1000.f;

    private static final int MAX_POINT_LIGHTS = 5;

    private static final int MAX_SPOT_LIGHTS = 5;

    private ShaderProgram sceneShaderProgram;

    private final float specularPower;
    
    private final Matrix4f projectionMatrix;

    private final Matrix4f modelViewMatrix;
    
    private final Matrix4f viewMatrix;
    
    public SceneRenderer() {
        specularPower = 10f;
        projectionMatrix = new Matrix4f();
        modelViewMatrix = new Matrix4f();
        viewMatrix = new Matrix4f();
    }

    //TODO: Move this to Graphics Engine - we should clear before all renderers run, not just before this one - though 
    // this one *should* probably always be first.
    public void clear() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }
    
    // Scene can be anything, but we'll make the game components implement SceneRenderable.
    // This way a reference to the game state, stored as a component hierarchy can be rendered without 
    // further manipulation.
    private Collection<? extends SceneRenderable> scene;
    
    // Camera is a reference to the game camera. It contains world coordinate information that is used
    // to translate the world coordinates to screen coordinates
    private Camera camera;
    
    private int windowHeightPx;
    
    private int windowWidthPx;
    
    private Vector3f ambientLight;

    private DirectionalLight directionalLight;
    
    @Override
    public void loadShaders() throws Exception {
        // Create shader
        sceneShaderProgram = new ShaderProgram();
        ResourceLoader resLoader = new ResourceLoader();
        sceneShaderProgram.createVertexShader(resLoader.loadToString("shaders/vertex.vs"));
        sceneShaderProgram.createFragmentShader(resLoader.loadToString("shaders/fragment.fs"));
        sceneShaderProgram.link();

        // Create uniforms for modelView and projection matrices and texture
        sceneShaderProgram.createUniform("projectionMatrix");
        sceneShaderProgram.createUniform("modelViewMatrix");
        sceneShaderProgram.createUniform("texture_sampler");
        // Create uniform for material
        sceneShaderProgram.createMaterialUniform("material");
        // Create lighting related uniforms
        sceneShaderProgram.createUniform("specularPower");
        sceneShaderProgram.createUniform("ambientLight");
        sceneShaderProgram.createPointLightListUniform("pointLights", MAX_POINT_LIGHTS);
        sceneShaderProgram.createSpotLightListUniform("spotLights", MAX_SPOT_LIGHTS);
        sceneShaderProgram.createDirectionalLightUniform("directionalLight");
    }

    @Override
    public void render() {
	
	clear();
	
	int spotLights = 0;
	int pointLights = 0;
	
	sceneShaderProgram.bind();
	
        // Update projection Matrix
        Matrix4f projectionMatrix = getProjectionMatrix(FOV, windowWidthPx, windowHeightPx, Z_NEAR, Z_FAR);
        sceneShaderProgram.setUniform("projectionMatrix", projectionMatrix);

        // Update view Matrix
        Matrix4f viewMatrix = getViewMatrix(camera);
	
        // Prepare Global Lighting
        prepareLightingGlobals(viewMatrix);
        
	// Prepare Lighting Uniforms
	for (SceneRenderable thing : scene) {
	    if (thing.hasPointLight() && ++pointLights < MAX_POINT_LIGHTS) {
		preparePointLight(thing.getPointLight(), pointLights, viewMatrix);
	    }
	    if (thing.hasSpotLight() && ++spotLights < MAX_SPOT_LIGHTS) {
		prepareSpotLight(thing.getSpotLight(), spotLights, viewMatrix);
	    }
	}
	
	// Render Scene
	for (SceneRenderable thing : scene) {
	    render(thing, viewMatrix);
	}
	
	sceneShaderProgram.unbind();
    }

    @Override
    public void cleanupShaders() {
        if (sceneShaderProgram != null) {
            sceneShaderProgram.cleanup();
        }
    }
    
    public void setScene(Collection<? extends SceneRenderable> scene) {
	this.scene = scene;
    }
    
    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    public void setWindowHeightPx(int windowHeightPx) {
        this.windowHeightPx = windowHeightPx;
    }

    public void setWindowWidthPx(int windowWidthPx) {
        this.windowWidthPx = windowWidthPx;
    }
    
    public void setAmbientLight(Vector3f ambientLight) {
        this.ambientLight = ambientLight;
    }

    public void setDirectionalLight(DirectionalLight directionalLight) {
        this.directionalLight = directionalLight;
    }

    private void prepareLightingGlobals(Matrix4f viewMatrix) {
        sceneShaderProgram.setUniform("ambientLight", ambientLight);
        sceneShaderProgram.setUniform("specularPower", specularPower);
        
        // Get a copy of the directional light object and transform its position to view coordinates
        DirectionalLight currDirLight = new DirectionalLight(directionalLight);
        Vector4f dir = new Vector4f(currDirLight.getDirection(), 0);
        dir.mul(viewMatrix);
        currDirLight.setDirection(new Vector3f(dir.x, dir.y, dir.z));
        sceneShaderProgram.setUniform("directionalLight", currDirLight);
    }
    
    private void preparePointLight(PointLight pointLight, int index, Matrix4f viewMatrix) {
        // Get a copy of the point light object and transform its position to view coordinates
        PointLight currPointLight = new PointLight(pointLight);
        Vector3f lightPos = currPointLight.getPosition();
        Vector4f aux = new Vector4f(lightPos, 1);
        aux.mul(viewMatrix);
        lightPos.x = aux.x;
        lightPos.y = aux.y;
        lightPos.z = aux.z;
        sceneShaderProgram.setUniform("pointLights", currPointLight, index);
    }
    
    private void prepareSpotLight(SpotLight spotLight, int index, Matrix4f viewMatrix) {
        // Get a copy of the spot light object and transform its position and cone direction to view coordinates
        SpotLight currSpotLight = new SpotLight(spotLight);
        Vector4f dir = new Vector4f(currSpotLight.getConeDirection(), 0);
        dir.mul(viewMatrix);
        currSpotLight.setConeDirection(new Vector3f(dir.x, dir.y, dir.z));
        Vector3f lightPos = currSpotLight.getPointLight().getPosition();

        Vector4f aux = new Vector4f(lightPos, 1);
        aux.mul(viewMatrix);
        lightPos.x = aux.x;
        lightPos.y = aux.y;
        lightPos.z = aux.z;

        sceneShaderProgram.setUniform("spotLights", currSpotLight, index);
    }
    
    private void render(SceneRenderable thing, Matrix4f viewMatrix) {
        sceneShaderProgram.setUniform("texture_sampler", 0);
        // Render each gameItem
        Model model = thing.getModel();
        if (model == null) {
            return; //Nothing to do
        }
        // Set model view matrix for this item
        Matrix4f modelViewMatrix = getModelViewMatrix(thing.getPosition(), viewMatrix);
        sceneShaderProgram.setUniform("modelViewMatrix", modelViewMatrix);
        // Render the mesh for this game item
        sceneShaderProgram.setUniform("material", model.getMaterial());
        model.render();
    }
    

    private Matrix4f getProjectionMatrix(float fov, float width, float height, float zNear, float zFar) {
        float aspectRatio = width / height;        
        projectionMatrix.identity();
        projectionMatrix.perspective(fov, aspectRatio, zNear, zFar);
        return projectionMatrix;
    }
    
    private Matrix4f getViewMatrix(Camera camera) {
        Vector3f cameraPos = camera.getPosition();
        Vector3f rotation = camera.getRotation();
        
        viewMatrix.identity();
        // First do the rotation so camera rotates over its position
        viewMatrix.rotate((float)Math.toRadians(rotation.x), new Vector3f(1, 0, 0))
                .rotate((float)Math.toRadians(rotation.y), new Vector3f(0, 1, 0));
        // Then do the translation
        viewMatrix.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        return viewMatrix;
    }

    private Matrix4f getModelViewMatrix(GamePosition gameItem, Matrix4f viewMatrix) {
        Vector3f rotation = gameItem.getRotation();
        modelViewMatrix.identity().translate(gameItem.getPosition()).
                rotateX((float)Math.toRadians(-rotation.x)).
                rotateY((float)Math.toRadians(-rotation.y)).
                rotateZ((float)Math.toRadians(-rotation.z)).
                scale(gameItem.getScale());
        Matrix4f viewCurr = new Matrix4f(viewMatrix);
        return viewCurr.mul(modelViewMatrix);
    }
}
