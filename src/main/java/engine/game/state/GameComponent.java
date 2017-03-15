package engine.game.state;

import graphics.Position;
import graphics.core.scene.SceneRenderable;

/**
 * Instances of this class will be containers for all in-game data.
 * 
 * @author cypress980
 *
 */
public class GameComponent implements SceneRenderable {
    
    private final String modelFile;
    private final String textureFile;
    
    private Position position;
    
    public GameComponent(String modelFile, String textureFile) {
	this.modelFile = modelFile;
	this.textureFile = textureFile;
	this.position = new Position();
    }
    
    public void setPosition(Position position) {
	this.position = position;
    }
    
    @Override
    public Position getPosition() {
        return position;
    }
    
    @Override
    public String getModelFile() {
        return modelFile;
    }
    
    @Override
    public String getTextureFile() {
	return textureFile;
    }
}
