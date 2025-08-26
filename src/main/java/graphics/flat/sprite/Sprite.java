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

    private final SpriteSheet spriteSheet;
    private final Position position;
    private int frame = 0;
    private final float z;
    
    public Sprite(SpriteSheet spriteSheet) throws Exception {
        this.spriteSheet = spriteSheet;
        this.position = new Position();
        z = 0;
    }
    
    public Sprite(SpriteSheet spriteSheet, float z) throws Exception {
        this.spriteSheet = spriteSheet;
        this.position = new Position();
        this.z = z;
    }
    
    @Override
    public Position getPosition() {
        return position;
    }
    
    public void setFrame(int frame) {
	this.frame  = frame;
	spriteSheet.setFrame(frame);
    }
    
    public int getFrame() {
	return frame;
    }
    
    public void updateSize(GameWindow window) {
        this.setPosition(10f, window.getHeight() - 50f);
    }
    
    public void setPosition(float x, float y) {
        this.position.setCoordinates(x, y, z);
    }
    
    public void update() throws Exception {
	spriteSheet.update();
    }
    
    @Override
    public Model getModel() {
	return spriteSheet.getModel();
    }
}
