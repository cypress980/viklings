package engine.physics;

import org.joml.Vector3f;

public class RigidBody {
    
    private final HitBox hitBox;
    
    private float mass;
    
    private Vector3f velocity;
    
    private float frictionCoef = 5f;
    
    public RigidBody(HitBox hitBox, float mass, Vector3f position, Vector3f velocity) {
	this.hitBox = hitBox;
	this.mass = mass;
	this.velocity = velocity;
    }
    
    public CollisionEvent getCollision(RigidBody b) {
	
	if (this.hitBox.isCollision(b.getHitBox())) {
	    //to determine the new velocity of each point mass, we assume an elastic collision of point masses
	    // This tells us two things. 1) Momentum (p=m*v) is conserved:
	    // m1*v1 + m2*v2 = m1*v1' + m2*v2'
	    
	    // And 2) Kinetic Energy (KE = 1/2*mv^2) is Conserved, because no work was done during the collision:
	    // 1/2 m1*v1^2 + 1/2 m2*v2^2 = 1/2 m1*v1'^2 + 1/2 m2*v2'^2
	    
	    // Given m1 v1, and m2 v2
	    // Find v1' and v2'
	    
	    // First take v2 to be at rest, so v = v1 - v2. 
	    // (subtracting v2 gives us the frame of reference where v2 is at rest because v2 - v2 = 0)
	    //
	    // This assumption lets us use a simple derivation:
	    // v1 = v * ( ( m1 - m2 ) / (m1 + m2) )
	    // v2 = v * ( 2 m1 / (m1 + m2)) 
	    
	    Vector3f v = this.velocity.sub(b.velocity);
	    float massSum = this.mass + b.getMass();
	    
	    Vector3f dv1 = (new Vector3f(v)).sub((
		    new Vector3f(v)).mul((this.mass - b.getMass()) / massSum).add(b.getVelocity()));
	    
	    Vector3f dv2 = v.mul((2 * this.mass) / massSum).add(b.getVelocity());
	    
	    return new CollisionEvent(
		    new ElasticCollisionMessage(this, dv1), 
		    new ElasticCollisionMessage(b, dv2));
	}
	
	return CollisionEvent.NONE;
    }
    
    public void applyFriction(float interval) {
	float dv = frictionCoef*interval;
	if (dv >= velocity.length()) {
	    velocity = new Vector3f();
	} else {
	    velocity.sub(new Vector3f(velocity).normalize().mul(frictionCoef));
	}
    }

    public float getMass() {
        return mass;
    }

    public void setMass(float mass) {
        this.mass = mass;
    }

    public Vector3f getVelocity() {
        return velocity;
    }

    public void setVelocity(Vector3f velocity) {
        this.velocity = velocity;
    }

    public HitBox getHitBox() {
        return hitBox;
    }
    
}
