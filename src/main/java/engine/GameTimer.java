package engine;

public class GameTimer {
    private double lastLoopTimeSeconds;
    
    public void startTimer() {
        lastLoopTimeSeconds = getTimeSeconds();
    }
    
    public double getTimeSeconds() {
        return System.nanoTime() / 1_000_000_000.0;
    }

    public float getElapsedTimeSeconds() {
        double time = getTimeSeconds();
        float elapsedTime = (float) (time - lastLoopTimeSeconds);
        lastLoopTimeSeconds = time;
        return elapsedTime;
    }

    public double getLastLoopTimeSeconds() {
        return lastLoopTimeSeconds;
    }
}
