package se.sundsvall.digitalmail.api;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.problem.Problem;
import se.sundsvall.digitalmail.api.model.DigitalMailRequest;
import se.sundsvall.digitalmail.api.model.DigitalMailResponse;
import se.sundsvall.digitalmail.api.model.validation.DigitalMailRequestValidator;
import se.sundsvall.digitalmail.domain.DigitalMailRequestDto;
import se.sundsvall.digitalmail.service.DigitalMailService;

import javax.validation.Valid;

@RestController
@RequestMapping(value = "/")
@Tag(name = "Digital Mail")
public class DigitalMailResource {
    
    private final DigitalMailService digitalMailService;
    private final DigitalMailRequestValidator validator;
    public DigitalMailResource(DigitalMailService digitalMailService, final DigitalMailRequestValidator validator) {this.digitalMailService = digitalMailService;
        this.validator = validator;
    }
    
    @ApiResponse(responseCode = "200", description = "Successful Operation", content = @Content(schema = @Schema(implementation = DigitalMailResponse.class)))
    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(schema = @Schema(implementation = Problem.class)))
    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content(schema = @Schema(implementation = Problem.class)))
    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(schema = @Schema(implementation = Problem.class)))
    @PostMapping(value = "/sendDigitalMail", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DigitalMailResponse> sendDigitalMail(
            @Valid
            @RequestBody
            DigitalMailRequest request) {
        validator.validateRequest(request);
        var response = digitalMailService.sendDigitalMail(new DigitalMailRequestDto(request));
        return ResponseEntity.ok(response);
    }
}
