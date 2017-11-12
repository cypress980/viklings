package graphics.flat.sprite;

import engine.game.state.Position;

public class SpriteAnimator {
    
    private int walkMin = 3, walkMax = 8; // [min, max]

    //private int turnRightMin = 1, turnRightMax = 2;
    
    private int standStillMin = 0, standStillMax = 0;

    private int currentAnimation;
    private int currentFrame = 0;
    private float secondsSinceFrameChange = 0;
    private static final int WALK_ANIM = 1;
    private static final int STAND_STILL = 0;
    
    private static float FRAME_DURATION = 0.150f;
    
    private final Sprite sprite;
    
    public SpriteAnimator(Sprite sprite) {
	this.sprite = sprite;
    }
    
    public void startAnimation(int animation) {
	if (currentAnimation == animation) {
	    return;
	}
	
	currentAnimation = animation;
	
	switch(currentAnimation) {
	
    	case STAND_STILL:
    	    setSpriteFrame(standStillMin);
    	    break;
    	case WALK_ANIM:
    	    setSpriteFrame(walkMin);
    	    break;
	}
    }
    
    public void update(float interval) throws Exception {
	secondsSinceFrameChange += interval;
	
	if (secondsSinceFrameChange >= FRAME_DURATION) {
	    nextFrame();
	    sprite.update();
	}
    }
    
    public void stopAnimation() {
	startAnimation(STAND_STILL);
    }
    
    private void nextFrame() throws Exception {
	switch(currentAnimation) {
    	case STAND_STILL:
    	    setSpriteFrame(++currentFrame % (1 + standStillMax - standStillMin) + standStillMin);
    	    break;
    	case WALK_ANIM:
    	    setSpriteFrame(currentFrame = ++currentFrame % (1 + walkMax - walkMin) + walkMin);
    	    break;
	}
    }
    
    private void setSpriteFrame(int frame) {
	this.sprite.setFrame(frame);
	currentFrame = frame;
	secondsSinceFrameChange = 0;
    }

    public Position getPosition() {
	return sprite.getPosition();
    }

    public void setPosition(float x, float y) {
	sprite.setPosition(x, y);
    }
}
