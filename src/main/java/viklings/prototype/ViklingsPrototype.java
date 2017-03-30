package viklings.prototype;

import static org.lwjgl.glfw.GLFW.*;

import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Vector3f;

import engine.GameEngine;
import engine.GameLogic;
import engine.GameWindow;
import engine.MouseInput;
import engine.physics.HitBox;
import engine.physics.PhysicsEngine;
import engine.physics.PhysicsEngine.Pair;
import engine.physics.RigidBody;
import graphics.GraphicsEngine;
import graphics.flat.FlatRenderable;
import graphics.flat.FlatRenderer;
import graphics.flat.Text;
import graphics.flat.sprite.Sprite;
import graphics.flat.sprite.SpriteSheet;
import viklings.prototype.ViklingCharacter.Move;

/**
 * This is a 2D prototype for the viklings game
 * @author cypress980
 *
 */
public class ViklingsPrototype implements GameLogic {
    static final Logger logger = LogManager.getLogger(ViklingsPrototype.class.getName());
    
    public static void main(String[] args) {
	try {
	    boolean vSync = true;
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
    
    private FlatRenderer gameRenderer;
    
    private List<FlatRenderable> scene = new ArrayList<>();
    
    public ViklingsPrototype() {
	graphicsEngine = new GraphicsEngine();
	physicsEngine = new PhysicsEngine();
    }
    
    ViklingCharacter bjorn;
    ViklingCharacter punchy;

    private final ArrayList<Pair<RigidBody>> physicsInteractions = new ArrayList<>();
    private Text debugText;
    
    @Override
    public void init(GameWindow window) throws Exception {
	// Set up scene renderer
	gameRenderer = new FlatRenderer();
	debugText = new Text("Hi Cuddlebug");
	scene.add(debugText);
	
	// Bjorn
	SpriteSheet bjornSpriteSheet = new SpriteSheet("textures/sprites/vikling.png", 9, 1);
	Sprite bjornSprite = new Sprite(bjornSpriteSheet, 0.1f);
	bjornSprite.setFrame(1);
	bjornSprite.setPosition(50, 50);
	
	Vector3f physxPosition = new Vector3f(62, 50, 0);
	HitBox bjornHitBox = new HitBox(physxPosition, 32, 12);
	RigidBody bjornPhsxBody = new RigidBody(bjornHitBox, 50, physxPosition, new Vector3f());
	
	bjorn = new ViklingCharacter(bjornSprite, bjornPhsxBody);
	
	// Punchy
	SpriteSheet punchySpriteSheet = new SpriteSheet("textures/sprites/vikling.png", 9, 1);
	Sprite punchySprite = new Sprite(punchySpriteSheet, 0.2f);
	punchySprite.setFrame(1);
	punchySprite.setPosition(100, 100);
	
	Vector3f punchyphysxPosition = new Vector3f(112, 100, 0);
	HitBox punchyHitBox = new HitBox(punchyphysxPosition, 32, 12);
	RigidBody punchyPhsxBody = new RigidBody(punchyHitBox, 40, punchyphysxPosition, new Vector3f());
	
	punchy = new ViklingCharacter(punchySprite, punchyPhsxBody);
	
	physicsEngine.registerListener(bjornPhsxBody, bjornPhsxBody);
	physicsEngine.registerListener(punchyPhsxBody, punchyPhsxBody);
	
	physicsInteractions.add(new Pair<>(bjornPhsxBody, punchyPhsxBody));
	physicsEngine.setPossibleInteractions(physicsInteractions);
	
	scene.add(bjornSprite);
	scene.add(punchySprite);
	
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
	    dyCam = .01f;
	} else if (window.isKeyPressed(GLFW_KEY_DOWN)) {
	    dyCam = -.01f;
	}
	
	if (window.isKeyPressed(GLFW_KEY_RIGHT)) {
	    dxCam = .01f;
	} else if (window.isKeyPressed(GLFW_KEY_LEFT)) {
	    dxCam = -.01f;
	}
	
	this.gameRenderer.moveCamera(new Vector3f(dxCam, dyCam, 0));
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
	    
	    physicsEngine.simulatePhysics(interval);
	    bjorn.update(interval);
	    punchy.update(interval);
	} catch (Exception e) {
	    e.printStackTrace();
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
}
