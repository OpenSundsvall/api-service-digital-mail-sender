package se.sundsvall.digitalmail.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.MediaType;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.digitalmail.api.model.validation.In;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(setterPrefix = "with")
public class DigitalMailRequest {
    
    @ValidUuid
    @Schema(description = "partyId for the person or organization the digital mail should be sent to", example = "6a5c3d04-412d-11ec-973a-0242ac130003", required = true)
    private String partyId;
    
    @ValidMunicipalityId
    @Schema(description = "MunicipalityId", example = "2281", required = true)
    private String municipalityId;
    
    @NotBlank
    @Schema(description = "The subject of the digital mail.", example = "Viktig information från Sundsvalls kommun", required = true)
    private String headerSubject;
    
    @NotNull
    @Valid
    private SupportInfo supportInfo;

    @Builder.Default
    private List<Attachment> attachments = new ArrayList<>();
    
    @Valid
    private BodyInformation bodyInformation;
    
    @Setter
    @Getter
    @Builder(setterPrefix = "with")
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "An attachment, e.g. pdf, docx etc.")
    public static class Attachment {
        
        @In(MediaType.APPLICATION_PDF_VALUE)
        @Schema(description = "Allowed type is: application/pdf", example = MediaType.APPLICATION_PDF_VALUE, required = true)
        private String contentType;
        
        @NotBlank
        @Schema(description = "Base64-encoded body", required = true)
        private String body;
    
        @NotBlank
        @Schema(description = "The name of the file", example = "sample.pdf", required = true)
        private String filename;
    }
}
