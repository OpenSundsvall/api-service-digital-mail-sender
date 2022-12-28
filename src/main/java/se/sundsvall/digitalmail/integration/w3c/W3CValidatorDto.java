package se.sundsvall.digitalmail.integration.w3c;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple wrapper class for "parsing" the response from the w3c-validator.
 * We don't care about the content since a non-empty response means that the html didn't validate.
 */
@Data
public class W3CValidatorDto {
    
    List<Message> messages = new ArrayList<>();
    
    public static class Message {}
}
