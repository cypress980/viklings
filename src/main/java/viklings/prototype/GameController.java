package viklings.prototype;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_T;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_UP;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_W;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;

import engine.controls.KeyboardListener;
import graphics.flat.Camera;
import viklings.prototype.Character.Move;

public class GameController implements KeyboardListener {

    private final Character guy;
    private final Camera camera;

    public GameController(Character guy, Camera camera) {
        this.guy = guy;
        this.camera = camera;
    }

    @Override
    public void invoke(long window, int key, int scancode, int action, int mods) {
        //Control guy
        if (key == GLFW_KEY_W && action == GLFW_PRESS) {
            guy.move(Move.UP, true);
        } else if (key == GLFW_KEY_W && action == GLFW_RELEASE) {
            guy.move(Move.UP, false);
        }

        if (key == GLFW_KEY_S && action == GLFW_PRESS) {
            guy.move(Move.DOWN, true);
        } else if (key == GLFW_KEY_S && action == GLFW_RELEASE) {
            guy.move(Move.DOWN, false);
        }

        if (key == GLFW_KEY_A && action == GLFW_PRESS) {
            guy.move(Move.LEFT, true);
        } else if (key == GLFW_KEY_A && action == GLFW_RELEASE) {
            guy.move(Move.LEFT, false);
        }

        if (key == GLFW_KEY_D && action == GLFW_PRESS) {
            guy.move(Move.RIGHT, true);
        } else if (key == GLFW_KEY_D && action == GLFW_RELEASE) {
            guy.move(Move.RIGHT, false);
        }

        if (key == GLFW_KEY_T && action == GLFW_PRESS) {
            guy.talk();
        }

        if (key == GLFW_KEY_S && action == GLFW_PRESS) {
            guy.search();
        }

        // control camera
        float dxCam = 0, dyCam = 0;

        if (key == GLFW_KEY_UP) {
            dyCam = .02f;
        } else if (key == GLFW_KEY_DOWN) {
            dyCam = -.02f;
        }

        if (key == GLFW_KEY_RIGHT) {
            dxCam = .02f;
        } else if (key == GLFW_KEY_LEFT) {
            dxCam = -.02f;
        }

        camera.movePosition(dxCam, dyCam, 0);

        // Control window
        if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
            glfwSetWindowShouldClose(window, true);
        }

        if (key == GLFW_KEY_SPACE && action == GLFW_RELEASE) {

        }
    }

}
