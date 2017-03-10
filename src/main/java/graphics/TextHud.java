package graphics;

import org.joml.Vector3f;

import engine.GameItem;
import engine.GameWindow;
import engine.Hud;
import engine.TextItem;

public class TextHud implements Hud {

    private static final int FONT_COLS = 16;
    
    private static final int FONT_ROWS = 16;

    private static final String FONT_TEXTURE = "textures/font_texture.png";

    private final GameItem[] gameItems;

    private final TextItem statusTextItem;

    public TextHud(String statusText) throws Exception {
        this.statusTextItem = new TextItem(statusText, FONT_TEXTURE, FONT_COLS, FONT_ROWS);
        this.statusTextItem.getMesh().getMaterial().setColor(new Vector3f(1, 1, 1));
        gameItems = new GameItem[]{statusTextItem};
    }

    public void setStatusText(String statusText) {
        this.statusTextItem.setText(statusText);
    }

    @Override
    public GameItem[] getGameItems() {
        return gameItems;
    }
    
    public void updateSize(GameWindow window) {
        this.statusTextItem.setPosition(10f, window.getHeight() - 50f, 0);
    }
}
