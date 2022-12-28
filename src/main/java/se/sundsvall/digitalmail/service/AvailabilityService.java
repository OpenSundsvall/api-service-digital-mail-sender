package se.sundsvall.digitalmail.service;

import org.springframework.stereotype.Service;
import org.zalando.problem.Problem;
import se.sundsvall.digitalmail.domain.Mailbox;
import se.sundsvall.digitalmail.integration.skatteverket.RecipientIntegrationMapper;
import se.sundsvall.digitalmail.integration.skatteverket.reachable.ReachableIntegration;

import java.util.List;
import java.util.Optional;

import static org.zalando.problem.Status.NOT_FOUND;

@Service
public class AvailabilityService {
    
    private final RecipientIntegrationMapper skatteverketMapper;
    private final ReachableIntegration reachableIntegration;
    
    public AvailabilityService(final RecipientIntegrationMapper skatteverketMapper, final ReachableIntegration reachableIntegration) {
        this.skatteverketMapper = skatteverketMapper;
        this.reachableIntegration = reachableIntegration;
    }
    
    /**
     * Fetch a list of possible mailboxes.
     * This is a list in case we need to expand upon this in the future.
     * @param personalNumbers containing all partyIds we should fetch mailboxes for
     * @return
     */
    public List<Mailbox> getRecipientMailboxesAndCheckAvailability(List<String> personalNumbers) {
        //Fetch ssn by partyId from citizenmapping
        //TODO, first check in citizenmapping, if no match, check "legal-entity-mapping when it's available.
        
        //Call skatteverket to see which mailbox (if any) the person has
        var isReachableRequest = skatteverketMapper.createIsReachableRequest(personalNumbers);
        var isReachableResponse = reachableIntegration.callSkatteverketIsReacheble(isReachableRequest);
        
        //Get the recipient from the response, don't know if it may differ..
        List<Optional<Mailbox>> possibleMailboxes = skatteverketMapper.getMailboxSettings(isReachableResponse);
        
        //Get only existing mailboxes.
        final List<Mailbox> mailboxes = possibleMailboxes.stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
        
        //Check if we didn't get a single mailbox
        if(mailboxes.isEmpty()) {
            throw Problem.builder()
                    .withTitle("Couldn't send digital mail")
                    .withDetail("No mailbox could be found for any of the given partyIds or the recipients doesn't allow the sender.")
                    .withStatus(NOT_FOUND)
                    .build();
        }
        
        return mailboxes;
    }
}
