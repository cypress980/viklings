package engine.controls;

public interface MouseListener {
    void cursorPositionEvent(long window, double xpos, double ypos);
    void mouseButtonEvent(long window, int button, int action, int mods);
}
