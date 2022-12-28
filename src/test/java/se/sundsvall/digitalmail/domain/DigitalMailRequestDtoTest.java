package se.sundsvall.digitalmail.domain;

import org.junit.jupiter.api.Test;
import se.sundsvall.digitalmail.api.model.BodyInformation;
import se.sundsvall.digitalmail.api.model.DigitalMailRequest;
import se.sundsvall.digitalmail.api.model.SupportInfo;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class DigitalMailRequestDtoTest {
    
    @Test
    void testConstructor() {
        DigitalMailRequest request = DigitalMailRequest.builder()
                .withPartyId("partyId")
                .withHeaderSubject("Subject")
                .withSupportInfo(new SupportInfo())
                .withAttachments(new ArrayList<>())
                .withBodyInformation(BodyInformation.builder().build())
                .build();
        
        DigitalMailRequestDto dto = new DigitalMailRequestDto(request);
        
        assertThat(dto.getPartyId()).isEqualTo("partyId");
        assertThat(dto.getHeaderSubject()).isEqualTo("Subject");
        assertThat(dto.getSupportInfo()).isNotNull();
        assertThat(dto.getAttachments()).isNotNull();
        assertThat(dto.getBodyInformation()).isNotNull();
    }
}