package engine;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import engine.physics.PhysicsEngine;

public class GameEngine implements Runnable {
    private static final Logger logger = LogManager.getLogger(PhysicsEngine.class.getName());
    
    public static final int MAX_UPDATE_INTERVAL = 30;

    private final Thread gameLoopThread;
    private final GameLogic gameLogic;
    private final GameWindow window;
    private final GameTimer timer;
    
    private final Map<EngineComponent, Float> engineAccumulators;

    private boolean isPaused;

    public GameEngine(String windowTitle, int width, int height, boolean vsSync, GameLogic gameLogic) throws Exception {
	gameLoopThread = new Thread(this, "GAME_LOOP_THREAD");
	window = new GameWindow(windowTitle, width, height, vsSync);
	this.gameLogic = gameLogic;
	timer = new GameTimer();
	engineAccumulators = new HashMap<>();
	for (EngineComponent component : gameLogic.getEngineComponents() ) {
	    engineAccumulators.put(component, 0f);
	}
    }

    public void start() {
	String osName = System.getProperty("os.name");
	if ( osName.contains("Mac") ) {
	    gameLoopThread.run();
	} else {
	    gameLoopThread.start();
	}
    }

    @Override
    public void run() {
	try {
	    init();
	    gameLoop();
	} catch (Exception ex) {
	    ex.printStackTrace();
	} finally {
	    cleanup();
	}
    }

    private void cleanup() {
	gameLogic.cleanup();
    }

    protected void init() throws Exception {
	window.init();
	timer.init();
	gameLogic.init(window);
    }
    
    protected void gameLoop() throws Exception {
	float elapsedTime;
	
	boolean running = true;
	while (running && !window.windowShouldClose()) {
	    elapsedTime = timer.getElapsedTime();
	    //Handle if we break for debugging by assuming we at most 1/30th of a second passed
	    elapsedTime = elapsedTime < MAX_UPDATE_INTERVAL ? elapsedTime : MAX_UPDATE_INTERVAL;
	    
	    for (Entry<EngineComponent, Float> engineAccumulator : engineAccumulators.entrySet()) {
		float engAccum = engineAccumulator.getValue() + elapsedTime;
		EngineComponent engine = engineAccumulator.getKey();
		
		if (engAccum >= engine.getUpdateInterval()) {
		    logger.trace("Refresh [{}] after [{}]", engine, engAccum);
		    engine.update(engAccum);
		    engAccum = 0f;
		}
		
		engineAccumulators.put(engine, engAccum);
	    }
	    
	    render();
	}
    }

    protected void render() {
	gameLogic.render(window);
	window.update();
    }

    public void addEngineComponent(EngineComponent engineComponent) {
	engineAccumulators.put(engineComponent, 0f);
    }
}
