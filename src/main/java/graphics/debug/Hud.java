package graphics.debug;

import engine.GamePosition;

public interface Hud {

    GamePosition[] getGameItems();

    default void cleanup() {
        GamePosition[] gameItems = getGameItems();
        for (GamePosition gameItem : gameItems) {
            gameItem.getMesh().cleanUp();
        }
    }
}
