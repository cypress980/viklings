package graphics.scene;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import engine.GamePosition;
import engine.GameWindow;
import graphics.Model;
import graphics.Renderer;
import graphics.ResourceLoader;
import graphics.Transformation;
import graphics.debug.Hud;

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

    private final Transformation transformation;

    private ShaderProgram sceneShaderProgram;

    private final float specularPower;

	private ShaderProgram hudShaderProgram;

    public SceneRenderer() {
        transformation = new Transformation();
        specularPower = 10f;
    }

    public void init(GameWindow window) throws Exception {
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
        
        setupHudShader();
    }

    public void clear() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    public void render(GameWindow window, Camera camera, GamePosition[] gameItems, Vector3f ambientLight,
            PointLight[] pointLightList, SpotLight[] spotLightList, DirectionalLight directionalLight, Hud hud) {

        clear();

        if ( window.isResized() ) {
            glViewport(0, 0, window.getWidth(), window.getHeight());
            window.setResized(false);
        }

        sceneShaderProgram.bind();

        // Update projection Matrix
        Matrix4f projectionMatrix = transformation.getProjectionMatrix(FOV, window.getWidth(), window.getHeight(), Z_NEAR, Z_FAR);
        sceneShaderProgram.setUniform("projectionMatrix", projectionMatrix);

        // Update view Matrix
        Matrix4f viewMatrix = transformation.getViewMatrix(camera);

        // Update Light Uniforms
        renderLights(viewMatrix, ambientLight, pointLightList, spotLightList, directionalLight);

        sceneShaderProgram.setUniform("texture_sampler", 0);
        // Render each gameItem
        for (GamePosition gameItem : gameItems) {
            Model mesh = gameItem.getMesh();
            // Set model view matrix for this item
            Matrix4f modelViewMatrix = transformation.getModelViewMatrix(gameItem, viewMatrix);
            sceneShaderProgram.setUniform("modelViewMatrix", modelViewMatrix);
            // Render the mesh for this game item
            sceneShaderProgram.setUniform("material", mesh.getMaterial());
            mesh.render();
        }

        sceneShaderProgram.unbind();
        
        renderHud(window, hud);
    }
    
    private void renderHud(GameWindow window, Hud hud) {

        hudShaderProgram.bind();

        Matrix4f ortho = transformation.getOrthoProjectionMatrix(0, window.getWidth(), window.getHeight(), 0);
        for (GamePosition gameItem : hud.getGameItems()) {
            Model mesh = gameItem.getMesh();
            // Set orthographic and model matrix for this HUD item
            Matrix4f projModelMatrix = transformation.getOrthoProjModelMatrix(gameItem, ortho);
            hudShaderProgram.setUniform("projModelMatrix", projModelMatrix);
            hudShaderProgram.setUniform("color", gameItem.getMesh().getMaterial().getColor());

            // Render the mesh for this HUD item
            mesh.render();
        }

        hudShaderProgram.unbind();
    }

    private void renderLights(Matrix4f viewMatrix, Vector3f ambientLight,
            PointLight[] pointLightList, SpotLight[] spotLightList, DirectionalLight directionalLight) {

        sceneShaderProgram.setUniform("ambientLight", ambientLight);
        sceneShaderProgram.setUniform("specularPower", specularPower);

        // Process Point Lights
        int numLights = pointLightList != null ? pointLightList.length : 0;
        for (int i = 0; i < numLights; i++) {
            // Get a copy of the point light object and transform its position to view coordinates
            PointLight currPointLight = new PointLight(pointLightList[i]);
            Vector3f lightPos = currPointLight.getPosition();
            Vector4f aux = new Vector4f(lightPos, 1);
            aux.mul(viewMatrix);
            lightPos.x = aux.x;
            lightPos.y = aux.y;
            lightPos.z = aux.z;
            sceneShaderProgram.setUniform("pointLights", currPointLight, i);
        }

        // Process Spot Lights
        numLights = spotLightList != null ? spotLightList.length : 0;
        for (int i = 0; i < numLights; i++) {
            // Get a copy of the spot light object and transform its position and cone direction to view coordinates
            SpotLight currSpotLight = new SpotLight(spotLightList[i]);
            Vector4f dir = new Vector4f(currSpotLight.getConeDirection(), 0);
            dir.mul(viewMatrix);
            currSpotLight.setConeDirection(new Vector3f(dir.x, dir.y, dir.z));
            Vector3f lightPos = currSpotLight.getPointLight().getPosition();

            Vector4f aux = new Vector4f(lightPos, 1);
            aux.mul(viewMatrix);
            lightPos.x = aux.x;
            lightPos.y = aux.y;
            lightPos.z = aux.z;

            sceneShaderProgram.setUniform("spotLights", currSpotLight, i);
        }

        // Get a copy of the directional light object and transform its position to view coordinates
        DirectionalLight currDirLight = new DirectionalLight(directionalLight);
        Vector4f dir = new Vector4f(currDirLight.getDirection(), 0);
        dir.mul(viewMatrix);
        currDirLight.setDirection(new Vector3f(dir.x, dir.y, dir.z));
        sceneShaderProgram.setUniform("directionalLight", currDirLight);
    }
    
    private void setupHudShader() throws Exception {
        hudShaderProgram = new ShaderProgram();
        ResourceLoader resLoader = new ResourceLoader();
        hudShaderProgram.createVertexShader(resLoader.loadToString("shaders/hud/hud_vertex.vs"));
        hudShaderProgram.createFragmentShader(resLoader.loadToString("shaders/hud/hud_fragment.fs"));
        hudShaderProgram.link();
        
        // Create uniforms for orthographic-model projection matrix and base color
        hudShaderProgram.createUniform("projModelMatrix");
        hudShaderProgram.createUniform("color");
    }

    public void cleanup() {
        if (sceneShaderProgram != null) {
            sceneShaderProgram.cleanup();
        }
        if (hudShaderProgram != null) {
            hudShaderProgram.cleanup();
        }
    }

    /*\
     * 
     * 
     * 
     * 
     * 
     * 
     * 
     * 
    \*/
    
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
        Matrix4f projectionMatrix = transformation.getProjectionMatrix(FOV, windowWidthPx, windowHeightPx, Z_NEAR, Z_FAR);
        sceneShaderProgram.setUniform("projectionMatrix", projectionMatrix);

        // Update view Matrix
        Matrix4f viewMatrix = transformation.getViewMatrix(camera);
	
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
        Matrix4f modelViewMatrix = transformation.getModelViewMatrix(thing.getPosition(), viewMatrix);
        sceneShaderProgram.setUniform("modelViewMatrix", modelViewMatrix);
        // Render the mesh for this game item
        sceneShaderProgram.setUniform("material", model.getMaterial());
        model.render();
    }
}
