package engine.controls;

/**
 * The controller interface represents a mapping from keyboard events, like "A key pressed and released"
 * to game events, like a character jumped
 * 
 * @author cypress980
 *
 */
public interface KeyboardListener {
    void invoke(long window, int key, int scancode, int action, int mods);
}
