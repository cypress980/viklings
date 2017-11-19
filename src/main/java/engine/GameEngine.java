package engine;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import graphics.GraphicsEngine;

public class GameEngine implements Runnable {
    private static final Logger logger = LogManager.getLogger(GameEngine.class.getName());
    
    private final Thread gameLoopThread;
    private final GameWindow window;
    private final GameTimer timer;
    
    private final Map<EngineComponent, Float> engineAccumulators;
    private final Set<EngineComponent> engineComponents;
    
    private GraphicsEngine graphicsEngine;
    
    public GameEngine(GameWindow window, GameTimer timer) {
	gameLoopThread = new Thread(this, "GAME_LOOP_THREAD");
	this.window = window;
	this.timer = timer;
	engineAccumulators = new HashMap<>();
	engineComponents = new HashSet<>();
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
	while (!window.windowShouldClose()) {
	    for (EngineComponent e : engineComponents) {
		timer.update(e);
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
	engineComponents.add(engineComponent);
    }

    public void setGraphicsEngine(GraphicsEngine graphicsEngine) {
	this.graphicsEngine = graphicsEngine;
    }
}
