package viklings.prototype.ai;

import org.joml.Vector3f;

import engine.ai.IntelligentAgent;
import engine.physics.RigidBody;
import viklings.prototype.ViklingCharacter;
import viklings.prototype.ViklingCharacter.Move;

public class ViklingBrain implements IntelligentAgent {
    
    private final Vector3f initialPos;
    private final ViklingCharacter body;
    private final RigidBody decisionContext;
    
    public ViklingBrain(ViklingCharacter body, RigidBody decisionContext) {
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
	    body.move(Move.STAND);
	    return;
	}
	
	if (decisionContext.getPosition().x < initialPos.x) {
	    body.move(Move.RIGHT);
	} else if (decisionContext.getPosition().x > initialPos.x) {
	    body.move(Move.LEFT);
	}
	
	if (decisionContext.getPosition().y < initialPos.y) {
	    body.move(Move.DOWN);
	} else if (decisionContext.getPosition().y > initialPos.y) {
	    body.move(Move.UP);
	}
    }

}
