package graphics.debug;

import org.joml.Vector3f;

import engine.GamePosition;
import engine.GameWindow;

public class DebugText implements DebugRenderable {

    private static final int FONT_COLS = 16;
    
    private static final int FONT_ROWS = 16;

    private static final String FONT_TEXTURE = "textures/font_texture.png";

    private final GamePosition[] gameItems;

    private final TextItem statusTextItem;

    public DebugText(String statusText) throws Exception {
        this.statusTextItem = new TextItem(statusText, FONT_TEXTURE, FONT_COLS, FONT_ROWS);
        this.statusTextItem.getModel().getMaterial().setColor(new Vector3f(1, 1, 1));
        gameItems = new GamePosition[]{statusTextItem};
    }

    public void setStatusText(String statusText) {
        this.statusTextItem.setText(statusText);
    }

    @Override
    public GamePosition[] getGameItems() {
        return gameItems;
    }
    
    public void updateSize(GameWindow window) {
        this.statusTextItem.setPosition(10f, window.getHeight() - 50f, 0);
    }
}
