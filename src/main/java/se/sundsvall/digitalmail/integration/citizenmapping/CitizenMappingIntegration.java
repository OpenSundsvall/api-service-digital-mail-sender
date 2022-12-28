package se.sundsvall.digitalmail.integration.citizenmapping;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import se.sundsvall.digitalmail.integration.DigitalMailHealthIndicator;

@Component
public class CitizenMappingIntegration extends DigitalMailHealthIndicator {
    
    public static final String INTEGRATION_NAME = "citizenmapping";
    
    private final CitizenMappingClient citizenMappingClient;
    
    @Autowired
    public CitizenMappingIntegration(CitizenMappingClient citizenMappingClient) {
        super(INTEGRATION_NAME);
        this.citizenMappingClient = citizenMappingClient;
    }
    
    public String getCitizenMapping(String partyId) {
        return citizenMappingClient.getCitizenMapping(partyId);
    }
}
