package se.sundsvall.digitalmail.integration.skatteverket.reachable;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.zalando.problem.Problem;
import se.gov.minameddelanden.schema.recipient.v3.IsReachable;
import se.gov.minameddelanden.schema.recipient.v3.IsReachableResponse;

import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;

@Component
public class ReachableIntegration extends ReachableHealthIndicator {
    
    public static final String INTEGRATION_NAME = "Skatteverket Reachable";
    
    private final WebServiceTemplate isReachableTemplate;
    
    public ReachableIntegration(@Qualifier("skatteverket-isreachable-webservice-template") WebServiceTemplate isReachableTemplate) {
        this.isReachableTemplate = isReachableTemplate;
    }
    
    /**
     * Fetches a mailbox and if a mailbox is reachable.
     * @param isReachableRequest
     * @return
     */
    public IsReachableResponse callSkatteverketIsReacheble(IsReachable isReachableRequest) {
    
        IsReachableResponse response;
        try {
            response = (IsReachableResponse) isReachableTemplate.marshalSendAndReceive(isReachableRequest);
        } catch (Exception e) {
            setOutOfService(e);
            
            throw Problem.builder()
                    .withTitle("Error while getting digital mailbox from skatteverket")
                    .withStatus(INTERNAL_SERVER_ERROR)
                    .withDetail(e.getMessage())
                    .build();
        }
        
        return response;
    }
}
