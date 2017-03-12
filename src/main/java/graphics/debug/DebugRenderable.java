package graphics.debug;

import engine.GamePosition;

public interface DebugRenderable {

    GamePosition[] getGameItems();

    default void cleanup() {
        GamePosition[] gameItems = getGameItems();
        for (GamePosition gameItem : gameItems) {
            gameItem.getModel().cleanUp();
        }
    }
}
