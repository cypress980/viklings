package viklings.prototype;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_W;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_X;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_Z;

import java.util.ArrayList;
import java.util.List;
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
    
    Sprite bjorn;
    
    @Override
    public void init(GameWindow window) throws Exception {
	// Set up scene renderer
	gameRenderer = new FlatRenderer();
	//scene.add(new Text("Hi Cuddlebug"));
	//        SpriteSheet font = new SpriteSheet("textures/sprites/font_texture.png", 16, 16);
	//        Sprite chr = new Sprite(font);
	//        chr.setFrame(48); //Should be zero
	//      scene.add(chr);
	SpriteSheet bjornSpriteSheet = new SpriteSheet("textures/sprites/vikling.png", 9, 1);
	bjorn = new Sprite(bjornSpriteSheet);
	bjorn.setFrame(1);
	bjorn.setPosition(50, 50);
	scene.add(bjorn);
	gameRenderer.setScene(scene);
	gameRenderer.setWindowHeightPx(window.getHeight());
	gameRenderer.setWindowWidthPx(window.getWidth());
	graphicsEngine.addRenderer(gameRenderer);
    }

    @Override
    public void input(GameWindow window, MouseInput mouseInput) {
	if (window.isKeyPressed(GLFW_KEY_W)) {
	    //
	} else if (window.isKeyPressed(GLFW_KEY_S)) {
	    //
	}
	if (window.isKeyPressed(GLFW_KEY_A)) {
	    //
	} else if (window.isKeyPressed(GLFW_KEY_D)) {
	    //
	}
	if (window.isKeyPressed(GLFW_KEY_Z)) {
	    //
	} else if (window.isKeyPressed(GLFW_KEY_X)) {
	    //
	}
    }

    @Override
    public void update(float interval, MouseInput mouseInput) {
	//Update game components

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
