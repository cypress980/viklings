package graphics.debug;

import org.joml.Vector3f;

import engine.GameWindow;
import graphics.Position;
import graphics.core.Model;

/**
 * Simple wrapper for text item with default font
 * 
 * @author cypress980
 *
 */
public class DebugText implements DebugRenderable {

    private static final int FONT_COLS = 16;
    
    private static final int FONT_ROWS = 16;

    private static final String FONT_TEXTURE = "textures/font_texture.png";

    private final TextItem statusTextItem;

    public DebugText(String statusText) throws Exception {
        this.statusTextItem = new TextItem(statusText, FONT_TEXTURE, FONT_COLS, FONT_ROWS);
        this.statusTextItem.getModel().getMaterial().setColor(new Vector3f(1, 1, 1));
    }

    public void setStatusText(String statusText) {
        this.statusTextItem.setText(statusText);
    }

    @Override
    public Position getPosition() {
        return statusTextItem.getPosition();
    }
    
    public void updateSize(GameWindow window) {
        this.statusTextItem.setPosition(10f, window.getHeight() - 50f);
    }

    @Override
    public Model getModel() {
	return statusTextItem.getModel();
    }
}
