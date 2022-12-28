package se.sundsvall.digitalmail.integration.skatteverket.interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.WebServiceClientException;
import org.springframework.ws.client.support.interceptor.ClientInterceptorAdapter;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.soap.SoapMessage;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class SoapMessageSizeInterceptor extends ClientInterceptorAdapter {
    
    private static final Logger LOG = LoggerFactory.getLogger(SoapMessageSizeInterceptor.class);
    
    private final long maxSize;
    
    public SoapMessageSizeInterceptor(@Value("${integration.skatteverket.message-max-size:2097152}") long maxSize) {
        this.maxSize = maxSize;
        LOG.info("Max size of SOAP messages is set to {} bytes.", maxSize);
    }
    
    @Override
    public boolean handleRequest(final MessageContext messageContext) throws WebServiceClientException {
        LOG.info("Checking size of SOAP message.");
    
        final SoapMessage soapMessage = (SoapMessage) messageContext.getRequest();
        
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            soapMessage.writeTo(outputStream);
            checkSizeOfMessage(outputStream);
        } catch (IOException e) {
            LOG.warn("Couldn't calculate size of SOAP message, sending it anyway.");
        }
        
        return true;
    }
    
    private void checkSizeOfMessage(ByteArrayOutputStream outputStream) {
        final int length = outputStream.toString(StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8).length;
        
        if(length > maxSize) {
            throw Problem.builder()
                    .withTitle("Message is too big to be sent as a digital mail.")
                    .withStatus(Status.BAD_REQUEST)
                    .withDetail("Size is: " + length + " bytes. Max allowed is: " + maxSize + " bytes.")
                    .build();
        }
    }
}
