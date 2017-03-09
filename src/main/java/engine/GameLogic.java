package engine;

public interface GameLogic {

	void init(GameWindow window) throws Exception;

	void input(GameWindow window, MouseInput mouseInput);

	void update(float interval, MouseInput mouseInput);

	void render(GameWindow window);
	
	void cleanup();
}
