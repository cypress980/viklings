package engine.game.state;

import java.util.ArrayList;
import java.util.List;

import engine.EngineComponent;

public class GameStateEngine implements EngineComponent {
    
    private final float updateIntervalSeconds;
    private final List<Updatable> updatableItems;
    
    public GameStateEngine(float updateIntervalSeconds) {
	this.updateIntervalSeconds = updateIntervalSeconds;
	updatableItems = new ArrayList<>();
    }
    
    public void addUpdatableItem(Updatable item) {
	updatableItems.add(item);
    }
    
    @Override
    public void update(float interval) throws Exception {
	for (Updatable item : updatableItems) {
	    item.update(interval);
	}
    }

    @Override
    public boolean shouldUpdate(float interval)  {
	return interval >= updateIntervalSeconds;
    }
}
