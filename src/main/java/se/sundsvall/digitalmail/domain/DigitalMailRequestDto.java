package se.sundsvall.digitalmail.domain;

import lombok.Getter;
import lombok.Setter;
import se.sundsvall.digitalmail.api.model.DigitalMailRequest;

@Getter
@Setter
public class DigitalMailRequestDto extends DigitalMailRequest {
    
    private String recipientId; //Recipient id from e.g. kivra.
    
    public DigitalMailRequestDto(DigitalMailRequest request) {
        super(request.getPartyId(), request.getMunicipalityId(), request.getHeaderSubject(), request.getSupportInfo(), request.getAttachments(), request.getBodyInformation());
    }
}
