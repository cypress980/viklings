package viklings.prototype.setup;

import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Vector3f;

import engine.ai.IntelligenceEngine;
import engine.controls.ControlsEngine;
import engine.controls.HitBoxMouseListener;
import engine.game.state.GameStateEngine;
import engine.physics.HitBox;
import engine.physics.PhysicsEngine;
import engine.physics.PhysicsEngine.Pair;
import engine.physics.RigidBody;
import graphics.flat.Camera;
import graphics.flat.FlatRenderable;
import graphics.flat.FlatScene;
import graphics.flat.Text;
import graphics.flat.sprite.Sprite;
import graphics.flat.sprite.SpriteAnimator;
import graphics.flat.sprite.SpriteSheet;
import viklings.prototype.Character;
import viklings.prototype.Communication;
import viklings.prototype.GameController;
import viklings.prototype.TerrainGenerator;
import viklings.prototype.ai.ViklingBrain;


/**
 * This is a 2D prototype for the viklings game
 * @author cypress980
 *
 */
public class ViklingsLogic {
    private static final Logger logger = LogManager.getLogger(ViklingsLogic.class.getName());
    
    private final PhysicsEngine physicsEngine;
    private final IntelligenceEngine aiEngine;
    private final GameStateEngine stateEngine;
    private final ControlsEngine controlsEngine;
    private final TerrainGenerator terrainGenerator;
    private final Camera camera;
    private final FlatScene scene;

    public ViklingsLogic(
	    PhysicsEngine physicsEngine, 
	    IntelligenceEngine aiEngine, 
	    GameStateEngine stateEngine, 
	    ControlsEngine controlsEngine,
	    TerrainGenerator terrainGenerator, 
	    Camera camera,
	    FlatScene scene) {
	this.physicsEngine = physicsEngine;
	this.aiEngine = aiEngine;
	this.stateEngine = stateEngine;
	this.controlsEngine = controlsEngine;
	this.terrainGenerator = terrainGenerator;
	this.camera = camera;
	this.scene = scene;
    }
    
    private final ArrayList<Pair<RigidBody>> physicsInteractions = new ArrayList<>();
    private Text debugText;
    
    //TODO: This entire method basically just does dependency injection and game setup
    // Introduce a dependency injection framework so that we don't have to have all this code sitting where game logic belongs
    // And work on a system to load the game declaratively
    public void setupGame() throws Exception {
	Communication communication = new Communication();
	
	// Bjorn
	SpriteSheet bjornSpriteSheet = new SpriteSheet("textures/sprites/vikling.png", 9, 1);
	Sprite bjornSprite = new Sprite(bjornSpriteSheet, 0.2f);
	bjornSprite.setFrame(1);
	bjornSprite.setPosition(50, 50);
	SpriteAnimator bjornAnimation = new SpriteAnimator(bjornSprite);
	
	Vector3f physxPosition = new Vector3f(62, 50, 0);
	HitBox bjornHitBox = new HitBox(physxPosition, 32, 12);
	RigidBody bjornPhsxBody = new RigidBody(bjornHitBox, 50, physxPosition, new Vector3f());
	HitBox talkingHitBox = new HitBox(physxPosition, 96, 60, -32, -24);
	Inventory bjornInventory = new Inventory();
	Needs bjornNeeds = new Needs();
	Character bjorn = Character.newBuilder()
		.setName("Bjorn")
		.setBody(bjornPhsxBody)
		.setAnimation(bjornAnimation)
		.setVoice(communication)
		.setTalkingHitBox(talkingHitBox)
		.setNeeds(bjornNeeds)
		.setInventory(bjornInventory)
		.build();
	
	// Punchy
	SpriteSheet punchySpriteSheet = new SpriteSheet("textures/sprites/vikling.png", 9, 1);
	Sprite punchySprite = new Sprite(punchySpriteSheet, 0.21f);
	punchySprite.setFrame(1);
	punchySprite.setPosition(100, 100);
	SpriteAnimator punchyAnimation = new SpriteAnimator(punchySprite);
	
	Vector3f punchyphysxPosition = new Vector3f(112, 100, 0);
	HitBox punchyHitBox = new HitBox(punchyphysxPosition, 32, 12);
	RigidBody punchyPhsxBody = new RigidBody(punchyHitBox, 0.5f, punchyphysxPosition, new Vector3f());
	HitBox punchyTalkingHitBox = new HitBox(punchyphysxPosition, 96, 60, -32, -24);
	
	Character punchy = Character.newBuilder()
		.setName("punchy")
		.setAnimation(punchyAnimation)
		.setBody(punchyPhsxBody)
		.setVoice(communication)
		.setTalkingHitBox(punchyTalkingHitBox)
		.build();
	
	//Punchy is AI, so register with AI.
	ViklingBrain punchyAi = new ViklingBrain(punchy, punchyPhsxBody, punchyTalkingHitBox);
	aiEngine.addAgent(punchyAi);
	communication.addListener(punchyAi);
	
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
	
	controlsEngine.addKeyboardListener(new GameController(bjorn, camera));
	controlsEngine.addMouseListener(new HitBoxMouseListener(bjornHitBox));
    }
}
