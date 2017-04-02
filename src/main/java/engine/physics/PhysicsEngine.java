package engine.physics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import engine.EngineComponent;

/**
 * The physics engine tests for physical interactions between items in the world, 
 * and notifies listeners of those interactions
 * 
 * @author cypress980
 *
 */
public class PhysicsEngine implements EngineComponent {
    private static final Logger logger = LogManager.getLogger(PhysicsEngine.class.getName());
    private static final Listener DEFAULT_LISTENER = new DummyListener();
    
    private final Map<RigidBody, Listener> listeners;
    private List<Pair<RigidBody>> possibleInteractions;
    private float updateIntervalHint;
    
    public PhysicsEngine() {
	listeners = new HashMap<>();
    }
    
    public void registerListener(PhysicsEngine.Listener listener, RigidBody body) {
	listeners.put(body, listener);
    }
    
    /**
     * This method tests every body against every other body in the order received
     * It is up to the game to decide which objects are sent for testing
     * 
     * @throws Exception
     */
    public void simulatePhysics(float interval) throws Exception {
	
	for (Pair<RigidBody> pair : possibleInteractions) {
	    CollisionEvent collision = pair.a.getCollision(pair.b);
	    if (!collision.equals(CollisionEvent.NONE)) {

		//Notify of each interaction
		//This could probably be made more efficient by multithreading, but for now, we'll just 
		//pump out messages as we test on a single thread;
		Listener l1 = listeners.getOrDefault(collision.getMessageA().getBody(), DEFAULT_LISTENER);
		Listener l2 = listeners.getOrDefault(collision.getMessageB().getBody(), DEFAULT_LISTENER);
		
		l1.notifyOfCollision(collision.getMessageA());
		l2.notifyOfCollision(collision.getMessageB());
	    }
	}
    }
    
    public void setPossibleInteractions(List<Pair<RigidBody>> possibleInteractions) {
	this.possibleInteractions = possibleInteractions;
    }
    
    public static interface Listener {
	void notifyOfCollision(ElasticCollisionMessage message);
    }
    
    static class DummyListener implements Listener{
	@Override
	public void notifyOfCollision(ElasticCollisionMessage message) {
	    return; //Do Nothing
	}
    }
    
    public static class Pair<T> {
	final T a;
	final T b;
	
	public Pair(T a, T b) {
	    this.a = a;
	    this.b = b;
	}
    }

    @Override
    public void update(float interval) throws Exception{
	// Update bodies
	for (RigidBody body : listeners.keySet()) {
	    body.updatePhysics(interval);
	}
	// Test for physical interactions
	this.simulatePhysics(interval);
    }

    @Override
    public float getUpdateInterval() {
	return updateIntervalHint;
    }

    public void setUpdateIntervalHint(float updateIntervalHint) {
	this.updateIntervalHint = updateIntervalHint;
    }
}
