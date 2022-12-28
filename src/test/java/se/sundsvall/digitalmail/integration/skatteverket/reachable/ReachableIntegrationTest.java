package se.sundsvall.digitalmail.integration.skatteverket.reachable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.zalando.problem.ThrowableProblem;
import se.gov.minameddelanden.schema.recipient.AccountStatus;
import se.gov.minameddelanden.schema.recipient.ReachabilityStatus;
import se.gov.minameddelanden.schema.recipient.v3.IsReachable;
import se.gov.minameddelanden.schema.recipient.v3.IsReachableResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReachableIntegrationTest {
    
    @Mock
    private WebServiceTemplate mockReachableTemplate;
    
    private ReachableIntegration reachableIntegration;
    
    @BeforeEach
    void setup() {
        reachableIntegration = new ReachableIntegration(mockReachableTemplate);
    }
    
    //Not really testing much but behavior
    @Test
    void testCallIsReachable_whenOk_shouldReturnResponse() {
        var response = new IsReachableResponse()
                .withReturns(List.of(new ReachabilityStatus()
                        .withAccountStatus(new AccountStatus()
                                .withRecipientId("recipientId"))));
        when(mockReachableTemplate.marshalSendAndReceive(any(IsReachable.class))).thenReturn(response);
        
        final var isReachableResponse = reachableIntegration.callSkatteverketIsReacheble(new IsReachable());
        assertThat(isReachableResponse).isNotNull();
    }
    
    @Test
    void testCallIsRegistered_whenException_shouldThrowProblem() {
        when(mockReachableTemplate.marshalSendAndReceive(any(IsReachable.class))).thenThrow(new RuntimeException());
        assertThatExceptionOfType(ThrowableProblem.class).isThrownBy(() -> reachableIntegration.callSkatteverketIsReacheble(new IsReachable()))
                .withMessage("Error while getting digital mailbox from skatteverket");
    }
}