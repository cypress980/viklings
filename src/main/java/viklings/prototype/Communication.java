package viklings.prototype;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import engine.physics.HitBox;

public class Communication {
    public static enum Message {
	GREET,
	ASK,
	INSULT;
    }
    
    private final Set<CommunicationListener> listeners = new HashSet<>();
    
    public void addListener(CommunicationListener listener) {
	listeners.add(listener);
    }
    
    public void removeListener(CommunicationListener listener) {
	listeners.remove(listener);
    }
    
    public void say(Character speaker, Message message, HitBox range) {
	Set<CommunicationListener> listeners = findListeners(range);
	ListeningEvent event = new ListeningEvent(speaker, message);
	listeners.forEach((l) -> {
	    l.hear(event);
	});
    }

    private Set<CommunicationListener> findListeners(HitBox range) {
	return listeners.parallelStream()
		.filter((l) -> {
		    return l.getListeningRange().isCollision(range);
		    })
		.collect(Collectors.toSet());
    }
    
    public static class ListeningEvent {
	private final Character speaker;
	private final Message message;
	
	public ListeningEvent(Character speaker, Message message) {
	    this.speaker = speaker;
	    this.message = message;
	}

	public Character getSpeaker() {
	    return speaker;
	}

	public Message getMessage() {
	    return message;
	}
    }

    public static interface CommunicationListener {
	HitBox getListeningRange();
	void hear(ListeningEvent message);
    }
}
