package se.sundsvall.digitalmail.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.MediaType;
import se.sundsvall.digitalmail.api.model.validation.In;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
@Schema(description = "The body of the message", required = true)
@Builder(setterPrefix = "with")
@AllArgsConstructor
@NoArgsConstructor
public class BodyInformation {
    
    @In({ MediaType.TEXT_PLAIN_VALUE, MediaType.TEXT_HTML_VALUE})
    @NotBlank(message = "contentType must not be blank")
    @Schema(description = "The content type for the message, text/plain for only text, text/html for html messages.", example = MediaType.TEXT_HTML_VALUE, required = true)
    private String contentType;
    
    @NotBlank
    @Schema(description = "Plain text if contentType is set to 'text/plain', BASE64-encoded if contentType is set to 'text/html.", example = "PCFET0NUWVBFIGh0bWw+PGh0bWwgbGFuZz0iZW4iPjxoZWFkPjxtZXRhIGNoYXJzZXQ9InV0Zi04Ij48dGl0bGU+VGVzdDwvdGl0bGU+PC9oZWFkPjxib2R5PjxwPkhlbGxvPC9wPjwvYm9keT48L2h0bWw+", required = true)
    private String body;
}
