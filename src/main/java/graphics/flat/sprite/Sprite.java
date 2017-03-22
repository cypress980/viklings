package graphics.flat.sprite;

import engine.GameWindow;
import engine.game.state.Position;
import graphics.core.Model;
import graphics.flat.FlatRenderable;

/**
 * A 2D image that can be rendered in our scene
 * 
 * @author cypress980
 *
 */
public class Sprite implements FlatRenderable {

    private final SpriteSheet sprite;
    private final Position position;
    private int frame = 0;
    
    public Sprite(SpriteSheet spriteSheet) throws Exception {
        this.sprite = spriteSheet;
        this.position = new Position();
    }
    
    @Override
    public Position getPosition() {
        return position;
    }
    
    public void setFrame(int frame) throws Exception {
	this.frame  = frame;
	sprite.setFrame(frame);
    }
    
    public int getFrame() {
	return frame;
    }
    
    public void updateSize(GameWindow window) {
        this.setPosition(10f, window.getHeight() - 50f);
    }
    
    public void setPosition(float x, float y) {
        this.position.setCoordinates(x, y, 0);
    }
    
    @Override
    public Model getModel() {
	return sprite.getModel();
    }
}
