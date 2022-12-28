package se.sundsvall.digitalmail.api.model.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.zalando.problem.ThrowableProblem;
import se.sundsvall.digitalmail.TestObjectFactory;
import se.sundsvall.digitalmail.domain.DigitalMailRequestDto;
import se.sundsvall.digitalmail.integration.w3c.W3CValidatorClient;
import se.sundsvall.digitalmail.integration.w3c.W3CValidatorDto;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DigitalMailRequestValidatorTest {
    
    @Mock
    private W3CValidatorClient w3CValidatorClient;
    
    DigitalMailRequestValidator validator;
    
    @BeforeEach
    public void setup() {
        validator = new DigitalMailRequestValidator(w3CValidatorClient);
    }
    
    @Test
    void testValidateHtml() {
        when(w3CValidatorClient.validate(anyString())).thenReturn(new W3CValidatorDto());
        final DigitalMailRequestDto digitalMailRequestDto = generateDtoWithHtmlContent(VALID_HTML);
        digitalMailRequestDto.setPartyId("6a5c3d04-412d-11ec-973a-0242ac130003");
        
        validator.validateRequest(digitalMailRequestDto);
    }
    
    @Test
    void testInvalidHtml_shouldThrowException() {
        //Create a dto with a message in it, since that's what triggers a fault
        W3CValidatorDto w3cDto = new W3CValidatorDto();
        w3cDto.setMessages(List.of(new W3CValidatorDto.Message()));
        when(w3CValidatorClient.validate(anyString())).thenReturn(w3cDto);
        
        final DigitalMailRequestDto digitalMailRequestDto = generateDtoWithHtmlContent(NON_VALID_HTML);
        digitalMailRequestDto.setPartyId("6a5c3d04-412d-11ec-973a-0242ac130003");
        
        assertThatExceptionOfType(ThrowableProblem.class).isThrownBy(() -> validator.validateRequest(digitalMailRequestDto))
                .withMessage("Provided body html does not validate againts w3c.: Use https://validator.w3.org/ to make sure your html validates.");
    }
    
    @Test
    void testInvalidPartyId_shouldThrowException() {
        final DigitalMailRequestDto digitalMailRequestDto = new DigitalMailRequestDto(TestObjectFactory.generateDigitalMailRequestDto());
        digitalMailRequestDto.setPartyId("donald duck");
    
        assertThatExceptionOfType(ThrowableProblem.class).isThrownBy(() -> validator.validateRequest(digitalMailRequestDto))
                .withMessage("PartyId has wrong formatting: Faulty partyId: donald duck");
    }
    
    private DigitalMailRequestDto generateDtoWithHtmlContent(String html) {
    
        final String encodedHtml = new String(Base64.getEncoder().encode(html.getBytes(StandardCharsets.UTF_8)));
        final DigitalMailRequestDto digitalMailRequestDto = TestObjectFactory.generateDigitalMailRequestDto();
        digitalMailRequestDto.getBodyInformation().setContentType(MediaType.TEXT_HTML_VALUE);
        digitalMailRequestDto.getBodyInformation().setBody(encodedHtml);
        
        return digitalMailRequestDto;
    }
    
    //Not really needed but since the validator actually decodes the string, why not.
    private static final String VALID_HTML ="<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\"><title>Test</title></head><body><p>Hai</p></body></html>";
    private static final String NON_VALID_HTML ="<!DOCTYPE html><html><head><meta charset=\"utf-8\"><title>Test</title></head><body><p>Hai</p></body></html>";
}