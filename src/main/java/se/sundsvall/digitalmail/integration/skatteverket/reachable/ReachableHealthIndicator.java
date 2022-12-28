package se.sundsvall.digitalmail.integration.skatteverket.reachable;

import se.sundsvall.digitalmail.integration.DigitalMailHealthIndicator;

public class ReachableHealthIndicator extends DigitalMailHealthIndicator {
    
    public ReachableHealthIndicator() {
        super(ReachableIntegration.INTEGRATION_NAME);
    }
}
