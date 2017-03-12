package engine;

import graphics.Position;
import graphics.Model;
import graphics.scene.SceneRenderable;

/**
 * Instances of this class will be containers for all in-game data.
 * 
 * @author cypress980
 *
 */
public class GameComponent implements SceneRenderable {
    
    private Position position;
    private Model model;
    
    @Override
    public Position getPosition() {
        return position;
    }
    
    public void setPosition(Position position) {
        this.position = position;
    }
    
    @Override
    public Model getModel() {
        return model;
    }
    
    public void setModel(Model model) {
        this.model = model;
    }
}
