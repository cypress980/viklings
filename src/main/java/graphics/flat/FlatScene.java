package graphics.flat;

import java.util.ArrayList;
import java.util.List;

/**
 * The scene object is responsible for maintaining all of the renderable game state
 * 
 * If you want something to show up on the screen, you have to add it to the scene.
 * 
 * @author cypress980
 *
 */
public class FlatScene {
    
    private final List<FlatRenderable> sceneComponents;
    
    private static final float[] Z_ORDER = {0.0f, 0.1f, 0.2f, 0.3f};
    
    private static final float Z_ORDER_DELTA = 0.00001f; //10,000 items can be displayed at each layer.
    
    private static final int TERRAIN = 0;
    private static final int ITEMS = 1;
    private static final int CHARACTERS = 2;
    private static final int OVERLAY = 3;
    
    public FlatScene() {
	sceneComponents = new ArrayList<>();
    }
    
    public List<FlatRenderable> getSceneItems() {
	return sceneComponents;
    }
    
    public void addTerrain(FlatRenderable terrain) {
	add(terrain, Z_ORDER[TERRAIN]);
	Z_ORDER[TERRAIN] += Z_ORDER_DELTA;
    }
    
    public void addItem(FlatRenderable item) {
	add(item, Z_ORDER[ITEMS]);
	Z_ORDER[ITEMS] += Z_ORDER_DELTA;
    }
    
    public void addCharacter(FlatRenderable character) {
	add(character, Z_ORDER[CHARACTERS]);
	Z_ORDER[CHARACTERS] += Z_ORDER_DELTA;
    }
    
    public void addToOverlay(FlatRenderable item) {
	add(item, Z_ORDER[OVERLAY]);
	Z_ORDER[OVERLAY] += Z_ORDER_DELTA;
    }
    
    public void remove(FlatRenderable item) {
	sceneComponents.remove(item);
    }
    
    private void add(FlatRenderable item, float zOrder) {
	item.setZOrder(zOrder);
	sceneComponents.add(item);
    }
}
