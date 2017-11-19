package engine.ai;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class IntelligenceEngineTest {

    IntelligenceEngine aiEngine;

    @Mock
    IntelligentAgent agent;

    @Before
    public void setupTest() {
        MockitoAnnotations.initMocks(this);
        Mockito.when(agent.isThinking()).thenReturn(false);
    }

    @Test
    public void testIntelligenceEngine() throws Exception {
        givenAiEngineWithUpdateInterval(0.1F);
        whenAiEngineUpdatesAfterInterval(0.11F);
        thenAgentIsUpdatedWithDuration();
    }

    private void givenAiEngineWithUpdateInterval(float minimumUpdateInterval) {
        aiEngine = new IntelligenceEngine(minimumUpdateInterval);
        aiEngine.addAgent(agent);
    }

    private void whenAiEngineUpdatesAfterInterval(float updateInterval) throws Exception {
        aiEngine.update(updateInterval);
    }

    private void thenAgentIsUpdatedWithDuration() {
        Mockito.verify(agent).think();
    }
}