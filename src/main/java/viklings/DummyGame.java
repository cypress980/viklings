package viklings;

import org.joml.Vector2f;
import org.joml.Vector3f;

import engine.GameComponent;
import engine.GameLogic;
import engine.GameWindow;
import engine.MouseInput;
import graphics.Position;
import graphics.GraphicsEngine;
import graphics.Material;
import graphics.Model;
import graphics.ObjLoader;
import graphics.Texture;
import graphics.debug.DebugRenderer;
import graphics.debug.DebugText;
import graphics.scene.Camera;
import graphics.scene.DirectionalLight;
import graphics.scene.PointLight;
import graphics.scene.SceneRenderer;
import graphics.scene.SpotLight;

import static org.lwjgl.glfw.GLFW.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;


public class DummyGame implements GameLogic {

    private static final float MOUSE_SENSITIVITY = 0.2f;

    private final Vector3f cameraInc;

    private SceneRenderer sceneRenderer;
    
    private DebugRenderer debugRenderer;
    
    private final Camera camera;
    
    private final Collection<GameComponent> gameComponents;

    private Vector3f ambientLight;

    private float lightAngle;

    private List<PointLight> pointLights = new ArrayList<>(5);

    private List<SpotLight> spotLights = new ArrayList<>(5);

    private DirectionalLight directionalLight;
    
    private GraphicsEngine graphicsEngine;

    private List<DebugText> debugTexts = new ArrayList<>();;

    private static final float CAMERA_POS_STEP = 0.05f;

    public DummyGame() {
	gameComponents = new ArrayList<>();
        camera = new Camera();
        cameraInc = new Vector3f(0.0f, 0.0f, 0.0f);
        graphicsEngine = new GraphicsEngine();
    }

    @Override
    public void init(GameWindow window) throws Exception {
	// Set up scene renderer
        sceneRenderer = new SceneRenderer();
        sceneRenderer.setCamera(camera);
        sceneRenderer.setWindowHeightPx(window.getHeight());
        sceneRenderer.setWindowWidthPx(window.getWidth());
        sceneRenderer.setScene(gameComponents);

        //Set up objects in scene
        ObjLoader meshMaker = new ObjLoader();
        GameComponent grassblock = new GameComponent();
        Position position = new Position();
        position.setScale(0.5f);
        position.setPosition(0, 0, -2);
        Model cube = meshMaker.loadMesh("models/cube.obj");
        Texture texture = new Texture("textures/grassblock.png");
        float reflectance = 1f;
        Material material = new Material(texture, reflectance);
        cube.setMaterial(material);
        grassblock.setModel(cube);
        grassblock.setPosition(position);
        gameComponents.add(grassblock);
        
        // Point Light
        Vector3f lightPosition = new Vector3f(0, 0, 1);
        float lightIntensity = 1.0f;
        PointLight pointLight = new PointLight(new Vector3f(1, 1, 1), lightPosition, lightIntensity);
        PointLight.Attenuation att = new PointLight.Attenuation(0.0f, 0.0f, 1.0f);
        pointLight.setAttenuation(att);
        pointLights.add(pointLight);
        sceneRenderer.setPointLights(pointLights);
        
        // Spot Light - added as game component because there can be many in game with positions in the world
        // Anything with a position in the world should be a GameComponent (TODO: should it be called "SceneComponent"?)
        lightPosition = new Vector3f(0, 0.0f, 10f);
        pointLight = new PointLight(new Vector3f(1, 1, 1), lightPosition, lightIntensity);
        att = new PointLight.Attenuation(0.0f, 0.0f, 0.02f);
        pointLight.setAttenuation(att);
        Vector3f coneDir = new Vector3f(0, 0, -1);
        float cutoff = (float) Math.cos(Math.toRadians(140));
        SpotLight spotLight = new SpotLight(pointLight, coneDir, cutoff);
        spotLights.add(spotLight);
        sceneRenderer.setSpotLights(spotLights);
        
        // Ambient Light - set in scene as it is global
        ambientLight = new Vector3f(0.3f, 0.3f, 0.3f);
        sceneRenderer.setAmbientLight(ambientLight);
        
        // Directional Light - set in scene as it is global
        lightPosition = new Vector3f(-1, 0, 0);
        directionalLight = new DirectionalLight(new Vector3f(1, 1, 1), lightPosition, lightIntensity);
        sceneRenderer.setDirectionalLight(directionalLight);
    	
        // Set up debug renderer
        debugRenderer = new DebugRenderer();
        DebugText debugText = new DebugText("Hi Cuddlebug");
        debugTexts.add(debugText);
        debugRenderer.setHud(debugTexts);
        debugRenderer.setWindowHeightPx(window.getHeight());
        debugRenderer.setWindowWidthPx(window.getWidth());
        
        graphicsEngine.addRenderer(sceneRenderer);
        graphicsEngine.addRenderer(debugRenderer);
    }

    @Override
    public void input(GameWindow window, MouseInput mouseInput) {
        cameraInc.set(0, 0, 0);
        if (window.isKeyPressed(GLFW_KEY_W)) {
            cameraInc.z = -1;
        } else if (window.isKeyPressed(GLFW_KEY_S)) {
            cameraInc.z = 1;
        }
        if (window.isKeyPressed(GLFW_KEY_A)) {
            cameraInc.x = -1;
        } else if (window.isKeyPressed(GLFW_KEY_D)) {
            cameraInc.x = 1;
        }
        if (window.isKeyPressed(GLFW_KEY_Z)) {
            cameraInc.y = -1;
        } else if (window.isKeyPressed(GLFW_KEY_X)) {
            cameraInc.y = 1;
        }
        float lightPos = spotLights.get(0).getPointLight().getPosition().z;
        if (window.isKeyPressed(GLFW_KEY_N)) {
            this.spotLights.get(0).getPointLight().getPosition().z = lightPos + 0.1f;
        } else if (window.isKeyPressed(GLFW_KEY_M)) {
            this.spotLights.get(0).getPointLight().getPosition().z = lightPos - 0.1f;
        }
    }

    @Override
    public void update(float interval, MouseInput mouseInput) {
        // Update camera position
        camera.movePosition(cameraInc.x * CAMERA_POS_STEP, cameraInc.y * CAMERA_POS_STEP, cameraInc.z * CAMERA_POS_STEP);

        // Update camera based on mouse            
        if (mouseInput.isRightButtonPressed()) {
            Vector2f rotVec = mouseInput.getDisplVec();
            camera.moveRotation(rotVec.x * MOUSE_SENSITIVITY, rotVec.y * MOUSE_SENSITIVITY, 0);
        }
        
        // Update directional light direction, intensity and colour
        lightAngle += 1.1f;
        if (lightAngle > 90) {
            directionalLight.setIntensity(0);
            if (lightAngle >= 360) {
                lightAngle = -90;
            }
        } else if (lightAngle <= -80 || lightAngle >= 80) {
            float factor = 1 - (float) (Math.abs(lightAngle) - 80) / 10.0f;
            directionalLight.setIntensity(factor);
            directionalLight.getColor().y = Math.max(factor, 0.9f);
            directionalLight.getColor().z = Math.max(factor, 0.5f);
        } else {
            directionalLight.setIntensity(1);
            directionalLight.getColor().x = 1;
            directionalLight.getColor().y = 1;
            directionalLight.getColor().z = 1;
        }
        double angRad = Math.toRadians(lightAngle);
        directionalLight.getDirection().x = (float) Math.sin(angRad);
        directionalLight.getDirection().y = (float) Math.cos(angRad);
    }
    
    @Override
    public void render(GameWindow window) {
        graphicsEngine.render();
    }

    @Override
    public void cleanup() {
        graphicsEngine.removeRenderer(debugRenderer);
        graphicsEngine.removeRenderer(sceneRenderer);
        for (GameComponent item : gameComponents) {
            if (item.getModel() != null) {
        	item.getModel().cleanUp();
            }
        }
    }

}