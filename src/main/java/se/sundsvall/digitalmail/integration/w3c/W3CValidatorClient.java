package se.sundsvall.digitalmail.integration.w3c;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(
        name = "W3CClient",
        url = "${integration.w3cvalidator.url}",
        configuration = W3CValidatorConfig.class
)
public interface W3CValidatorClient {
    
    @PostMapping
    W3CValidatorDto validate(String html);
}
