package engine.game.state;

public interface GameComponentSerDe<T> {

    GameComponent deserialize(T in) throws Exception;

    T serialize(GameComponent out) throws Exception;

}