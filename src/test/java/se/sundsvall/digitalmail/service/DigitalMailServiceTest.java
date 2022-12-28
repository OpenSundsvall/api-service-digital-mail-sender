package se.sundsvall.digitalmail.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.Problem;
import org.zalando.problem.ThrowableProblem;
import se.gov.minameddelanden.schema.service.v3.DeliverSecure;
import se.gov.minameddelanden.schema.service.v3.DeliverSecureResponse;
import se.sundsvall.digitalmail.api.model.DigitalMailRequest;
import se.sundsvall.digitalmail.api.model.DigitalMailResponse;
import se.sundsvall.digitalmail.domain.DigitalMailRequestDto;
import se.sundsvall.digitalmail.domain.Mailbox;
import se.sundsvall.digitalmail.integration.citizenmapping.CitizenMappingIntegration;
import se.sundsvall.digitalmail.integration.skatteverket.sendmail.DigitalMailIntegration;
import se.sundsvall.digitalmail.integration.skatteverket.sendmail.DigitalMailMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;

@ExtendWith(MockitoExtension.class)
class DigitalMailServiceTest {
    
    @Mock
    private CitizenMappingIntegration mockCitizenMappingIntegration;
    
    @Mock
    private DigitalMailMapper mockDigitalMailMapper;
    
    @Mock
    private DigitalMailIntegration mockDigitalMailIntegration;
    
    @Mock
    private AvailabilityService mockAvailabilityService;
    
    private DigitalMailService service;
    
    @BeforeEach
    void setup() {
        service = new DigitalMailService(mockCitizenMappingIntegration, mockDigitalMailIntegration, mockDigitalMailMapper, mockAvailabilityService);
    }
    
    @Test
    void testSendDigitalMail_shouldReturnResponse() {
        final DigitalMailRequestDto request = generateDigitalMailRequestDto();
        Map<String, String> pNumberMap = new HashMap<>(){{put("pNumber", "partyId");}};
        Mailbox mailbox = new Mailbox("recipientId", "serviceAddress", "kivra");
        
        when(mockCitizenMappingIntegration.getCitizenMapping(anyString())).thenReturn("personalNumber");
        when(mockAvailabilityService.getRecipientMailboxesAndCheckAvailability(anyList())).thenReturn(List.of(mailbox));
        when(mockDigitalMailMapper.createDeliverSecure(any(DigitalMailRequestDto.class))).thenReturn(new DeliverSecure());
        when(mockDigitalMailIntegration.sendDigitalMail(any(DeliverSecure.class), eq("serviceAddress"))).thenReturn(new DeliverSecureResponse());
        when(mockDigitalMailMapper.createDigitalMailResponse(any(DeliverSecureResponse.class), eq("partyId"))).thenReturn(new DigitalMailResponse());
    
        final DigitalMailResponse digitalMailResponse = service.sendDigitalMail(new DigitalMailRequestDto(request));
        
        assertThat(digitalMailResponse).isNotNull();
        verify(mockCitizenMappingIntegration, times(1)).getCitizenMapping(anyString());
        verify(mockAvailabilityService, times(1)).getRecipientMailboxesAndCheckAvailability(anyList());
        verify(mockDigitalMailMapper, times(1)).createDeliverSecure(any(DigitalMailRequestDto.class));
        verify(mockDigitalMailIntegration, times(1)).sendDigitalMail(any(DeliverSecure.class), eq("serviceAddress"));
        verify(mockDigitalMailMapper, times(1)).createDigitalMailResponse(any(DeliverSecureResponse.class), eq("partyId"));
    }
    
    //Same thing will happen if any integration throws an exception so will only test one.
    @Test
    void testSendDigitalMail_citizenMappingThrowsException_shouildThrowProblem() {
        final DigitalMailRequestDto request = generateDigitalMailRequestDto();
        when(mockCitizenMappingIntegration.getCitizenMapping(anyString())).thenThrow(Problem.builder().withStatus(INTERNAL_SERVER_ERROR).build());
    
        assertThatExceptionOfType(ThrowableProblem.class).isThrownBy(() -> service.sendDigitalMail(request));
        
        verify(mockCitizenMappingIntegration, times(1)).getCitizenMapping(anyString());
        verify(mockAvailabilityService, times(0)).getRecipientMailboxesAndCheckAvailability(anyList());
        verify(mockDigitalMailMapper, times(0)).createDeliverSecure(any(DigitalMailRequestDto.class));
        verify(mockDigitalMailIntegration, times(0)).sendDigitalMail(any(DeliverSecure.class), eq("serviceAddress"));
        verify(mockDigitalMailMapper, times(0)).createDigitalMailResponse(any(DeliverSecureResponse.class), eq("partyId"));
        
    }
    
    private DigitalMailRequestDto generateDigitalMailRequestDto() {
        return new DigitalMailRequestDto(DigitalMailRequest.builder()
                .withPartyId("partyId")
                .build());
    }
}