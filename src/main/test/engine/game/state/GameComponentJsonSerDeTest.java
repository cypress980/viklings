package engine.game.state;

import org.junit.Before;
import org.junit.Test;

import graphics.Position;

public class GameComponentJsonSerDeTest {
    private GameComponentJsonSerDe serDe = new GameComponentJsonSerDe();
    private GameComponent grassblock;
    
    @Before
    public void setupGameComponent() throws Exception {
        grassblock = new GameComponent("models/cube.obj", "textures/grassblock.png");
        Position position = new Position();
        position.setScale(0.5f);
        position.setCoordinates(0, 0, -2);
        grassblock.setPosition(position);
    }
    
    @Test
    public void testSerialize() throws Exception {
	System.out.println(serDe.serialize(grassblock));
    }
}
