package graphics.core.scene;

import graphics.Position;
import graphics.core.Model;

public interface SceneRenderable {
    String getModelFile();
    String getTextureFile();
    Position getPosition();
}
