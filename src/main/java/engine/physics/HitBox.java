package engine.physics;

import org.joml.Vector3f;

public class HitBox {
    
    private float minX, maxX, minY, maxY;
    
    private final Vector3f position;
    /**
     * 
     * @param position position of top left corner of hit box
     * @param height height of hit box
     * @param width width of hit box
     */
    public HitBox(Vector3f position, float height, float width) {
	this.position = position;
	
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

    public boolean isCollision(HitBox that) {
	
	return this.maxX >= that.minX &&
		this.minX <= that.maxX &&
		this.maxY >= that.minY &&
		this.minY <= that.maxY;              

    }
}
