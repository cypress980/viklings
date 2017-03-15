package graphics;

import org.joml.Vector3f;

public class Position {

    private final Vector3f coordinates;

    private float scale;

    private final Vector3f rotation;

    public Position() {
	coordinates = new Vector3f(0, 0, 0);
	scale = 1;
	rotation = new Vector3f(0, 0, 0);
    }
    
    public Vector3f getPosition() {
	return coordinates;
    }
    
    public void setCoordinates(float x, float y, float z) {
	this.coordinates.x = x;
	this.coordinates.y = y;
	this.coordinates.z = z;
    }
    
    public float getScale() {
	return scale;
    }
    
    public void setScale(float scale) {
	this.scale = scale;
    }
    
    public Vector3f getRotation() {
	return rotation;
    }
    
    public void setRotation(float x, float y, float z) {
	this.rotation.x = x;
	this.rotation.y = y;
	this.rotation.z = z;
    }
}