package viklings.prototype;

import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Vector3f;

import engine.game.state.Updatable;
import engine.physics.HitBox;
import engine.physics.RigidBody;
import graphics.flat.sprite.SpriteAnimator;
import viklings.prototype.setup.Inventory;
import viklings.prototype.setup.Needs;

public class Character implements Updatable {
    private static final Logger logger = LogManager.getLogger(Character.class.getName());
    
    private static final float TOP_RUNNING_SPEED = 100; //pixels per second
    private static final float ACCELERATION = 200; // 0.5s to get to top speed
    private static final float DECELERATION = 3000; // 0.05 s to stop from top speed
    
    private final SpriteAnimator animation;
    private final RigidBody body;
    private final String name;
    
    private final Vector3f positionDelta;

    private Communication voice;

    private Needs needs;

    private Inventory inventory;
    
    public static enum Move {
	STAND,
	RIGHT,
	LEFT,
	UP,
	DOWN;
    }

    public static Builder newBuilder() {
	return new Builder();
    }
    
    private Character(
	    String name, 
	    SpriteAnimator animation, 
	    RigidBody body, 
	    Communication voice, 
	    HitBox talkingHitBox, 
	    Needs needs, 
	    Inventory inventory) {
	this.name = name;
	this.animation = animation;
	this.body = body;
	this.voice = voice;
	this.talkingHitBox = talkingHitBox;
	this.needs = needs;
	this.inventory = inventory;
	positionDelta = new Vector3f(body.getPosition()).sub(animation.getPosition().getCoordinates());
    }
    
    private boolean moveRight = false;
    private boolean moveLeft = false;
    private boolean moveUp = false;
    private boolean moveDown = false;

    private final HitBox talkingHitBox;
    
    public void stop() {
	moveRight = false;
	moveLeft = false;
	moveUp = false;
	moveDown = false;
	animation.stopAnimation();
    }
    
    public void move(Move move, boolean go) {
	logger.debug("{} move event [{}], [{}]", name, move, go);
	switch(move) {
	case RIGHT:
	    animation.startAnimation(1);
	    moveRight = go;
	    if (go) {
		moveLeft = false;
	    }
	    break;
	case LEFT:
	    animation.startAnimation(1);
	    moveLeft = go;
	    if (go) {
		moveRight = false;
	    }
	    break;
	case UP:
	    animation.startAnimation(1);
	    moveUp = go;
	    if (go) {
		moveDown = false;
	    }
	    break;
	case DOWN:
	    animation.startAnimation(1);
	    moveDown = go;
	    if (go) {
		moveUp = false;
	    }
	    break;
	default:
	    break;
	}
    }

    @Override
    public void update(float interval) throws Exception {
	//Find change in velocity
 	Vector3f dv;
 	//to calculate final velocity
 	Vector3f vf;
 	
 	if (!body.isSliding()) {

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
	body.move(ds);
	//Don't move sprite independently. Instead, move it to where the hitbox is explicitly, respecting the initial
	//difference in position.
	Vector3f spritePos = body.getPosition().add(positionDelta);
	animation.setPosition(spritePos.x, spritePos.y);
	
	//Update animation
	animation.update(interval);
    }
    
    private Vector3f calculateDeltaVForMovesForInterval(float interval) {
	// Get current Velocity of body
	Vector3f bodyVelocity = body.getVelocity();
	Vector3f movesDv = new Vector3f();
	Vector3f acc = new Vector3f();
	
	if (moveRight) {
	    acc.x += ACCELERATION;
	} 

	if (moveLeft) {
	    acc.x -= ACCELERATION;
	}
	
	if (moveDown) {
	    acc.y += ACCELERATION;
	}
	
	if (moveUp) {
	    acc.y -= ACCELERATION;
	}
	
	if (!(moveUp || moveDown || moveRight || moveLeft)) {
	    animation.stopAnimation();
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
	
	logger.trace("{} moves dv [{}] for interval [{}]", name, movesDv, interval);
	
	return movesDv;
    }

    public void talk() {
	logger.debug("{}: Hi!", name);
	voice.say(this, Communication.Message.GREET, talkingHitBox);
    }
    
    public static class Builder {
	private Builder() {};
	
	private String name = null;
	private SpriteAnimator animation = null;
	private RigidBody body = null;
	private Communication voice = null;
	private HitBox talkingHitBox = null;
	private Needs needs;
	private Inventory inventory;
	
	public Character build() {
	    Objects.nonNull(name);
	    Objects.nonNull(animation);
	    Objects.nonNull(body);
	    Objects.nonNull(voice);
	    Objects.nonNull(talkingHitBox);
	    return new Character(
		    name,
		    animation,
		    body,
		    voice,
		    talkingHitBox,
		    needs,
		    inventory
		    );
	}

	public String getName() {
	    return name;
	}

	public Builder setName(String name) {
	    this.name = name;
	    return this;
	}
	
	public Builder setAnimation(SpriteAnimator animation) {
	    this.animation = animation;
	    return this;
	}

	public Builder setBody(RigidBody body) {
	    this.body = body;
	    return this;
	}

	public Builder setTalkingHitBox(HitBox talkingHitBox) {
	    this.talkingHitBox = talkingHitBox;
	    return this;
	}
	
	public Builder setVoice(Communication voice) {
	    this.voice = voice;
	    return this;
	}
	
	public Builder setNeeds(Needs needs) {
	    this.needs = needs;
	    return this;
	}

	public Builder setInventory(Inventory inventory) {
	    this.inventory = inventory;
	    return this;
	}
	
	public SpriteAnimator getAnimation() {
	    return animation;
	}

	public RigidBody getBody() {
	    return body;
	}

	public Communication getVoice() {
	    return voice;
	}

	public HitBox getTalkingHitBox() {
	    return talkingHitBox;
	}
    }
}
