package se.sundsvall.digitalmail.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.ThrowableProblem;
import se.gov.minameddelanden.schema.recipient.v3.IsReachable;
import se.gov.minameddelanden.schema.recipient.v3.IsReachableResponse;
import se.sundsvall.digitalmail.domain.Mailbox;
import se.sundsvall.digitalmail.integration.skatteverket.RecipientIntegrationMapper;
import se.sundsvall.digitalmail.integration.skatteverket.reachable.ReachableIntegration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {
    
    @Mock
    private RecipientIntegrationMapper mockSkatteverketMapper;
    
    @Mock
    private ReachableIntegration mockReachableIntegration;
    
    private AvailabilityService availabilityService;
    
    @BeforeEach
    public void setup() {
        availabilityService = new AvailabilityService(mockSkatteverketMapper, mockReachableIntegration);
    }
    
    @Test
    void testPresentMailbox_shouldReturnMailbox() {
        when(mockSkatteverketMapper.createIsReachableRequest(anyList())).thenReturn(new IsReachable());
        when(mockReachableIntegration.callSkatteverketIsReacheble(any(IsReachable.class))).thenReturn(new IsReachableResponse());
        when(mockSkatteverketMapper.getMailboxSettings(any(IsReachableResponse.class))).thenReturn(generatePresentMailboxResponse());
    
        final List<Mailbox> mailboxes = availabilityService.getRecipientMailboxesAndCheckAvailability(List.of("personalNumber"));
        
        assertThat(mailboxes).isNotEmpty();
    }
    
    @Test
    void testEmptyOptionalMailbox_shouldOnlyReturnPresentMailboxes() {
        when(mockSkatteverketMapper.createIsReachableRequest(anyList())).thenReturn(new IsReachable());
        when(mockReachableIntegration.callSkatteverketIsReacheble(any(IsReachable.class))).thenReturn(new IsReachableResponse());
    
        final List<Optional<Mailbox>> empty = generateEmptyMailboxResponse();
        final List<Optional<Mailbox>> present = generatePresentMailboxResponse();
        
        //Combine them
        present.addAll(empty);
    
        when(mockSkatteverketMapper.getMailboxSettings(any(IsReachableResponse.class))).thenReturn(present);
    
        final List<Mailbox> mailboxes = availabilityService.getRecipientMailboxesAndCheckAvailability(List.of("personalNumber"));
    
        assertThat(mailboxes).isNotEmpty();
        assertThat(mailboxes).singleElement().isNotNull();
    }
    
    @Test
    void testNoMailbox_shouldThrowException() {
        when(mockSkatteverketMapper.createIsReachableRequest(anyList())).thenReturn(new IsReachable());
        when(mockReachableIntegration.callSkatteverketIsReacheble(any(IsReachable.class))).thenReturn(new IsReachableResponse());
    
        when(mockSkatteverketMapper.getMailboxSettings(any(IsReachableResponse.class))).thenReturn(generateEmptyMailboxResponse());
        
        assertThatExceptionOfType(ThrowableProblem.class)
                .isThrownBy(() -> availabilityService.getRecipientMailboxesAndCheckAvailability(List.of("personalNumber")));
    }
    
    private List<Optional<Mailbox>> generateEmptyMailboxResponse() {
        List<Optional<Mailbox>> possibleMailboxes = new ArrayList<>();
        Optional<Mailbox> optionalMailbox = Optional.empty();
        possibleMailboxes.add(optionalMailbox);
        
        return possibleMailboxes;
    }
    
    private List<Optional<Mailbox>> generatePresentMailboxResponse() {
        List<Optional<Mailbox>> possibleMailboxes = new ArrayList<>();
        Mailbox mailbox = new Mailbox("recipientId", "serviceAddress", "serviceName");
    
        final Optional<Mailbox> optionalMailbox = Optional.of(mailbox);
        possibleMailboxes.add(optionalMailbox);
        return possibleMailboxes;
    }
    
}