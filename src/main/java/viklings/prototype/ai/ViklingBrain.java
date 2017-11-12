package viklings.prototype.ai;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.joml.Vector3f;

import engine.ai.IntelligentAgent;
import engine.physics.HitBox;
import engine.physics.RigidBody;
import viklings.prototype.Character;
import viklings.prototype.Character.Move;
import viklings.prototype.Communication.CommunicationListener;
import viklings.prototype.Communication.ListeningEvent;

public class ViklingBrain implements IntelligentAgent, CommunicationListener {
    
    private final Vector3f initialPos;
    private final Character body;
    private final RigidBody decisionContext;
    private final HitBox listeningHitBox;
    
    private final int CLOSE_ENOUGH = 5;
    
    private final Queue<ListeningEvent> messages = new LinkedList<>();
    
    public ViklingBrain(Character body, RigidBody decisionContext, HitBox listeningHitBox) {
	this.body = body;
	this.decisionContext = decisionContext;
	this.initialPos = decisionContext.getPosition();
	this.listeningHitBox = listeningHitBox;
    }
    
    private final AtomicBoolean isThinking = new AtomicBoolean(false);
    /**
     * For now the dumb guy just tries to go back to where he started
     * If he hears something, he responds stops, and responds.
     */
    @Override
    public void think() {
	isThinking.set(true);
	boolean stopAndRespond = false;
	ListeningEvent msg = null;
	
	for(;;) {
	    msg = messages.poll();
	    stopAndRespond = true;
	    break;
	}
	messages.clear();
	
	float distance = decisionContext.getPosition().distance(initialPos);
	
	boolean isThereX = false;
	boolean isThereY = false;
	
	if (!stopAndRespond && !(distance < 1 || decisionContext.isSliding())) {
	    if (decisionContext.getPosition().x < initialPos.x - CLOSE_ENOUGH) {
		body.move(Move.RIGHT, true);
	    } else if (decisionContext.getPosition().x > initialPos.x + CLOSE_ENOUGH) {
		body.move(Move.LEFT, true);
	    } else {
		isThereX = true;
	    }
	    
	    if (decisionContext.getPosition().y < initialPos.y - CLOSE_ENOUGH) {
		body.move(Move.DOWN, true);
	    } else if (decisionContext.getPosition().y > initialPos.y + CLOSE_ENOUGH) {
		body.move(Move.UP, true);
	    } else {
		isThereY = true;
	    }
	}
	if ((isThereX && isThereY) || stopAndRespond) {
	    body.stop();
	    if (msg != null) {
		body.talk();
	    }
	}
	isThinking.set(false);
    }
    
    @Override
    public HitBox getListeningRange() {
	return listeningHitBox;
    }
    
    @Override
    public void hear(ListeningEvent message) {
	if (message.getSpeaker() != body) { //Ignore your own voice
	    messages.clear();
	    messages.add(message);
	}
    }
    
    @Override
    public boolean isThinking() {
	return isThinking.get();
    }
}
