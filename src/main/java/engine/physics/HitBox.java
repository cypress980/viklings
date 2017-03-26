package engine.physics;

import org.joml.Vector3f;

public class HitBox {
    
    private float delta = 0.0f;
    
    private float minX, maxX, minY, maxY;
    
    /**
     * 
     * @param position position of top left corner of hit box
     * @param height height of hit box
     * @param width width of hit box
     */
    public HitBox(Vector3f position, float height, float width) {
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
	minX += displacement.x;
	minY += displacement.y;
	maxX += displacement.x;
	maxY += displacement.y;
    }

    public boolean isCollision(HitBox that) {
	
	return this.maxX >= that.minX &&
		this.minX <= that.maxX &&
		this.maxY >= that.minY &&
		this.minY <= that.maxY;              

    }
}
