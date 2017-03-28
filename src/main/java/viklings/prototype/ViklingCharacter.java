package viklings.prototype;

import java.util.ArrayList;
import java.util.List;

import org.joml.Vector3f;

import engine.physics.RigidBody;
import graphics.flat.sprite.Sprite;
import graphics.flat.sprite.SpriteAnimator;

public class ViklingCharacter {
    
    private final Sprite sprite;
    
    private final List<Vector3f> moves;

    private final SpriteAnimator spriteAnimator;
    
    private final RigidBody body;
    
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
	//TODO: move responsibility for updating these to the physics engine
	body.updatePhysics(interval);
	
	//Update animation
	spriteAnimator.update(interval);
	
	//Find change in velocity
 	Vector3f dv;
 	//to calculate final velocity
 	Vector3f vf;
 	
 	if (!body.isInCollision()) {

 	    // If not in collision, Process Player Control Movement
 	    dv = calculateDeltaVForMovesForInterval(interval);
 	    vf = body.getVelocity().add(dv);
 	    
 	    //Enforce top speed though
 	    if (vf.length() > TOP_RUNNING_SPEED) {
 		vf.normalize().mul(TOP_RUNNING_SPEED);
 	    }
 	    
 	    // Calculate new velocity after the interval based on movements and forces, and set the body's new velocity
 	    body.setVelocity(vf);
 	}
	
	// Calculate displacement from collision or player movement control
	Vector3f ds = body.getVelocity().mul(interval); //Displacement is sum of previous velocity * time
	body.getHitBox().move(ds);
	sprite.move(ds);
    }
    
    private Vector3f calculateDeltaVForMovesForInterval(float interval) {
	// TODO: Figure out how to move physics to physics engine, and have controls in controls engine so that this
	// object becomes only responsible for orchestration
	
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
}
