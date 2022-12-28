package se.sundsvall.digitalmail.integration.skatteverket;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import se.gov.minameddelanden.schema.recipient.AccountStatus;
import se.gov.minameddelanden.schema.recipient.ReachabilityStatus;
import se.gov.minameddelanden.schema.recipient.ServiceSupplier;
import se.gov.minameddelanden.schema.recipient.v3.IsReachable;
import se.gov.minameddelanden.schema.recipient.v3.IsReachableResponse;
import se.sundsvall.digitalmail.domain.Mailbox;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("junit")
class RecipientIntegrationMapperTest {
    
    @Autowired
    private RecipientIntegrationMapper mapper;
    
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    @Test
    void testCreateIsRegistered() {
        List<String> list = new ArrayList<>();
        list.add("197001011234");
        final IsReachable isReachableRequest = mapper.createIsReachableRequest(list);
        System.out.println(gson.toJson(isReachableRequest));
        
        assertThat(isReachableRequest.getSenderOrgNr()).isEqualTo("162120002411");
        assertThat(isReachableRequest.getRecipientIds().get(0)).isEqualTo("197001011234");
    }
    
    @Test
    void testGetMailboxSettings_shouldReturnRecipientId_WhenPresent() {
        IsReachableResponse response = createIsReachableResponse(false, true, true);
        final List<Optional<Mailbox>> mailboxSettings = mapper.getMailboxSettings(response);
        assertThat(mailboxSettings.get(0).isPresent()).isTrue();
        assertThat(mailboxSettings.get(0).get().serviceAddress()).isEqualTo("http://somewhere.com");
        assertThat(mailboxSettings.get(0).get().recipientId()).isEqualTo("recipientId");
        
    }
    
    @Test
    void testGetMailboxSettings_shouldReturnEmpty_WhenPending() {
        IsReachableResponse response = createIsReachableResponse(true, true, false);
        final List<Optional<Mailbox>> mailboxSettings = mapper.getMailboxSettings(response);
        assertThat(mailboxSettings.get(0).isPresent()).isFalse();
    }
    
    @Test
    void testGetMailboxSettings_shouldReturnEmpty_WhenNoServiceSupplier() {
        IsReachableResponse response = createIsReachableResponse(false, false, false);
        final List<Optional<Mailbox>> mailboxSettings = mapper.getMailboxSettings(response);
        assertThat(mailboxSettings.get(0).isPresent()).isFalse();
    }
    
    @Test
    void testGetMailboxSettings_shouldReturnEmpty_whenSenderNotAccepted() {
        IsReachableResponse response = createIsReachableResponse(false, true, true);
        final List<Optional<Mailbox>> mailboxSettings = mapper.getMailboxSettings(response);
        assertThat(mailboxSettings.get(0).isPresent()).isTrue();
    }
    
    @Test
    void testFindMatchingSupplier() {
        assertThat(mapper.isSupportedSupplier("billo")).isTrue();
        assertThat(mapper.isSupportedSupplier("fortnox")).isTrue();
        assertThat(mapper.isSupportedSupplier("Kivra")).isTrue();
        assertThat(mapper.isSupportedSupplier("minmyndighetspost")).isTrue();
        assertThat(mapper.isSupportedSupplier("notSupported")).isFalse();
    }
    
    @Test
    void testFindShortSupplierName_shouldMapAndReturnShortName() {
        assertThat(mapper.getShortSupplierName("Billo")).isEqualTo("billo");
        assertThat(mapper.getShortSupplierName("Fortnox")).isEqualTo("fortnox");
        assertThat(mapper.getShortSupplierName("Kivra")).isEqualTo("kivra");
        assertThat(mapper.getShortSupplierName("minmyndighetspost.se")).isEqualTo("minmyndighetspost");
    }
    
    /**
     *
     * @param pending if pending, the mailbox has not yet been created and should be interpreted as the recipient not having a digital mailbox.
     * @param shouldHaveServiceSupplier No serviceSupplier indicates that the recipient doesn't have a digital mailbox.
     * @param isAccepted is the sender accepted by the recipient
     * @return
     */
    private IsReachableResponse createIsReachableResponse(boolean pending, boolean shouldHaveServiceSupplier, boolean isAccepted) {
        final IsReachableResponse response = new IsReachableResponse()
                .withReturns(List.of(new ReachabilityStatus()
                                .withSenderAccepted(isAccepted)
                                .withAccountStatus(new AccountStatus()
                                    .withRecipientId("recipientId")
                                    .withPending(pending))));
        
        if(shouldHaveServiceSupplier) {
            response.getReturns().get(0).getAccountStatus()
                        .withServiceSupplier(new ServiceSupplier()
                            .withId("165568402266")
                            .withName("Kivra")
                            .withServiceAdress("http://somewhere.com"));
        }
        
        return response;
    }
}
