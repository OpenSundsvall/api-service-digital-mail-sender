package se.sundsvall.digitalmail.integration.skatteverket.sendmail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.zalando.problem.ThrowableProblem;
import se.gov.minameddelanden.schema.service.v3.DeliverSecure;
import se.gov.minameddelanden.schema.service.v3.DeliverSecureResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecureMailIntegrationTest {
    
    @Mock
    private WebServiceTemplate mockWebServiceTemplate;
    
    private DigitalMailIntegration mailIntegration;
    
    @BeforeEach
    public void setup() {
        mailIntegration = new DigitalMailIntegration(mockWebServiceTemplate);
    }
    
    @Test
    void testSuccessfulSentMail_shouldReturnDeliveryResult() {
        when(mockWebServiceTemplate.marshalSendAndReceive(eq("http://nowhere.com"), any(DeliverSecure.class))).thenReturn(new DeliverSecureResponse());
    
        final DeliverSecureResponse deliverSecureResponse = mailIntegration.sendDigitalMail(new DeliverSecure(), "http://nowhere.com");
        assertThat(deliverSecureResponse).isNotNull();
        
        verify(mockWebServiceTemplate, times(1)).marshalSendAndReceive(eq("http://nowhere.com"), any(DeliverSecure.class));
    }
    
    @Test
    void testExceptionFromIntegration_shouldThrowProblem() {
        when(mockWebServiceTemplate.marshalSendAndReceive(eq("http://nowhere.com"), any(DeliverSecure.class))).thenThrow(new RuntimeException("error-message"));
    
        assertThatExceptionOfType(ThrowableProblem.class).isThrownBy(() -> mailIntegration.sendDigitalMail(new DeliverSecure(), "http://nowhere.com"))
                .withMessage("Couldn't send secure digital mail: error-message");
        
    }
    
    @Test
    void testGetProblemCause_fakingXmlParsingError_shouldReturnProblem() {
        final ThrowableProblem problemCause = mailIntegration.getProblemCause(null); //Couldn't find a better way to produce similar error...
        assertThat(problemCause.getMessage()).isEqualTo("Couldn't get Cause");
    }
    
}