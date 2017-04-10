package engine;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import engine.physics.PhysicsEngine;
import graphics.GraphicsEngine;

public class GameEngine implements Runnable {
    private static final Logger logger = LogManager.getLogger(PhysicsEngine.class.getName());
    
    private static final int MAX_UPDATE_INTERVAL = 30;

    private final Thread gameLoopThread;
    private final GameWindow window;
    private final GameTimer timer;
    
    private final Map<EngineComponent, Float> engineAccumulators;

    private GraphicsEngine graphicsEngine;
    
    public GameEngine(GameWindow gameWindow) {
	gameLoopThread = new Thread(this, "GAME_LOOP_THREAD");
	window = gameWindow;
	timer = new GameTimer();
	engineAccumulators = new HashMap<>();
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
	    timer.startTimer();
	    gameLoop();
	} catch (Exception ex) {
	    ex.printStackTrace();
	} finally {
	    cleanup();
	}
    }

    private void cleanup() {
	graphicsEngine.removeAllRenderers();
    }
    
    protected void gameLoop() throws Exception {
	float elapsedTime;
	
	boolean running = true;
	while (running && !window.windowShouldClose()) {
	    elapsedTime = timer.getElapsedTimeSeconds();
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
	graphicsEngine.render();
	window.update();
    }

    public void addEngineComponent(EngineComponent engineComponent) {
	engineAccumulators.put(engineComponent, 0f);
    }

    public void setGraphicsEngine(GraphicsEngine graphicsEngine) {
	this.graphicsEngine = graphicsEngine;
    }
}
