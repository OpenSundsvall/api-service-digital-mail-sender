package se.sundsvall.digitalmail.integration.citizenmapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CitizenMappingIntegrationTest {
    
    private CitizenMappingIntegration integration;
    
    @Mock
    private CitizenMappingClient mockCitizenMappingClient;
    
    @BeforeEach
    void setup() {
        integration = new CitizenMappingIntegration(mockCitizenMappingClient);
    }
    
    @Test
    void testGetCitizenMapping_shouildReturnPersonalNumber() {
        when(mockCitizenMappingClient.getCitizenMapping(eq("anyId"))).thenReturn("legalId");
    
        final String personalNumber = integration.getCitizenMapping("anyId");
        assertThat(personalNumber).isEqualTo("legalId");
    }
}