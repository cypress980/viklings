package engine;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import engine.physics.PhysicsEngine;

public class GameEngine implements Runnable {
    private static final Logger logger = LogManager.getLogger(PhysicsEngine.class.getName());
    
    private static final float TARGET_FPS = 60;
    public static final int TARGET_UPS = 30;

    private final Thread gameLoopThread;
    private final GameLogic gameLogic;
    private final GameWindow window;
    private final GameTimer timer;
    private MouseInput mouseInput;
    
    private final Map<EngineComponent, Float> engineAccumulators;

    public GameEngine(String windowTitle, int width, int height, boolean vsSync, GameLogic gameLogic) throws Exception {
	gameLoopThread = new Thread(this, "GAME_LOOP_THREAD");
	window = new GameWindow(windowTitle, width, height, vsSync);
	this.gameLogic = gameLogic;
	this.mouseInput = new MouseInput();
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
	mouseInput.init(window);
    }

    //TODO: we need separate time scales for updating User Input, Physics, and Graphics
    protected void gameLoop() throws Exception {
	float elapsedTime;
	float accumulator = 0f;
	float interval = 1f / TARGET_UPS;

	boolean running = true;
	while (running && !window.windowShouldClose()) {
	    elapsedTime = timer.getElapsedTime();
	    
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
	    
	    accumulator += elapsedTime;
	    
	    while (accumulator >= interval) {
		input(interval);
		update(interval);
		accumulator -= interval;
	    }

	    render();

	    if (!window.isvSync()) {
		sync();
	    }
	}
    }

    private void sync() {
	float loopSlot = 1f / TARGET_FPS;
	double endTime = timer.getLastLoopTime() + loopSlot;
	while (timer.getTime() < endTime) {
	    try {
		Thread.sleep(1);
	    } catch (InterruptedException ie) {
	    }
	}
    }

    protected void input(float interval) {
	mouseInput.input(window);
	gameLogic.input(window, mouseInput, interval);
    }

    protected void update(float interval) {
	gameLogic.update(interval);
    }

    protected void render() {
	gameLogic.render(window);
	window.update();
    }

    public void addEngineComponent(EngineComponent engineComponent) {
	engineAccumulators.put(engineComponent, 0f);
    }
}
