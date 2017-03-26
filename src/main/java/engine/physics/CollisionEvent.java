package engine.physics;

public class CollisionEvent {
    public static final CollisionEvent NONE = new CollisionEvent();

    private final ElasticCollisionMessage a;
    private final ElasticCollisionMessage b;
    
    private CollisionEvent() {
	a = null;
	b = null;
    }
    
    public CollisionEvent(ElasticCollisionMessage a, ElasticCollisionMessage b) {
	this.a = a;
	this.b = b;
    }

    public ElasticCollisionMessage getMessageA() {
        return a;
    }

    public ElasticCollisionMessage getMessageB() {
        return b;
    }
}
