package engine;

public interface EngineComponent {
    void update(float interval) throws Exception;
    boolean shouldUpdate(float interval);
}
