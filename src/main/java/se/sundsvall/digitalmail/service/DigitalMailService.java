package se.sundsvall.digitalmail.service;

import org.springframework.stereotype.Service;
import se.gov.minameddelanden.schema.service.v3.DeliverSecure;
import se.gov.minameddelanden.schema.service.v3.DeliverSecureResponse;
import se.sundsvall.digitalmail.api.model.DigitalMailResponse;
import se.sundsvall.digitalmail.domain.DigitalMailRequestDto;
import se.sundsvall.digitalmail.domain.Mailbox;
import se.sundsvall.digitalmail.integration.citizenmapping.CitizenMappingIntegration;
import se.sundsvall.digitalmail.integration.skatteverket.sendmail.DigitalMailIntegration;
import se.sundsvall.digitalmail.integration.skatteverket.sendmail.DigitalMailMapper;

import java.util.List;

@Service
public class DigitalMailService {
    
    private final CitizenMappingIntegration citizenMappingIntegration;
    private final DigitalMailMapper digitalMailMapper;
    private final DigitalMailIntegration digitalMailIntegration;
    private final AvailabilityService availabilityService;
    
    
    public DigitalMailService(CitizenMappingIntegration citizenMappingIntegration, final DigitalMailIntegration digitalMailIntegration, final DigitalMailMapper digitalMailMapper, final AvailabilityService availabilityService) {
        this.citizenMappingIntegration = citizenMappingIntegration;
        this.digitalMailIntegration = digitalMailIntegration;
        this.digitalMailMapper = digitalMailMapper;
        this.availabilityService = availabilityService;
    }
    
    /**
     * Send a digital mail to a recipient
     * @param requestDto containing message and recipient
     * @return Response whether the sending went ok or not.
     */
    public DigitalMailResponse sendDigitalMail(DigitalMailRequestDto requestDto) {
    
        final String personalNumber = citizenMappingIntegration.getCitizenMapping(requestDto.getPartyId());
        
        final List<Mailbox> possibleMailbox = availabilityService.getRecipientMailboxesAndCheckAvailability(List.of(personalNumber));
        
        Mailbox mailbox = possibleMailbox.get(0);            //We will always only have one here if no exception has been thrown, then we wouldn't be here.
        requestDto.setRecipientId(mailbox.recipientId());
        
        final DeliverSecure deliverSecureRequest = digitalMailMapper.createDeliverSecure(requestDto);
    
        //send message, since the serviceAddress may differ we set this as a parameter into the integration.
        DeliverSecureResponse deliverSecureResponse = digitalMailIntegration.sendDigitalMail(deliverSecureRequest, mailbox.serviceAddress());

        return digitalMailMapper.createDigitalMailResponse(deliverSecureResponse, requestDto.getPartyId());
    }
}
