package viklings.prototype;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.joml.Vector3f;

import engine.physics.RigidBody;
import engine.physics.ElasticCollisionMessage;
import engine.physics.PhysicsEngine;

import graphics.flat.sprite.Sprite;
import graphics.flat.sprite.SpriteAnimator;

public class ViklingCharacter implements PhysicsEngine.Listener {
    
    private final Sprite sprite;
    
    private final List<Vector3f> moves;
    
    private final List<Vector3f> collisions;

    private final SpriteAnimator spriteAnimator;
    
    private final RigidBody body;

    private static final float FRICTION_COEF = 800f; // slow down by this many px per frame due to friction 
    						     // after collision (includes constant normal force)
    
    //walk speed in position delta per second
    private static final float TOP_RUNNING_SPEED = 200; //pixels per second
    
    private static final float ACCELERATION = 400; // 0.5s to get to top speed
    private static final float DECELERATION = 4000; // 0.05 s to stop from top speed
    
    public static enum Move {
	STAND,
	RIGHT,
	LEFT,
	UP,
	DOWN;
    }
    
    public ViklingCharacter(Sprite sprite, RigidBody body) {
	this.sprite = sprite;
	this.spriteAnimator = new SpriteAnimator(sprite);
	this.moves = new ArrayList<>();
	this.collisions = new ArrayList<>();
	this.body = body;
    }
    
    public void move(Move move) {
	Vector3f acc = new Vector3f();
	
	switch(move) {
	case STAND:
	    spriteAnimator.stopAnimation();
	    break;
	case RIGHT:
	    spriteAnimator.startAnimation(1);
	    acc.x += ACCELERATION;
	    break;
	case LEFT:
 	    spriteAnimator.startAnimation(1);
	    acc.x -= ACCELERATION;
	    break;
	case UP:
	    spriteAnimator.startAnimation(1);
	    acc.y -= ACCELERATION;
	    break;
	case DOWN:
	    spriteAnimator.startAnimation(1);
	    acc.y += ACCELERATION;
	}
	
	if (move != Move.STAND) {
	    moves.add(acc);
	}
    }

    public void update(float interval) throws Exception {
	
	//Update animation
	spriteAnimator.update(interval);
	
	//Find change in velocity
 	Vector3f dv;
 	//to calculate final velocity
 	Vector3f vf;
 	
	if (collisions.isEmpty()) {
	    // If not in collision, Process Player Control Movement
	    dv = calculateDeltaVForMovesForInterval(interval);
	    vf = (new Vector3f(body.getVelocity())).add(dv);
	    //Enforce top speed though
	    if (vf.length() > TOP_RUNNING_SPEED) {
		vf.normalize().mul(TOP_RUNNING_SPEED);
	    }
	} else {
	    // otherwise process collision
	    dv = calculateDeltaVForCollisionFrictionForInterval(interval);
	    vf = (new Vector3f(body.getVelocity())).add(dv);
	}
	
	// Calculate new velocity after the interval based on movements and forces, and set the body's new velocity
	
	body.setVelocity(vf);
	
	// Calculate displacement from collision or player movement control
	Vector3f ds = new Vector3f(body.getVelocity()).mul(interval); //Displacement is sum of previous velocity * time
	sprite.move(ds);
	body.getHitBox().move(ds);
    }
    
    @Override
    public void physicsUpdate(ElasticCollisionMessage message) {
	collisions.add(message.getDv());
	body.getHitBox().move(message.getDs());
	sprite.move(message.getDs());
	body.getVelocity().add(message.getDv());
    }
    
    private Vector3f calculateDeltaVForMovesForInterval(float interval) {
	// Get current Velocity of body
	Vector3f bodyVelocity = body.getVelocity();
	Vector3f movesDv = new Vector3f();
	Vector3f acc = new Vector3f();
	
	for (Vector3f move : moves) {
	    acc.add(move);
	}

	// first x
	if (acc.x != 0) {
	    //increase velocity by the acceleration over the last interval
	    movesDv.x += acc.x * interval;
	} else if (bodyVelocity.x > 0) {
	    //try to stop in positive direction
	    float stoppingDv = DECELERATION * interval;

	    if (bodyVelocity.x <= stoppingDv) {
		movesDv.x = -bodyVelocity.x;
	    } else {
		movesDv.x -= stoppingDv;
	    }
	} else if (bodyVelocity.x < 0) {
	    //negative
	    float stoppingDv = 0 - DECELERATION * interval;

	    if (bodyVelocity.x >= stoppingDv) {
		movesDv.x = -bodyVelocity.x;
	    } else {
		movesDv.x -= stoppingDv;
	    }
	}

	// now y
	if (acc.y != 0) {
	    //increase velocity by the acceleration over the last interval
	    movesDv.y += acc.y * interval;
	} else if (bodyVelocity.y > 0) {
	    //positive
	    float stoppingDv = DECELERATION * interval;

	    if (bodyVelocity.y <= stoppingDv) {
		movesDv.y = -bodyVelocity.y;
	    } else {
		movesDv.y -= stoppingDv;
	    }
	} else if (bodyVelocity.y < 0) {
	    //negative
	    float stoppingDv = 0 - DECELERATION * interval;

	    if (bodyVelocity.y >= stoppingDv) {
		movesDv.y = -bodyVelocity.y;
	    } else {
		movesDv.y -= stoppingDv;
	    }
	}
	
	//Finished these moves
	moves.clear();
	
	return movesDv;
    }
    
    private Vector3f calculateDeltaVForCollisionFrictionForInterval(float interval) { 
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
	return collisionsDv;
    }

}
