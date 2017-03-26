package engine.game.state;

import com.fasterxml.jackson.annotation.JsonProperty;

import graphics.core.scene.SceneRenderable;

/**
 * Instances of this class will be containers for all in-game data.
 * We want to be able to serialize state. We're going with jackson JSON for that
 * We can serialize to byte[] or json. This means we need our objects to be serializable
 * or define custom mappers to serialize them. We're going with annotations to make them 
 * easier to serialize
 * 
 * @author cypress980
 *
 */
public class GameComponent implements SceneRenderable {
    
    private String modelFile;
    private Material material;
    private Position position;
    
    public GameComponent(String modelFile, Material material) {
	this.modelFile = modelFile;
	this.material = material;
	this.position = new Position();
    }
    
    @JsonProperty
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
    public Material getMaterial() {
	return material;
    }
    
    //For SerDe
    GameComponent () {}
    
    //For SerDe
    @JsonProperty
    public void setModelFile(String modelFile) {
	this.modelFile = modelFile;
    }
    
    //For SerDe
    @JsonProperty
    public void setTextureFile(Material material) {
	this.material = material;
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + ((modelFile == null) ? 0 : modelFile.hashCode());
	result = prime * result + ((position == null) ? 0 : position.hashCode());
	result = prime * result + ((material == null) ? 0 : material.hashCode());
	return result;
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj)
	    return true;
	if (obj == null)
	    return false;
	if (getClass() != obj.getClass())
	    return false;
	GameComponent other = (GameComponent) obj;
	if (modelFile == null) {
	    if (other.modelFile != null)
		return false;
	} else if (!modelFile.equals(other.modelFile))
	    return false;
	if (position == null) {
	    if (other.position != null)
		return false;
	} else if (!position.equals(other.position))
	    return false;
	if (material == null) {
	    if (other.material != null)
		return false;
	} else if (!material.equals(other.material))
	    return false;
	return true;
    }
}
