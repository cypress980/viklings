package engine;

import java.util.Optional;

import graphics.Model;
import graphics.scene.PointLight;
import graphics.scene.SceneRenderable;
import graphics.scene.SpotLight;

/**
 * Instances of this class will be containers for all in-game data.
 * 
 * @author cypress980
 *
 */
public class GameComponent implements SceneRenderable {
    
    private GamePosition position;
    private Model model;
    
    Optional<PointLight> pointLight = Optional.empty();
    Optional<SpotLight> spotLight = Optional.empty();
    
    @Override
    public GamePosition getPosition() {
        return position;
    }
    
    public void setPosition(GamePosition position) {
        this.position = position;
    }
    
    @Override
    public Model getModel() {
        return model;
    }
    
    public void setModel(Model model) {
        this.model = model;
    }
    
    public void setPointLight(PointLight pointLight) {
        this.pointLight = Optional.of(pointLight);
    }
    
    public void clearPointLight() {
	this.pointLight = Optional.empty();
    }
    
    public void setSpotLight(SpotLight spotLight) {
        this.spotLight = Optional.of(spotLight);
    }
    
    public void clearSpotLight() {
	this.spotLight = Optional.empty();
    }
    
    @Override
    public boolean hasPointLight() {
	return pointLight.isPresent();
    }
    
    @Override
    public PointLight getPointLight() {
	return pointLight.get();
    }
    
    @Override
    public boolean hasSpotLight() {
	return spotLight.isPresent();
    }
    
    @Override
    public SpotLight getSpotLight() {
	return spotLight.get();
    }
}
