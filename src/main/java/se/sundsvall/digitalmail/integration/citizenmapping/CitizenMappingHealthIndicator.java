package se.sundsvall.digitalmail.integration.citizenmapping;

import org.springframework.stereotype.Component;
import se.sundsvall.digitalmail.integration.DigitalMailHealthIndicator;

@Component
public class CitizenMappingHealthIndicator extends DigitalMailHealthIndicator {
    
    public CitizenMappingHealthIndicator() {
        super(CitizenMappingIntegration.INTEGRATION_NAME);
    }
}
