package engine.game.state;

import com.fasterxml.jackson.databind.ObjectMapper;

public class GameComponentByteArraySerDe implements GameComponentSerDe<byte[]> {
    private final ObjectMapper mapper = new ObjectMapper();
    
    @Override
    public GameComponent deserialize(byte[] in) throws Exception {
	return mapper.readValue(in, GameComponent.class);
    }
    
    @Override
    public byte[] serialize(GameComponent out) throws Exception {
	return mapper.writeValueAsBytes(out);
    }
}
