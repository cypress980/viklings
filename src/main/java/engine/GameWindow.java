package engine;

import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_FORWARD_COMPAT;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwGetKey;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowPos;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.glfw.GLFW.glfwSetWindowSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_TRUE;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.system.MemoryUtil.NULL;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.glfw.GLFWWindowSizeCallback;
import org.lwjgl.opengl.GL;

public class GameWindow {

    private final String title;

    private int width;

    private int height;

    private long windowHandle;

    private GLFWErrorCallback errorCallback;

    private GLFWKeyCallback keyCallback;

    private GLFWWindowSizeCallback windowSizeCallback;

    private boolean resized;
    
    public GameWindow(String title, int width, int height) {
	this.title = title;
	this.width = width;
	this.height = height;
	this.resized = false;
	
	init();
    }

    private void init() {
	// Setup an error callback. The default implementation
	// will print the error message in System.err.
	errorCallback = GLFWErrorCallback.createPrint(System.err);
	glfwSetErrorCallback(errorCallback);

	// Initialize GLFW. Most GLFW functions will not work before doing this.
	if (!glfwInit()) {
	    throw new IllegalStateException("Unable to initialize GLFW");
	}

	glfwDefaultWindowHints(); // optional, the current window hints are already the default
	glfwWindowHint(GLFW_VISIBLE, GL_FALSE); // the window will stay hidden after creation
	glfwWindowHint(GLFW_RESIZABLE, GL_TRUE); // the window will be resizable
	glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
	glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2);
	glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
	glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);
	
	// Create the window
	windowHandle = glfwCreateWindow(width, height, title, NULL, NULL);
	if (windowHandle == NULL) {
	    throw new RuntimeException("Failed to create the GLFW window");
	}

	// Setup resize callback
	windowSizeCallback = new GLFWWindowSizeCallback() {
	    @Override
	    public void invoke(long window, int width, int height) {
		GameWindow.this.width = width;
		GameWindow.this.height = height;
		GameWindow.this.setResized(true);
	    }
	};

	glfwSetWindowSizeCallback(windowHandle, windowSizeCallback);

	// Setup a key callback. It will be called every time a key is pressed, repeated or released.
	keyCallback = new GLFWKeyCallback() {
	    @Override
	    public void invoke(long window, int key, int scancode, int action, int mods) {
		if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
		    glfwSetWindowShouldClose(window, true);
		}
	    }
	};

	glfwSetKeyCallback(windowHandle, keyCallback);

	// Get the resolution of the primary monitor
	GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
	// Center our window
	glfwSetWindowPos(
		windowHandle,
		(vidmode.width() - width) / 2,
		(vidmode.height() - height) / 2
		);

	// Make the OpenGL context current
	glfwMakeContextCurrent(windowHandle);

	// Enable v-sync
	glfwSwapInterval(1);

	// Make the window visible
	glfwShowWindow(windowHandle);

	GL.createCapabilities();

	// Set the clear color
	glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

	glEnable(GL_DEPTH_TEST);
	
	// Support for transparencies
	glEnable(GL_BLEND);
	glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
	
	// For fun wireframes, uncomment this
	//glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
    }

    public void setClearColor(float r, float g, float b, float alpha) {
	glClearColor(r, g, b, alpha);
    }

    public boolean isKeyPressed(int keyCode) {
	return glfwGetKey(windowHandle, keyCode) == GLFW_PRESS;
    }

    public boolean windowShouldClose() {
	return glfwWindowShouldClose(windowHandle);
    }

    public String getTitle() {
	return title;
    }

    public int getWidth() {
	return width;
    }

    public int getHeight() {
	return height;
    }

    public boolean isResized() {
	return resized;
    }

    public void setResized(boolean resized) {
	this.resized = resized;
    }

    public void update() {
	glfwSwapBuffers(windowHandle);
	glfwPollEvents();
    }

    public long getWindowHandle() {
	return windowHandle;
    }
}