package se.sundsvall.digitalmail.domain;

/**
 * Simple record for sending recipientId, serviceAddress and serviceName back and forth
 * @param recipientId
 * @param serviceAddress
 */
public record Mailbox(String recipientId, String serviceAddress, String serviceName) {
}
