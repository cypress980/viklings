package viklings.prototype.setup;

import engine.GameEngine;
import engine.GameTimer;
import engine.GameWindow;
import engine.ai.IntelligenceEngine;
import engine.controls.ControlsEngine;
import engine.game.state.GameStateEngine;
import engine.physics.PhysicsEngine;
import graphics.GraphicsEngine;
import graphics.flat.Camera;
import graphics.flat.FlatRenderer;
import graphics.flat.FlatScene;
import viklings.prototype.TerrainGenerator;


/**
 * This class does Dependency Injection for us.
 * 
 * I may upgrade to a fancy DI framework at some point.
 * 
 * I tried dagger, but it had a fatal flaw in my opinion:
 * To build the dependency graph, you needed to use a class that was generated
 * by an annotation. Eclipse would only generate that annotation for me if I enabled
 * compiler annotation processing for the project, and imported the jar.
 * This would create a separate workflow for building the project, all for some
 * annotation magic. Fuck that. I'll just encapsulate my dependency injection setup
 * all over here in plain old java code. It's not really that bad.
 * 
 * @author cypress980
 *
 */
public class ViklingsSetup {
    
    private static final float PHYSICS_UPDATE_INTERVAL_SECONDS = 1f/60;
    private static final float AI_UPDATE_INTERVAL_SECONDS = 1f/60f; 
    private static final float GAME_STATE_UPDATE_INTERVAL_SECONDS = 1f/120f;
    
    public GameEngine getGameEngine() throws Exception {
	GameWindow gameWindow = new GameWindow("Viklings 2D", 600, 480);
	
	GraphicsEngine graphicsEngine = new GraphicsEngine();
	PhysicsEngine physicsEngine = new PhysicsEngine(PHYSICS_UPDATE_INTERVAL_SECONDS);
	IntelligenceEngine aiEngine = new IntelligenceEngine(AI_UPDATE_INTERVAL_SECONDS);
	
	//TODO - Once we've extracted the graphics engine into this format, as the other engine components,
	// then we'll use the same update interval for the scene as for the graphics engine
	GameStateEngine stateEngine = new GameStateEngine(GAME_STATE_UPDATE_INTERVAL_SECONDS);
	
	FlatScene scene = new FlatScene();
	
	// Set up scene renderer
	Camera camera = new Camera();
	FlatRenderer gameRenderer = new FlatRenderer(camera);
	gameRenderer.setScene(scene);
	gameRenderer.setWindowHeightPx(gameWindow.getHeight());
	gameRenderer.setWindowWidthPx(gameWindow.getWidth());
	
	//Terrain Generator
	TerrainGenerator terrainGenerator = new TerrainGenerator(camera);
	
	graphicsEngine.addRenderer(gameRenderer);
	
	ControlsEngine controlsEngine = new ControlsEngine(gameWindow);
	
	GameTimer timer = new GameTimer();
	GameEngine gameEngine = new GameEngine(gameWindow, timer);
	
	gameEngine.addEngineComponent(physicsEngine);
	gameEngine.addEngineComponent(aiEngine);
	gameEngine.addEngineComponent(stateEngine);
	
	gameEngine.setGraphicsEngine(graphicsEngine);
	
	ViklingsLogic logic = new ViklingsLogic(
		physicsEngine, 
		aiEngine, 
		stateEngine, 
		controlsEngine, 
		terrainGenerator, 
		camera, 
		scene);
	
	logic.setupGame();
	
	return gameEngine;
    }
}
