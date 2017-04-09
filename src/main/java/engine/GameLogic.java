package engine;

import java.util.List;

public interface GameLogic {

	void init(GameWindow window) throws Exception;

	void render(GameWindow window);
	
	void cleanup();
	
	List<EngineComponent> getEngineComponents();
}
