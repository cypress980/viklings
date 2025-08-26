package engine.physics;

import org.joml.Vector3f;
import org.junit.Assert;
import org.junit.Test;

public class HitBoxTest {

    private HitBox hitBox;
    private HitBox otherHitBox;
    
    @Test
    public void hitBoxDetectsIntersectionsWithDifferentOrientations() {
	hitBox = givenHitBoxWithRectangle(0f, 1f, 0f, 1f);
	otherHitBox = givenHitBoxWithRectangle(0.9f, 1.9f, 0f, 1f);
	
	Assert.assertTrue(hitBox.isCollision(otherHitBox));
	Assert.assertTrue(otherHitBox.isCollision(hitBox));
	
	hitBox = givenHitBoxWithRectangle(0f, 1f, 0f, 1f);
	otherHitBox = givenHitBoxWithRectangle(0f, 1f, 0.9f, 1.9f);
	
	Assert.assertTrue(hitBox.isCollision(otherHitBox));
	Assert.assertTrue(otherHitBox.isCollision(hitBox));
    }
    
    @Test
    public void hitBoxReturnsOverlapX() {
	hitBox = givenHitBoxWithRectangle(0f, 1f, 0f, 1f);
	otherHitBox = givenHitBoxWithRectangle(0.9f, 1.9f, 0f, 1f);
	
	Vector3f actualIntersection = hitBox.getIntersection(otherHitBox);
	Vector3f expectedIntersection = new Vector3f(0.1f, 1f, 0f);
	
	Assert.assertEquals(expectedIntersection.x, actualIntersection.x, 0.000001f);
	Assert.assertEquals(expectedIntersection.y, actualIntersection.y, 0.000001f);
	
	//Try testing the other way - we should get a negative x this time
	actualIntersection = otherHitBox.getIntersection(hitBox);
	expectedIntersection = new Vector3f(-0.1f, 1f, 0f);
	
	Assert.assertEquals(expectedIntersection.x, actualIntersection.x, 0.000001f);
	Assert.assertEquals(expectedIntersection.y, actualIntersection.y, 0.000001f);
    }
    
    
    @Test
    public void hitBoxReturnsOverlapY() {
	hitBox = givenHitBoxWithRectangle(0f, 1f, 0f, 1f);
	otherHitBox = givenHitBoxWithRectangle(0f, 1f, 0.9f, 1.9f);
	
	Vector3f actualIntersection = hitBox.getIntersection(otherHitBox);
	Vector3f expectedIntersection = new Vector3f(1f, 0.1f, 0f);
	
	Assert.assertEquals(expectedIntersection.x, actualIntersection.x, 0.000001f);
	Assert.assertEquals(expectedIntersection.y, actualIntersection.y, 0.000001f);
	
	//Try testing the other way - we should get a negative x this time
	actualIntersection = otherHitBox.getIntersection(hitBox);
	expectedIntersection = new Vector3f(1f, -0.1f, 0f);
	
	Assert.assertEquals(expectedIntersection.x, actualIntersection.x, 0.000001f);
	Assert.assertEquals(expectedIntersection.y, actualIntersection.y, 0.000001f);
    }

    private HitBox givenHitBoxWithRectangle(float minX, float maxX, float minY, float maxY) {
	float h = maxY - minY;
	float w = maxX - minX;
	Vector3f position = new Vector3f(minX, minY, 0);
	
	return new HitBox(position, h, w);
    }
    
}
