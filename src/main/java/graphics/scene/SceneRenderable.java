package graphics.scene;

import engine.GamePosition;
import graphics.Model;

public interface SceneRenderable {
    Model getModel();
    
    boolean hasPointLight();

    PointLight getPointLight();

    boolean hasSpotLight();
    
    SpotLight getSpotLight();

    GamePosition getPosition();
}
