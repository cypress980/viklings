package viklings.prototype.ai;

import org.joml.Vector3f;

import engine.ai.IntelligentAgent;
import engine.physics.RigidBody;
import viklings.prototype.Character;
import viklings.prototype.Character.Move;

public class ViklingBrain implements IntelligentAgent {
    
    private final Vector3f initialPos;
    private final Character body;
    private final RigidBody decisionContext;
    
    public ViklingBrain(Character body, RigidBody decisionContext) {
	this.body = body;
	this.decisionContext = decisionContext;
	this.initialPos = decisionContext.getPosition();
    }
    
    /**
     * For now the dumb guy just tries to go back to where he started
     */
    @Override
    public void think() {
	float distance = decisionContext.getPosition().distance(initialPos);
	if (distance < 1 || decisionContext.isSliding()) {
	    return;
	}
	
	if (decisionContext.getPosition().x < initialPos.x) {
	    body.move(Move.RIGHT, true);
	} else if (decisionContext.getPosition().x > initialPos.x) {
	    body.move(Move.LEFT, true);
	} else {
	    body.move(Move.RIGHT, false);
	    body.move(Move.LEFT, false);
	}
	
	if (decisionContext.getPosition().y < initialPos.y) {
	    body.move(Move.DOWN, true);
	} else if (decisionContext.getPosition().y > initialPos.y) {
	    body.move(Move.UP, true);
	} else {
	    body.move(Move.UP, false);
	    body.move(Move.DOWN, false);
	}
    }

}
