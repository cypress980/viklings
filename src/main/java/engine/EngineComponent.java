package engine;

public interface EngineComponent {
    
    public void update(float interval) throws Exception;
    
    public float getUpdateInterval();
}
