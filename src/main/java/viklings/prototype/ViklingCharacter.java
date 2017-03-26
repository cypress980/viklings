package viklings.prototype;

import java.util.ArrayList;
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
    
    //walk speed in position delta per second
    private static final float TOP_RUNNING_SPEED = 200;
    
    private static final float ACCELERATION = 90;
    private static final float DECELERATION = 150;
    
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
	Vector3f acc = new Vector3f();
	for (Vector3f move : moves) {
	    acc.add(move);
	}
	
	// TODO: this should probably live in the physics engine
	Vector3f velocity = new Vector3f(body.getVelocity());
	if(acc.length() != 0) {
	    acc.mul(interval);
	    //We want to move, so accelerate
	    velocity.add(acc);
	    
	    if (velocity.length() > TOP_RUNNING_SPEED) {
		velocity.normalize().mul(TOP_RUNNING_SPEED);
	    } else {
		body.getVelocity().add(acc.mul(interval));
	    }
	} else {
	    float v = velocity.length();
	    if (v != 0) {
		float deceleration = DECELERATION * interval;
		if (deceleration > 1) {
		    deceleration = 1;
		}
		if (v <= deceleration) {
		    velocity = new Vector3f(); //stop
		} else {
		    velocity.sub((new Vector3f(velocity)).mul(deceleration));
		}
	    }
	}
	
	for (Vector3f bounce : collisions) {
	    velocity.add(bounce);
	}
	
	Vector3f displacement = new Vector3f(velocity).mul(interval); //Displacement is sum of velocities * time
	
	sprite.move(displacement);
	body.setVelocity(velocity);
	body.getHitBox().move(displacement);
	spriteAnimator.update(interval);
	
	moves.clear();
    }

    @Override
    public void physicsUpdate(ElasticCollisionMessage message) {
	collisions.add(message.getVelocity());
    }
}
