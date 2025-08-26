package engine.game.state;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class GameComponentByteArraySerDeTest {
    
    private GameComponentByteArraySerDe serDe = new GameComponentByteArraySerDe();
    private GameComponent grassblock;
    
    @Before
    public void setupGameComponent() throws Exception {
	float grassReflectance = 0.8f;
	Material grass = new Material("textures/grassblock.png", grassReflectance);
        grassblock = new GameComponent("models/cube.obj", grass);
        Position position = new Position();
        position.setScale(0.5f);
        position.setCoordinates(0, 0, -2);
        grassblock.setPosition(position);
    }
    
    @Test
    public void testSerDe() throws Exception {
	byte[] componentBytes = serDe.serialize(grassblock);
	GameComponent grassblockPhoenix = serDe.deserialize(componentBytes);
	
	Assert.assertEquals(grassblock, grassblockPhoenix);
    }
}
