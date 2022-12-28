package se.sundsvall.digitalmail.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import se.sundsvall.digitalmail.api.model.DigitalMailRequest;
import se.sundsvall.digitalmail.api.model.DigitalMailResponse;
import se.sundsvall.digitalmail.api.model.validation.DigitalMailRequestValidator;
import se.sundsvall.digitalmail.domain.DigitalMailRequestDto;
import se.sundsvall.digitalmail.service.DigitalMailService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DigitalMailResourceTest {
    
    @Mock
    private DigitalMailService mockDigitalMailService;
    
    @Mock
    private DigitalMailRequestValidator mockValidator;
    
    private DigitalMailResource digitalMailResource;
    
    @BeforeEach
    void setup() {
        digitalMailResource = new DigitalMailResource(mockDigitalMailService, mockValidator);
    }
    
    @Test
    void testSendDigitalMail_shouldReturnNotImplementedForNow() {
        Mockito.doNothing().when(mockValidator).validateRequest(any(DigitalMailRequest.class));
        final ResponseEntity<DigitalMailResponse> response = digitalMailResource.sendDigitalMail(new DigitalMailRequest());
        
        assertThat(response.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
        verify(mockDigitalMailService, times(1)).sendDigitalMail(any(DigitalMailRequestDto.class));
    }
}