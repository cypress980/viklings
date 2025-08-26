package graphics.core.scene;

import engine.game.state.Material;
import engine.game.state.Position;

public interface SceneRenderable {
    String getModelFile();
    Material getMaterial();
    Position getPosition();
}
