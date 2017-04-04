package engine.physics;

import org.joml.Vector3f;

public class HitBox {
    
    private float minX, maxX, minY, maxY;
    
    private final Vector3f position;
    
    private final float height, width;
    
    /**
     * 
     * @param position position of top left corner of hit box
     * @param height height of hit box
     * @param width width of hit box
     */
    public HitBox(Vector3f position, float height, float width) {
	this.position = new Vector3f(position);
	
	this.height = height;
	this.width = width;
	
	minX = position.x;
	minY = position.y;
	
	maxX = minX + width;
	maxY = minY + height;
    }
    
    public float getMinX() {
        return minX;
    }

    public float getMaxX() {
        return maxX;
    }

    public float getMinY() {
        return minY;
    }

    public float getMaxY() {
        return maxY;
    }
    
    public void move(Vector3f displacement) {
	position.add(displacement);
	
	minX += displacement.x;
	minY += displacement.y;
	maxX += displacement.x;
	maxY += displacement.y;
    }
    
    public Vector3f getPosition() {
	return new Vector3f(position); //Defensive Copy
    }
    
    public void setPosition(Vector3f position) {
	this.position.set(position);
	
	minX = position.x;
	minY = position.y;
	
	maxX = minX + width;
	maxY = minY + height;
    }

    public boolean isCollision(HitBox that) {
	
	return this.maxX > that.minX &&
		this.minX < that.maxX &&
		this.maxY > that.minY &&
		this.minY < that.maxY;              

    }
    
    public Vector3f getIntersection(HitBox that) {
	Vector3f intersection = new Vector3f();
	
	if (!isCollision(that)) {
	    return intersection;
	}
	
	//If we're intersecting, find the intersection
	if (this.maxX >= that.minX && this.minX <= that.minX) {
	    // x-->+
	    // [this [-->] that] +
	    intersection.x = (this.maxX - that.minX);
	} else if (that.maxX >= this.minX && that.minX <= this.minX) {
	    // x-->+
	    // [that [<--] this] -
	    intersection.x = (this.minX - that.maxX);
	}
	
	if (this.maxY >= that.minY && this.maxY <= that.maxY) {
	    // y-->+
	    // [this [-->] that] +
	    intersection.y = (this.maxY - that.minY);
	} else if (that.maxY >= this.minY && that.minY <= this.minY) {
	    // y-->+
	    // [that [<--] this] -
	    intersection.y = (this.minY - that.maxY);
	}
	
	return intersection;
    }

    @Override
    public String toString() {
	return "HitBox [minX=" + minX + ", maxX=" + maxX + ", minY=" + minY + ", maxY=" + maxY + ", position="
		+ position + "]";
    }
}
