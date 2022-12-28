package se.sundsvall.digitalmail.integration.w3c;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "integration.w3cvalidator")
public class W3CValidatorProperties {
    private Duration connectTimeout;
    private Duration readTimeout;
}
