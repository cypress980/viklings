package engine.physics;

import org.joml.Vector3f;

public class ElasticCollisionMessage {
    private final RigidBody body;
    private final Vector3f v;
    
    public ElasticCollisionMessage(RigidBody body, Vector3f v) {
	this.body = body;
	this.v = v;
    }

    public RigidBody getBody() {
        return body;
    }

    public Vector3f getVelocity() {
        return v;
    }
}
