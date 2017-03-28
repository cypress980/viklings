package engine.physics;

import org.joml.Vector3f;

public class ElasticCollisionMessage {
    private final RigidBody body;
    private final Vector3f dv;
    private final Vector3f ds;
    
    public ElasticCollisionMessage(RigidBody body, Vector3f dv, Vector3f ds) {
	this.body = body;
	this.dv = dv;
	this.ds = ds;
    }

    public RigidBody getBody() {
        return body;
    }

    public Vector3f getDv() {
        return dv;
    }
    
    public Vector3f getDs() {
        return ds;
    }
}
