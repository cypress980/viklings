package graphics.flat;

import engine.game.state.Position;
import graphics.core.Model;

public interface FlatRenderable {
    Model getModel();
    Position getPosition();
    void setZOrder(float zOrder);
}
