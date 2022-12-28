package se.sundsvall.digitalmail.integration.skatteverket.sendmail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.zalando.problem.Problem;
import org.zalando.problem.ThrowableProblem;
import se.gov.minameddelanden.schema.service.v3.DeliverSecure;
import se.gov.minameddelanden.schema.service.v3.DeliverSecureResponse;

import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;

@Component
public class DigitalMailIntegration extends SendMailHealthIndicator {
    
    private static final Logger LOG = LoggerFactory.getLogger(DigitalMailIntegration.class);
    public static final String INTEGRATION_NAME = "DigitalMail";
    
    private final WebServiceTemplate distributeTemplate;
    
    @Autowired
    public DigitalMailIntegration(@Qualifier("skatteverket-sendmail-webservice-template") WebServiceTemplate distributeTemplate) {
        this.distributeTemplate = distributeTemplate;
    }
    
    /**
     * Send a digital mail
     * @param deliverSecureRequest
     * @param serviceAddress
     * @return
     */
    public DeliverSecureResponse sendDigitalMail(DeliverSecure deliverSecureRequest, String serviceAddress) {
        LOG.debug("Trying to send secure digital mail.");
    
        DeliverSecureResponse response;
        try {
            response = (DeliverSecureResponse) distributeTemplate.marshalSendAndReceive(serviceAddress, deliverSecureRequest);
        } catch (Exception e) {
            //Might come from interceptor
            if(e instanceof ThrowableProblem) {
                throw e;
            }
            
            setOutOfService(e);
            
            ThrowableProblem cause = getProblemCause(e);
    
            throw Problem.builder()
                    .with("integration", INTEGRATION_NAME)
                    .withCause(cause)
                    .withDetail(e.getMessage())
                    .withStatus(INTERNAL_SERVER_ERROR)
                    .withTitle("Couldn't send secure digital mail")
                    .build();
        }
        
        return response;
    }
    
    //If we get an error parsing XML we can't use ".getCause()", really special case..
    ThrowableProblem getProblemCause(final Exception e) {
        ThrowableProblem cause;
        try {
            cause = ((ThrowableProblem) e.getCause());
        } catch (Exception ex) {
            LOG.error("Couldn't get cause.", e);
            cause = Problem.builder()
                    .withDetail("Couldn't get Cause")
                    .build();
        }
        return cause;
    }
}
