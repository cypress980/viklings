package viklings.prototype;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_UP;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_W;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Vector3f;

import engine.EngineComponent;
import engine.GameEngine;
import engine.GameLogic;
import engine.GameWindow;
import engine.MouseInput;
import engine.ai.IntelligenceEngine;
import engine.physics.HitBox;
import engine.physics.PhysicsEngine;
import engine.physics.PhysicsEngine.Pair;
import engine.physics.RigidBody;
import graphics.GraphicsEngine;
import graphics.core.scene.Camera;
import graphics.flat.FlatRenderable;
import graphics.flat.FlatRenderer;
import graphics.flat.Text;
import graphics.flat.sprite.Sprite;
import graphics.flat.sprite.SpriteSheet;
import viklings.prototype.ViklingCharacter.Move;
import viklings.prototype.ai.ViklingBrain;

/**
 * This is a 2D prototype for the viklings game
 * @author cypress980
 *
 */
public class ViklingsPrototype implements GameLogic {
    private static final Logger logger = LogManager.getLogger(ViklingsPrototype.class.getName());
    
    //TODO: make this a property
    private static final float PHYSICS_UPDATE_INTERVAL_SECONDS = 1f/120f;
    
    //TODO: make this a property
    private static final float AI_UPDATE_INTERVAL_SECONDS = 1f/60f;
    
    public static void main(String[] args) {
	try {
	    boolean vSync = true;
	    //TODO: I need to get rid of this game engine tutorial shit, where we call this class the game logic,
	    // and the engine runs the logic. It's silly and confusing.
	    // Instead, this will just be the startingpoint of the game. We will compose the game here
	    // This means creating all the game engine components, and loading up all the game state.
	    // Then we start the game engine.
	    // To accomplish this, we still need to do a couple of things. 
	    // 
	    // 1, we need to move the controls out to their own class, and plug them into the game somehow
	    // 2, we need to move the remaining game update logic out of this class
	    // 3, we need to rework the graphics engine to work in the engine component framework
	    GameLogic gameLogic = new ViklingsPrototype();
	    GameEngine gameEng = new GameEngine("Viklings 2D", 600, 480, vSync, gameLogic);
	    gameEng.start();
	} catch (Exception excp) {
	    excp.printStackTrace();
	    System.exit(-1);
	}
    }
    
    private final GraphicsEngine graphicsEngine;

    private final PhysicsEngine physicsEngine;
    
    private final IntelligenceEngine aiEngine;
    
    private FlatRenderer gameRenderer;
    
    private List<FlatRenderable> scene = new ArrayList<>();

    private final ArrayList<EngineComponent> gameEngineComponents;
    
    public ViklingsPrototype() {
	graphicsEngine = new GraphicsEngine();
	physicsEngine = new PhysicsEngine(PHYSICS_UPDATE_INTERVAL_SECONDS);
	aiEngine = new IntelligenceEngine(AI_UPDATE_INTERVAL_SECONDS);
	gameEngineComponents = new ArrayList<>();
	gameEngineComponents.add(physicsEngine);
	gameEngineComponents.add(aiEngine);
    }
    
    ViklingCharacter bjorn;
    ViklingCharacter punchy;

    private final ArrayList<Pair<RigidBody>> physicsInteractions = new ArrayList<>();
    private Text debugText;

    private Camera camera;

    private TerrainGenerator terrainGenerator;
    
    //TODO: This entire method basically just does dependency injection and game setup
    // Introduce a dependency injection framework so that we don't have to have all this code sitting where game logic belongs
    // And work on a system to load the game declaratively
    @Override
    public void init(GameWindow window) throws Exception {
	// Set up scene renderer
	camera = new Camera();
	gameRenderer = new FlatRenderer(camera);
	
	//Terrain Generator
	terrainGenerator = new TerrainGenerator(camera);
	
	// Bjorn
	SpriteSheet bjornSpriteSheet = new SpriteSheet("textures/sprites/vikling.png", 9, 1);
	Sprite bjornSprite = new Sprite(bjornSpriteSheet, 0.2f);
	bjornSprite.setFrame(1);
	bjornSprite.setPosition(50, 50);
	
	Vector3f physxPosition = new Vector3f(62, 50, 0);
	HitBox bjornHitBox = new HitBox(physxPosition, 32, 12);
	RigidBody bjornPhsxBody = new RigidBody(bjornHitBox, 50, physxPosition, new Vector3f());
	
	bjorn = new ViklingCharacter(bjornSprite, bjornPhsxBody);
	
	// Punchy
	SpriteSheet punchySpriteSheet = new SpriteSheet("textures/sprites/vikling.png", 9, 1);
	Sprite punchySprite = new Sprite(punchySpriteSheet, 0.21f);
	punchySprite.setFrame(1);
	punchySprite.setPosition(100, 100);
	
	Vector3f punchyphysxPosition = new Vector3f(112, 100, 0);
	HitBox punchyHitBox = new HitBox(punchyphysxPosition, 32, 12);
	RigidBody punchyPhsxBody = new RigidBody(punchyHitBox, 0.5f, punchyphysxPosition, new Vector3f());
	
	punchy = new ViklingCharacter(punchySprite, punchyPhsxBody);
	
	//Punchy is AI, so register with AI.
	ViklingBrain punchyAi = new ViklingBrain(punchy, punchyPhsxBody);
	aiEngine.addAgent(punchyAi);
	
	physicsEngine.registerListener(bjornPhsxBody, bjornPhsxBody);
	physicsEngine.registerListener(punchyPhsxBody, punchyPhsxBody);
	
	physicsInteractions.add(new Pair<>(bjornPhsxBody, punchyPhsxBody));
	physicsEngine.setPossibleInteractions(physicsInteractions);
	//Add Terrain
	FlatRenderable terrain = terrainGenerator.generateTerrain();
	scene.add(terrain);
	
	//Add items to the scene
	SpriteSheet goldSpriteSheet = new SpriteSheet("textures/gold.png", 1, 1);
	Sprite goldSprite = new Sprite(goldSpriteSheet, 0.11f);
	goldSprite.setPosition(80f, 80f);
	scene.add(goldSprite);
	
	//Add characters
	scene.add(bjornSprite);
	scene.add(punchySprite);
	
	//Add Debug text
	debugText = new Text("Hi Cuddlebug", 0.3f);
	scene.add(debugText);
	
	gameRenderer.setScene(scene);
	gameRenderer.setWindowHeightPx(window.getHeight());
	gameRenderer.setWindowWidthPx(window.getWidth());
	graphicsEngine.addRenderer(gameRenderer);
    }
    
    private boolean isPaused = false;
    
    
    @Override
    public void input(GameWindow window, MouseInput mouseInput, float interval) {
	inputPauseControls(window, mouseInput, interval);
	inputCameraControls(window, mouseInput);
	
	if (!isPaused) {
	    inputCharacterControls(window, mouseInput);
	}
    }
    
    private boolean isSpacePressed = false;
    
    private void inputPauseControls(GameWindow window, MouseInput mouseInput, float interval) {
	//Since this is a toggled key, we need to have a cooldown on it, so we dont rapidly toggle on and off
	if (isSpacePressed && !window.isKeyPressed(GLFW_KEY_SPACE)) {
	    isPaused = !isPaused;
	}
	
	isSpacePressed = window.isKeyPressed(GLFW_KEY_SPACE);
    }
    
    private void inputCharacterControls(GameWindow window, MouseInput mouseInput) {
	if (window.isKeyPressed(GLFW_KEY_W)) {
	    bjorn.move(Move.UP);
	}
	
	if (window.isKeyPressed(GLFW_KEY_S)) {
	    bjorn.move(Move.DOWN);
	} 
	
	if (window.isKeyPressed(GLFW_KEY_A)) {
	    bjorn.move(Move.LEFT);
	} 

	if (window.isKeyPressed(GLFW_KEY_D)) {
	    bjorn.move(Move.RIGHT);
	} 

	if (!window.isKeyPressed(GLFW_KEY_D) &&
		!window.isKeyPressed(GLFW_KEY_W) &&
		!window.isKeyPressed(GLFW_KEY_S) &&
		!window.isKeyPressed(GLFW_KEY_A)) {
	    bjorn.move(Move.STAND);
	}
    }
    
    private void inputCameraControls(GameWindow window, MouseInput mouseInput) {
	float dxCam = 0, dyCam = 0;
	
	if (window.isKeyPressed(GLFW_KEY_UP)) {
	    dyCam = .02f;
	} else if (window.isKeyPressed(GLFW_KEY_DOWN)) {
	    dyCam = -.02f;
	}
	
	if (window.isKeyPressed(GLFW_KEY_RIGHT)) {
	    dxCam = .02f;
	} else if (window.isKeyPressed(GLFW_KEY_LEFT)) {
	    dxCam = -.02f;
	}
	
	camera.movePosition(dxCam, dyCam, 0);
    }

    @Override
    public void update(float interval) {
	try {
	    if (isPaused) { 
		if (!debugText.getText().equals("Paused!")) {
		    debugText.setText("Paused!");
		}
		return;
	    } else if (!isPaused && !debugText.getText().equals("Hi Cuddlebug!")) {
		debugText.setText("Hi Cuddlebug!");
	    }
	    
	    bjorn.update(interval);
	    punchy.update(interval);
	} catch (Exception e) {
	    logger.error("Exception updating game logic!", e);
	    throw new RuntimeException();
	}
    }

    @Override
    public void render(GameWindow window) {
	graphicsEngine.render();
    }

    @Override
    public void cleanup() {
	graphicsEngine.removeRenderer(gameRenderer);
    }

    @Override
    public List<EngineComponent> getEngineComponents() {
	return gameEngineComponents;
    }
}
