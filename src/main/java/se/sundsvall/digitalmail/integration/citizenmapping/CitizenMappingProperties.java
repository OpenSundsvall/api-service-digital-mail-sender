package se.sundsvall.digitalmail.integration.citizenmapping;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "integration.citizenmapping")
public class CitizenMappingProperties {
    
    private String apiUrl;
    private String oauth2TokenUrl;
    private String oauth2ClientId;
    private String oauth2ClientSecret;
    private Duration connectTimeout;
    private Duration readTimeout;
}
