package viklings.prototype;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Vector3f;

import engine.EngineComponent;
import engine.GameEngine;
import engine.GameLogic;
import engine.GameWindow;
import engine.ai.IntelligenceEngine;
import engine.controls.ControlsEngine;
import engine.game.state.GameStateEngine;
import engine.physics.HitBox;
import engine.physics.PhysicsEngine;
import engine.physics.PhysicsEngine.Pair;
import engine.physics.RigidBody;
import graphics.GraphicsEngine;
import graphics.flat.Camera;
import graphics.flat.FlatRenderable;
import graphics.flat.FlatRenderer;
import graphics.flat.FlatScene;
import graphics.flat.Text;
import graphics.flat.sprite.Sprite;
import graphics.flat.sprite.SpriteSheet;
import viklings.prototype.ai.ViklingBrain;

/**
 * This is a 2D prototype for the viklings game
 * @author cypress980
 *
 */
public class ViklingsPrototype implements GameLogic {
    private static final Logger logger = LogManager.getLogger(ViklingsPrototype.class.getName());
    
    //TODO: make this a property
    private static final float PHYSICS_UPDATE_INTERVAL_SECONDS = 1f/60;
    
    //TODO: make this a property
    private static final float AI_UPDATE_INTERVAL_SECONDS = 1f/60f; 
    
    //TODO: make this a property
    private static final float GAME_STATE_UPDATE_INTERVAL_SECONDS = 1f/120f;
    
    
    
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
    
    private final GameStateEngine stateEngine;
    
    private GameController controller;
    
    private FlatRenderer gameRenderer;
    
    private final FlatScene scene;
    
    private final ArrayList<EngineComponent> gameEngineComponents;
    
    public ViklingsPrototype() {
	graphicsEngine = new GraphicsEngine();
	physicsEngine = new PhysicsEngine(PHYSICS_UPDATE_INTERVAL_SECONDS);
	aiEngine = new IntelligenceEngine(AI_UPDATE_INTERVAL_SECONDS);
	//TODO - Once we've extracted the graphics engine into this format, as the other engine components,
	// then we'll use the same update interval for the scene as for the graphics engine
	stateEngine = new GameStateEngine(GAME_STATE_UPDATE_INTERVAL_SECONDS);
	
	gameEngineComponents = new ArrayList<>();
	gameEngineComponents.add(physicsEngine);
	gameEngineComponents.add(aiEngine);
	gameEngineComponents.add(stateEngine);
	
	scene = new FlatScene();
    }
    
    Character bjorn;
    Character punchy;

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
	
	bjorn = new Character(bjornSprite, bjornPhsxBody, "bjorn");
	
	// Punchy
	SpriteSheet punchySpriteSheet = new SpriteSheet("textures/sprites/vikling.png", 9, 1);
	Sprite punchySprite = new Sprite(punchySpriteSheet, 0.21f);
	punchySprite.setFrame(1);
	punchySprite.setPosition(100, 100);
	
	Vector3f punchyphysxPosition = new Vector3f(112, 100, 0);
	HitBox punchyHitBox = new HitBox(punchyphysxPosition, 32, 12);
	RigidBody punchyPhsxBody = new RigidBody(punchyHitBox, 0.5f, punchyphysxPosition, new Vector3f());
	
	punchy = new Character(punchySprite, punchyPhsxBody, "punchy");
	
	//Punchy is AI, so register with AI.
	ViklingBrain punchyAi = new ViklingBrain(punchy, punchyPhsxBody);
	aiEngine.addAgent(punchyAi);
	
	physicsEngine.registerListener(bjornPhsxBody, bjornPhsxBody);
	physicsEngine.registerListener(punchyPhsxBody, punchyPhsxBody);
	
	physicsInteractions.add(new Pair<>(bjornPhsxBody, punchyPhsxBody));
	physicsEngine.setPossibleInteractions(physicsInteractions);
	//Add Terrain
	FlatRenderable terrain = terrainGenerator.generateTerrain();
	scene.addTerrain(terrain);
	
	//Add items to the scene
	SpriteSheet goldSpriteSheet = new SpriteSheet("textures/gold.png", 1, 1);
	Sprite goldSprite = new Sprite(goldSpriteSheet, 0.11f);
	goldSprite.setPosition(80f, 80f);
	scene.addItem(goldSprite);
	
	//Add characters
	scene.addCharacter(bjornSprite);
	scene.addCharacter(punchySprite);
	
	//Register characters for updates
	stateEngine.addUpdatableItem(bjorn);
	stateEngine.addUpdatableItem(punchy);
	
	//Add Debug text
	debugText = new Text("Hi Cuddlebug");
	scene.addToOverlay(debugText);
	
	gameRenderer.setScene(scene);
	
	gameRenderer.setWindowHeightPx(window.getHeight());
	gameRenderer.setWindowWidthPx(window.getWidth());
	graphicsEngine.addRenderer(gameRenderer);
	
	ControlsEngine controlsEngine = new ControlsEngine(window);
	controlsEngine.addKeyboardListener(new GameController(bjorn, camera));
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
