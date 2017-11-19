package engine.ai;

import engine.EngineComponent;

import java.util.ArrayList;
import java.util.List;

public class IntelligenceEngine implements EngineComponent {

    private final float updateInterval;
    private final List<IntelligentAgent> agents;

    public IntelligenceEngine(float updateInterval) {
        this.updateInterval = updateInterval;
        agents = new ArrayList<>();
    }

    @Override
    public void update(float interval) throws Exception {
        for (IntelligentAgent agent : agents) {
            if (!agent.isThinking()) {
                agent.think();
            }
        }
    }

    public void addAgent(IntelligentAgent agent) {
        agents.add(agent);
    }

    public void removeAgent(IntelligentAgent agent) {
        agents.remove(agent);
    }

    @Override
    public boolean shouldUpdate(float interval) {
        return interval >= updateInterval;
    }
}
