package se.sundsvall.digitalmail.integration.skatteverket;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import se.gov.minameddelanden.schema.recipient.ReachabilityStatus;
import se.gov.minameddelanden.schema.recipient.v3.IsReachable;
import se.gov.minameddelanden.schema.recipient.v3.IsReachableResponse;
import se.gov.minameddelanden.schema.recipient.v3.ObjectFactory;
import se.sundsvall.digitalmail.domain.Mailbox;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class RecipientIntegrationMapper {
    
    public static final String SENDER_ORG_NR = "162120002411"; //We will always send as sundsvalls kommun.
    private final List<String> supportedSuppliers;
    
    public RecipientIntegrationMapper(SkatteverketProperties properties) {
        this.supportedSuppliers = properties.getSupportedSuppliers();
    }
    
    /**
     *
     * @param personalNumbers map of personalnumbers with corresponding partyIds
     * @return
     */
    public IsReachable createIsReachableRequest(List<String> personalNumbers) {
        return new ObjectFactory().createIsReachable()
                .withRecipientIds(personalNumbers)
                .withSenderOrgNr(SENDER_ORG_NR);
    }
    public List<Optional<Mailbox>> getMailboxSettings(IsReachableResponse response) {
    
        List<Optional<Mailbox>> personalNumberMailboxList = new ArrayList<>();
        
        if(response.getReturns() != null && !response.getReturns().isEmpty()) {
            //There will only be one since we only ever ask for one, get it (for now at least).
            response.getReturns()
                    .forEach(reachabilityStatus -> personalNumberMailboxList.add(getMailboxSettings(reachabilityStatus)));
        }
        
        return personalNumberMailboxList;
    }
    
    /**
     * Check that:
     *  - there's not a pending accountregistration (that we have somewhere to send the message)
     *  - The sender is accepted by the recipient (no difference between disallowing and no mailbox)
     *  - that there's an existing servicesupplier object.
     * @param reachabilityStatus status of the recipient
     * @return Optional {@link Mailbox} containing the url and recipientId.
     */
    private Optional<Mailbox> getMailboxSettings(ReachabilityStatus reachabilityStatus) {
        Optional<Mailbox> possibleMailbox = Optional.empty();
        
        if( reachabilityStatus.isSenderAccepted() &&                                                                        //Make sure the recipient accepts the sender (Sundsvalls kommun)
            reachabilityStatus.getAccountStatus().getServiceSupplier() != null &&                                           //If the recipient doesn't have a mailbox this will not be present
            !reachabilityStatus.getAccountStatus().isPending() &&                                                           //It should not be a pending account registration
            isSupportedSupplier(reachabilityStatus.getAccountStatus().getServiceSupplier().getName()) &&                    //Check that we support the supplier
            StringUtils.isNotBlank(reachabilityStatus.getAccountStatus().getServiceSupplier().getServiceAdress())) {        //Make sure we have an address to send something to.
    
            final String recipientId = reachabilityStatus.getAccountStatus().getRecipientId();
            final String serviceAdress = reachabilityStatus.getAccountStatus().getServiceSupplier().getServiceAdress();
            final String serviceName = getShortSupplierName(reachabilityStatus.getAccountStatus().getServiceSupplier().getName());
            
            Mailbox mailbox = new Mailbox(recipientId, serviceAdress, serviceName);
            return Optional.of(mailbox);
        }
        
        return possibleMailbox;
    }
    
    /**
     * Check if the service supplier "name" is one that we support.
     * @param serviceSupplier
     * @return
     */
    boolean isSupportedSupplier(String serviceSupplier) {
        return supportedSuppliers.stream()
                .anyMatch(s -> serviceSupplier.toLowerCase().contains(s.toLowerCase()));
    }
    
    /**
     * Map the supplier name into our own "short" name.
     * @return
     */
    String getShortSupplierName(String originalSupplierName) {
        for(String shortName : supportedSuppliers) {
            if(originalSupplierName.toLowerCase().contains(shortName.toLowerCase())) {
                return shortName.toLowerCase();
            }
        }
        
        // Shouldn't be able to get here since we check that we support the supplier before but, need to return something.
        return originalSupplierName;
    }
}
