package engine;

import java.util.HashMap;
import java.util.Map;

public class GameTimer {
    private static final int MAX_UPDATE_INTERVAL = 30;

    private double lastLoopTimeSeconds;
    
    private final Map<EngineComponent, Long> lastUpdates = new HashMap<>();
    
    public void startTimer() {
        lastLoopTimeSeconds = getTimeSeconds();
    }
    
    public double getTimeSeconds() {
        return System.currentTimeMillis() / 1_000.0;
    }

    public float getElapsedTimeSeconds() {
        double time = getTimeSeconds();
        float elapsedTime = (float) (time - lastLoopTimeSeconds);
        lastLoopTimeSeconds = time;
	
        //Handle if we break for debugging by assuming we at most 1/30th of a second passed
        return elapsedTime < MAX_UPDATE_INTERVAL ? elapsedTime : MAX_UPDATE_INTERVAL;
    }

    public double getLastLoopTimeSeconds() {
        return lastLoopTimeSeconds;
    }

    /**
     * Update the engine component if appropriate time has elapsed since last update.
     * 
     * @param e - EngineComponent to update
     * @return true if update occurred, else false
     * @throws Exception if update raises an exception
     */
    public boolean update(EngineComponent e) throws Exception {
	double lastUpdateMs = lastUpdates.getOrDefault(e, 0L);
	long nowMs = System.currentTimeMillis() ;
	float intervalSeconds = (float)(nowMs - lastUpdateMs) / 1_000.0F;
	intervalSeconds = intervalSeconds < MAX_UPDATE_INTERVAL ? intervalSeconds : MAX_UPDATE_INTERVAL;
	if (e.shouldUpdate(intervalSeconds)) {
	    lastUpdates.put(e, nowMs);
	    e.update(intervalSeconds);
	    return true;
	}
	return false;
    }
}
