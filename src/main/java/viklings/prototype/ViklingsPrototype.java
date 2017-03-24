package viklings.prototype;

import static org.lwjgl.glfw.GLFW.*;

import java.util.ArrayList;
import java.util.List;

import org.joml.Vector3f;

import engine.GameEngine;
import engine.GameLogic;
import engine.GameWindow;
import engine.MouseInput;
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

    private FlatRenderer gameRenderer;

    private GraphicsEngine graphicsEngine;

    private List<FlatRenderable> scene = new ArrayList<>();
    
    public ViklingsPrototype() {
	graphicsEngine = new GraphicsEngine();
    }
    
    ViklingCharacter bjorn;
    
    @Override
    public void init(GameWindow window) throws Exception {
	// Set up scene renderer
	gameRenderer = new FlatRenderer();
	//        SpriteSheet font = new SpriteSheet("textures/sprites/font_texture.png", 16, 16);
	//        Sprite chr = new Sprite(font);
	//        chr.setFrame(48); //Should be zero
	//      scene.add(chr);
	scene.add(new Text("Hi Cuddlebug"));
	
	SpriteSheet bjornSpriteSheet = new SpriteSheet("textures/sprites/vikling.png", 9, 1);
	Sprite bjornSprite = new Sprite(bjornSpriteSheet, 1f);
	bjornSprite.setFrame(1);
	bjornSprite.setPosition(50, 50);
	
	bjorn = new ViklingCharacter(bjornSprite, 50, 50);
	scene.add(bjornSprite);
	
	gameRenderer.setScene(scene);
	gameRenderer.setWindowHeightPx(window.getHeight());
	gameRenderer.setWindowWidthPx(window.getWidth());
	graphicsEngine.addRenderer(gameRenderer);
    }

    @Override
    public void input(GameWindow window, MouseInput mouseInput) {
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
	if (window.isKeyPressed(GLFW_KEY_Z)) {
	    //
	} else if (window.isKeyPressed(GLFW_KEY_X)) {
	    //
	}
	
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
    public void update(float interval, MouseInput mouseInput) {
	try {
	    bjorn.update(interval);
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
