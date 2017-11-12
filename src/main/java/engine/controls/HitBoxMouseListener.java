package engine.controls;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import engine.physics.HitBox;

public class HitBoxMouseListener implements MouseListener {
    private static final Logger logger = LogManager.getLogger(HitBoxMouseListener.class.getName());
    
    private final HitBox hitBox;
    
    private boolean isPointerInHitBox = false;
    
    public HitBoxMouseListener(HitBox hitBox) {
	this.hitBox = hitBox;
    }
    
    @Override
    public void cursorPositionEvent(long window, double xpos, double ypos) {
	logger.debug("Mouse Position Event | [{}, {}] | Hitbox [{}]", xpos, ypos, hitBox);
	boolean isBounded = hitBox.isBounded(xpos, ypos);
	if (isBounded == isPointerInHitBox) {
	    //Do Nothing
	} else if (isBounded) {
	    logger.debug("Mouse pointer entered hitbox");
	    isPointerInHitBox = true;
	} else {
	    logger.debug("Mouse pointer left hitbox");
	    isPointerInHitBox = false;
	}
    }

    @Override
    public void mouseButtonEvent(long window, int button, int action, int mods) {
	if (isPointerInHitBox) {
	    logger.debug("Mouse Event | button: [{}] | action [{}] | mods [{}]", button, action, mods);
	}
    }

}
