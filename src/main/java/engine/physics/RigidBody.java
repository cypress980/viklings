package engine.physics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Vector3f;

public class RigidBody implements PhysicsEngine.Listener {
    private static final Logger logger = LogManager.getLogger(RigidBody.class.getName());
    
    private static final float FRICTION_COEF = 800f; // slow down by this many px per frame due to friction 
	     // after collision (includes constant normal force)

    private final HitBox hitBox;

    private float mass;

    private Vector3f velocity;
    
    private final List<Vector3f> collisions;
    
    /**
     * This is added to the displacement when a collision occurs to ensure collisions don't chain on eachother
     */
    private static final float OFFSET_DELTA = 0.0001f;
    
    public RigidBody(HitBox hitBox, float mass, Vector3f position, Vector3f velocity) {
	this.hitBox = hitBox;
	this.mass = mass;
	this.velocity = velocity;
	this.collisions = new ArrayList<>();
    }
    
    @Override
    public void notifyOfCollision(ElasticCollisionMessage message) {
	logger.debug("Collision! [{}]", message);
	collisions.add(message.getDv());
	hitBox.move(message.getDs());
	velocity.add(message.getDv());
    }
    
    public void updatePhysics(float interval) {
	
	// process collision
 	Vector3f collisionsDv = new Vector3f();
 	// Process Collision
 	// each collision event tells us the change in velocity immediately following the collision
 	// We then apply the friction coefficient to decelerate for the interval
 	// So what we went to do is tell the body the initial velocity change at the time of the collision event
 	// and then update it with a decayed velocity based on the friction
 	for (Iterator<Vector3f> iterator = collisions.iterator(); iterator.hasNext(); ) {
 	    Vector3f bounce = iterator.next();
 	    collisionsDv.add(new Vector3f(bounce).mul(interval));

 	    float dv = FRICTION_COEF * interval;
 	    if (dv >= bounce.length()) {
 		iterator.remove();
 	    } else {
 		bounce.sub(new Vector3f(bounce).normalize().mul(dv));
 	    }
 	}
 	
	velocity.add(collisionsDv);
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
	    // (subtracting v2 gives us the frame of reference where v2 is at rest because w2 = v2 - v2 = 0)
	    //
	    // This assumption lets us use a simple derivation:
	    // w1' = v * ( ( m1 - m2 ) / (m1 + m2) )
	    // w2' = v * ( 2 m1 / (m1 + m2)) 
	    //
	    // and
	    // v1' = w1' + v2
	    // v2' = w2' + v2

	    // Therefore
	    // dv1 = v1' - v1 = v * ( ( m1 - m2 ) / (m1 + m2) ) + v2 - v1
	    // dv2 = v2' - v2 = v * ( 2 m1 / (m1 + m2)) + v2 - v2

	    Vector3f v = new Vector3f(this.velocity).sub(b.velocity);
	    float massSum = this.mass + b.getMass();

	    Vector3f dv1 = (new Vector3f(v)).mul((this.mass - b.getMass()) / massSum).add(b.getVelocity()).sub(velocity);

	    Vector3f dv2 = (new Vector3f(v)).mul((2 * this.mass) / massSum).add(b.getVelocity());

	    //It's not enough here to only calculate the delta v. we also need delta position, aka displacement
	    // to ensure that these are behaving as rigid bodies.
	    // The correct method would be to scale the dv vector to fit in the intersection rectangle
	    // but that's a pain in the ass, so for now I'm just going to displace in the predominant direction of motion

	    Vector3f ds = new Vector3f();

	    // If v mostly goes in the x direction, displace in x
	    int maxComp = v.maxComponent();
	    if (maxComp == 0) {
		if (v.x > 0) {
		    //pos x, so must a.maxX must have hit b.minX
		    ds.x = this.hitBox.getMaxX() - b.hitBox.getMinX();
		} else {
		    ds.x = this.hitBox.getMinX() - b.hitBox.getMaxX();
		}
	    } else if (maxComp == 1) {
		// If v mostly goes in the y direction displace in y
		if (v.y > 0) {
		    //pos x, so must a.maxX must have hit b.minX
		    ds.y = this.hitBox.getMaxY() - b.hitBox.getMinY();
		} else {
		    ds.y = this.hitBox.getMinY() - b.hitBox.getMaxY();
		}
	    }
	    
	    return new CollisionEvent(
		    new ElasticCollisionMessage(this, dv1, new Vector3f()), 
		    new ElasticCollisionMessage(b, dv2, ds));
	}

	return CollisionEvent.NONE;
    }

    public float getMass() {
	return mass;
    }

    public void setMass(float mass) {
	this.mass = mass;
    }

    public Vector3f getVelocity() {
	return new Vector3f(velocity);
    }
    
    public void setVelocity(Vector3f velocity) {
	this.velocity = new Vector3f(velocity);
    }
    
    public HitBox getHitBox() {
	return hitBox;
    }

    public boolean isInCollision() {
	return !collisions.isEmpty();
    }

    @Override
    public String toString() {
	return "RigidBody [hitBox=" + hitBox + ", mass=" + mass + ", velocity=" + velocity + ", collisions="
		+ collisions + "]";
    }
}
