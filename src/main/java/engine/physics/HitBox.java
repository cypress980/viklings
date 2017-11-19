package engine.physics;

import org.joml.Vector3f;

public class HitBox {
    
    private final Vector3f position;
    
    private final float height, width, offsetY, offsetX;
    
    /**
     * 
     * @param position position of top left corner of hit box
     * @param height height of hit box
     * @param width width of hit box
     */
    public HitBox(Vector3f position, float height, float width) {
	this(position, height, width, 0, 0);
    }
    
    public HitBox(Vector3f position, float height, float width, float offsetY, float offsetX) {
	// By taking the raw Vector3f position, we anchor the hitbox to this. Meaning that this
	// position be changed, and we follow it. This is by design.
	this.position = position;
	this.height = height + offsetY;
	this.width = width + offsetX;
	this.offsetY = offsetY;
	this.offsetX = offsetX;
    }
    
    public float getMinX() {
        return position.x + offsetX;
    }

    public float getMaxX() {
        return position.x + width;
    }

    public float getMinY() {
        return position.y + offsetY;
    }

    public float getMaxY() {
        return position.y + height;
    }
    
    public Vector3f getPosition() {
	return new Vector3f(position); //Defensive Copy
    }
    
    public boolean isBounded(double xpos, double ypos) {
	return (xpos >= getMinX() && xpos <= getMaxX() && ypos >= getMinY() && ypos <= getMaxY());
    }

    public boolean isCollision(HitBox that) {
	
	return this.getMaxX() > that.getMinX() &&
		this.getMinX() < that.getMaxX() &&
		this.getMaxY() > that.getMinY() &&
		this.getMinY() < that.getMaxY();              

    }
    
    public Vector3f getIntersection(HitBox that) {
	Vector3f intersection = new Vector3f();
	
	if (!isCollision(that)) {
	    return intersection;
	}
	
	//If we're intersecting, find the intersection
	if (this.getMaxX() >= that.getMinX() && this.getMinX() <= that.getMinX()) {
	    // x-->+
	    // [this [-->] that] +
	    intersection.x = (this.getMaxX() - that.getMinX());
	} else if (that.getMaxX() >= this.getMinX() && that.getMinX() <= this.getMinX()) {
	    // x-->+
	    // [that [<--] this] -
	    intersection.x = (this.getMinX() - that.getMaxX());
	}
	
	if (this.getMaxY() >= that.getMinY() && this.getMaxY() <= that.getMaxY()) {
	    // y-->+
	    // [this [-->] that] +
	    intersection.y = (this.getMaxY() - that.getMinY());
	} else if (that.getMaxY() >= this.getMinY() && that.getMinY() <= this.getMinY()) {
	    // y-->+
	    // [that [<--] this] -
	    intersection.y = (this.getMinY() - that.getMaxY());
	}
	
	return intersection;
    }

    @Override
    public String toString() {
	return "HitBox [minX=" + getMinX() + ", maxX=" + getMaxX() + ", minY=" + getMinY() + ", maxY=" + getMaxY() + ", position="
		+ position + "]";
    }
}
