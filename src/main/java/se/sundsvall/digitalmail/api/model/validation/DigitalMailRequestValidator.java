package se.sundsvall.digitalmail.api.model.validation;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;
import se.sundsvall.digitalmail.api.model.BodyInformation;
import se.sundsvall.digitalmail.api.model.DigitalMailRequest;
import se.sundsvall.digitalmail.integration.w3c.W3CValidatorClient;
import se.sundsvall.digitalmail.integration.w3c.W3CValidatorDto;

import javax.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

@Component
public class DigitalMailRequestValidator {
    
    private final W3CValidatorClient w3CValidatorClient;
    
    public DigitalMailRequestValidator(W3CValidatorClient w3CValidatorClient) {
        this.w3CValidatorClient = w3CValidatorClient;
    }
    
    public void validateRequest(final DigitalMailRequest request) {
        validatePartyId(request.getPartyId());
        validateBodyInformation(request.getBodyInformation());
        validateHtml(request);
    }
    
    /**
     * If we have a bodyinformation object it needs to be populated, otherwise we will make an empty body later on.
     * @param bodyInformation
     */
    private void validateBodyInformation(final @Valid BodyInformation bodyInformation) {
        if(bodyInformation != null) {
            if(StringUtils.isBlank(bodyInformation.getBody()) || StringUtils.isBlank(bodyInformation.getContentType())) {
                throw Problem.builder()
                        .withTitle("contentType and body must not be empty if bodyinformation is provided.")
                        .withStatus(Status.BAD_REQUEST)
                        .build();
            }
        }
    }
    
    private void validateHtml(final DigitalMailRequest request) {
        //Check if the content is html and validate it
        if(request.getBodyInformation() != null && MediaType.TEXT_HTML_VALUE.equals(request.getBodyInformation().getContentType())) {
            final String bodyAsBase64 = request.getBodyInformation().getBody();
            String html = new String(Base64.getDecoder().decode(bodyAsBase64.getBytes(StandardCharsets.UTF_8)));
            final W3CValidatorDto validatorResponse = w3CValidatorClient.validate(html);
            checkValidationResult(validatorResponse);
        }
    }
    
    private void checkValidationResult(W3CValidatorDto validatorResponse) {
        
        //Basically just check if we have any messages, if we do, the html is not validated.
        if(!validatorResponse.getMessages().isEmpty()) {
            throw Problem.builder()
                    .withTitle("Provided body html does not validate againts w3c.")
                    .withStatus(Status.BAD_REQUEST)
                    .withDetail("Use https://validator.w3.org/ to make sure your html validates.")
                    .build();
        }
    }
    
    private void validatePartyId(String partyId) {
        try {
            //We only check that the parsing is ok, not the return value.
            UUID.fromString(String.valueOf(partyId));
        } catch (Exception e) {
            throw Problem.builder()
                    .withTitle("PartyId has wrong formatting")
                    .withStatus(Status.BAD_REQUEST)
                    .withDetail("Faulty partyId: " + partyId)
                    .build();
        }
    }
}
