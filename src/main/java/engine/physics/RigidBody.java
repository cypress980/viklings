package engine.physics;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Vector3f;

public class RigidBody implements PhysicsEngine.Listener {
    private static final Logger logger = LogManager.getLogger(RigidBody.class.getName());
    
    private static final float FRICTION_COEF = 1200f; // slow down by this many px per frame due to friction 
	     // after collision (includes constant normal force)

    private final HitBox hitBox;

    private float mass;

    private Vector3f velocity;
    
    private final List<ElasticCollisionMessage> collisionEvents;
    
    private boolean isSliding = false;

    private final Vector3f position;
    
    public RigidBody(HitBox hitBox, float mass, Vector3f position, Vector3f velocity) {
	this.hitBox = hitBox;
	this.mass = mass;
	this.velocity = velocity;
	this.collisionEvents = new ArrayList<>();
	this.position = new Vector3f(position);
    }
    
    @Override
    public void notifyOfCollision(ElasticCollisionMessage message) {
	logger.debug("Collision! [{}]", message);
	collisionEvents.add(message); //store the collision to process in the update
    }
    
    public void updatePhysics(float interval) {
	//Try this: Each collision is a single thing that happened since the last update.
	//Process all the displacements, then all the velocities, then add them to the current velocity.
	// After that, slow the velocity for friction.
	// Then calculate the final displacement from the collision displacements + velocity * interval, and move by that.
	for (ElasticCollisionMessage event : collisionEvents) {
	    //offset position
	    this.move(event.getDs());
	    this.velocity.add(event.getDv());
	}
	
	if (!collisionEvents.isEmpty()) {
	    isSliding = true;
	    //Clear collision events after processing
	    collisionEvents.clear();
	}
	
	//If we're in a collision, apply friction coefficient
	if (isSliding) {
	    float dvLen = FRICTION_COEF * interval;
	    if (dvLen > velocity.length()) {
		//If friction is greater than velocity, stop.
		velocity.set(0, 0, 0);
		//If we were in a collision, we're not anymore
		isSliding = false;
	    } else {
		Vector3f dv = new Vector3f(velocity).normalize().mul(dvLen);
		velocity.sub(dv);
	    }
	}
	
	//Finally, displace by velocity * interval
	Vector3f ds = new Vector3f(velocity).mul(interval);
	this.hitBox.move(ds);
    }
    
    public CollisionEvent getCollision(RigidBody b) {

	if (this.hitBox.isCollision(b.hitBox)) {
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
	    
	    Vector3f dv = new Vector3f(this.velocity).sub(b.velocity);
	    float massSum = this.mass + b.getMass();

	    Vector3f v1 = (new Vector3f(dv)).mul((this.mass - b.getMass()) / massSum);
	    Vector3f v2 = (new Vector3f(dv)).mul((2 * this.mass) / massSum);
	    
	    // But we need to return to the game's frame of reference.
	    // Remember, we determined v by taking v2 to be at rest, i.e. we did the calculation from v2's perspective
	    // now let's add v2's velocity back in
	    //
	    // v1' = w1' + v2
	    // v2' = w2' + v2
	    // 
	    // And lastly, we need to subtract the initial velocity of each to get the delta.
	    // 
	    // Therefore
	    // dv1 = v1' - v1 = v * ( ( m1 - m2 ) / (m1 + m2) ) + v2 - v1
	    // dv2 = v2' - v2 = v * ( 2 m1 / (m1 + m2)) + v2 - v2
	    
	    // Notice dv2 is actually = w2, because we initially assumed v2 to be 0.
	    
	    Vector3f dv1 = new Vector3f(v1).add(b.velocity).sub(this.velocity);
	    Vector3f dv2 = new Vector3f(v2);
	    
	    //It's not enough here to only calculate the delta v. we also need to displace them so they are no longer
	    // intersecting to ensure that these are behaving as rigid bodies.
	    Vector3f intersection = hitBox.getIntersection(b.hitBox);
	    
	    float dsLength;
	    Vector3f ds;
	    Vector3f ds1, ds2;
	    if (dv.length() > 0) {
		//Project the overlap of the rectangles onto the velocity vector
		// proj B onto A = A dot B / |A|^2 * A
		// so let B be our overlap, and A be our velocity
		dsLength = dv.dot(intersection) / dv.lengthSquared();
		ds = new Vector3f(dv).mul(dsLength); //Now make ds the length of dv
		
		//Now split it up proportionate to v1 & v2
		float dvTotal = dv1.length() + dv2.length();
		ds1 = new Vector3f(ds).negate().mul(dv1.length() / dvTotal);
		ds2 = new Vector3f(ds).mul(dv2.length() / dvTotal);
	    } else {
		intersection.setComponent(2, Float.MAX_VALUE); //Ignore z component for min
		int intersectionMin = intersection.minComponent();
		ds = (new Vector3f()).setComponent(intersectionMin, intersection.get(intersectionMin));
		
		//Since dv is too close to zero, instead of spliting up proportionate to v1 & v2
		// We will just split in half
		ds1 = new Vector3f(ds).mul(-0.5f);
		ds2 = new Vector3f(ds).mul(0.5f);
	    }
	    
	    return new CollisionEvent(
		    new ElasticCollisionMessage(this, dv1, ds1), 
		    new ElasticCollisionMessage(b, dv2, ds2));

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
    
    public Vector3f getPosition() {
	return new Vector3f(position);
    }
    
    public void move(Vector3f ds) {
	position.add(ds);
	hitBox.setPosition(position);
    }

    public void setPosition(Vector3f position) {
	this.position.set(position);
	hitBox.setPosition(position);
    }
    
    public boolean isSliding() {
	return isSliding;
    }
}
