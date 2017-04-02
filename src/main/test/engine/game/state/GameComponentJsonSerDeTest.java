package engine.game.state;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import engine.physics.PhysicsEngine;

public class GameComponentJsonSerDeTest {
    private static final Logger logger = LogManager.getLogger(GameComponentJsonSerDeTest.class.getName());
    
    private GameComponentJsonSerDe serDe = new GameComponentJsonSerDe();
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
	String json = serDe.serialize(grassblock);
	logger.debug("We deserialized an object to this json: [{}]", json);
	GameComponent grassblockPhoenix = serDe.deserialize(json);
	System.out.println(json);
	Assert.assertEquals(grassblock, grassblockPhoenix);
    }
}
