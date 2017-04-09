package engine.controls;

import static org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetMouseButtonCallback;

import java.util.HashMap;
import java.util.Map;

import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;

import engine.GameWindow;

/**
 * This class allows us to register listeners for keyboard and mouse events without extending LWJGL classes to do so
 * Instead we define our own interfaces, and tuck in the LWJGL inheritance inside this class.
 * 
 * TODO: We haven't actually added a way to unregister listeners - once you add a listener, it's listener.
 * Simply removing the listener, and letting the anonymous inner class get GC'd will result in errors.
 * We will need to grab the anon callbacks out of the map with the listener key, and unregister them with LWJGL
 * 
 * @author cypress980
 *
 */
public class ControlsEngine {
    private final Map<KeyboardListener, GLFWKeyCallback> keyboardListeners;
    private final Map<MouseListener, Pair<Object>> mouseListeners;
    private final GameWindow window;
    
    public ControlsEngine(GameWindow window) {
	keyboardListeners = new HashMap<>();
	mouseListeners = new HashMap<>();
	this.window = window;
    }
    
    public void addKeyboardListener(KeyboardListener keyboardListener) {
	// Setup a key callback. It will be called every time a key is pressed, repeated or released.
	GLFWKeyCallback passthruCallback = new GLFWKeyCallback() {
	    @Override
	    public void invoke(long window, int key, int scancode, int action, int mods) {
		keyboardListener.invoke(window, key, scancode, action, mods);
	    }
	};
	// The anonymous callback object is just a dumb passthrough method to appease the LWJGL interface
	// all it does is call the controller's invoke method
	// We store the anonymous object in the hashmap because we need to keep a reference so it doesn't get GC'd
	// as the code that actually calls the callback method is native, so no other reference will exist in the JVM
	keyboardListeners.put(keyboardListener, passthruCallback);
	
	//Register callback with LWJGL
	glfwSetKeyCallback(window.getWindowHandle(), passthruCallback);
    }
    
    public void addMouseListener(MouseListener controller) {
	GLFWCursorPosCallback cursorPosCallback = new GLFWCursorPosCallback() {
            @Override
            public void invoke(long window, double xpos, double ypos) {
                controller.cursorPositionEvent(window, xpos, ypos);
            }
        };
        
        //Register callback with LWJGL
        glfwSetCursorPosCallback(window.getWindowHandle(), cursorPosCallback);
        
        GLFWMouseButtonCallback mouseButtonCallback = new GLFWMouseButtonCallback() {
            @Override
            public void invoke(long window, int button, int action, int mods) {
                controller.mouseButtonEvent(window, button, action, mods);
            }
        };
        
        //Register callback with LWJGL
        glfwSetMouseButtonCallback(window.getWindowHandle(), mouseButtonCallback);
        
        //Add listener and both callback passthrus to our map
        mouseListeners.put(controller, new Pair<>(cursorPosCallback, mouseButtonCallback));
    }
    
    private static class Pair<T> {
	@SuppressWarnings("unused")
	final T a, b;
	
	Pair(T a, T b) {
	    this.a = a;
	    this.b = b;
	}
    }
}
