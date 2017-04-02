package engine;

import java.util.List;

public interface GameLogic {

	void init(GameWindow window) throws Exception;

	void input(GameWindow window, MouseInput mouseInput, float interval);

	/**
	 * 
	 * @param interval time elapsed since last update in seconds
	 */
	void update(float interval);

	void render(GameWindow window);
	
	void cleanup();
	
	List<EngineComponent> getEngineComponents();
}
