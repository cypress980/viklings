package engine.game.state;

import org.joml.Vector2f;
import org.joml.Vector3f;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Position {

    private final Vector3f coordinates;

    private float scale;

    private final Vector3f rotation;

    public Position() {
	coordinates = new Vector3f(0, 0, 0);
	scale = 1;
	rotation = new Vector3f(0, 0, 0);
    }
    
    public Vector3f getCoordinates() {
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
    
    @JsonCreator
    Position(
	    @JsonProperty("coordinates") Vector3f coordinates, 
	    @JsonProperty("scale") float scale, 
	    @JsonProperty("rotation") Vector3f rotation) {
	this.coordinates = coordinates;
	this.scale = scale;
	this.rotation = rotation;
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + ((coordinates == null) ? 0 : coordinates.hashCode());
	result = prime * result + ((rotation == null) ? 0 : rotation.hashCode());
	result = prime * result + Float.floatToIntBits(scale);
	return result;
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj)
	    return true;
	if (obj == null)
	    return false;
	if (getClass() != obj.getClass())
	    return false;
	Position other = (Position) obj;
	if (coordinates == null) {
	    if (other.coordinates != null)
		return false;
	} else if (!coordinates.equals(other.coordinates))
	    return false;
	if (rotation == null) {
	    if (other.rotation != null)
		return false;
	} else if (!rotation.equals(other.rotation))
	    return false;
	if (Float.floatToIntBits(scale) != Float.floatToIntBits(other.scale))
	    return false;
	return true;
    }

    public void change(Vector3f displacement) {
	this.coordinates.add(displacement);
    }
}