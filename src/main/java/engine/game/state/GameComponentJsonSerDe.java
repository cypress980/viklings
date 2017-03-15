package engine.game.state;

import com.fasterxml.jackson.databind.ObjectMapper;

public class GameComponentJsonSerDe implements GameComponentSerDe<String> {
    
    private final ObjectMapper mapper = new ObjectMapper();
    
    private boolean prettyPrint = false;
    
    public boolean isPrettyPrint() {
        return prettyPrint;
    }

    public void setPrettyPrint(boolean prettyPrint) {
        this.prettyPrint = prettyPrint;
    }

    @Override
    public GameComponent deserialize(String in) throws Exception {
	return mapper.readValue(in, GameComponent.class);
    }
    
    @Override
    public String serialize(GameComponent comp) throws Exception {
	if (prettyPrint) {
	    return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(comp);
	} else {
	    return mapper.writeValueAsString(comp);
	}
    }
}
