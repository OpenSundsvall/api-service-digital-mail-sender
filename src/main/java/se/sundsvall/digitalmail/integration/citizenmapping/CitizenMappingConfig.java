package se.sundsvall.digitalmail.integration.citizenmapping;

import java.util.concurrent.TimeUnit;

import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

import se.sundsvall.dept44.configuration.feign.FeignConfiguration;
import se.sundsvall.dept44.configuration.feign.FeignMultiCustomizer;
import se.sundsvall.dept44.configuration.feign.decoder.ProblemErrorDecoder;

import feign.Request;
import feign.codec.ErrorDecoder;

@Import(FeignConfiguration.class)
public class CitizenMappingConfig {
    
    private final CitizenMappingProperties properties;
    
    CitizenMappingConfig(final CitizenMappingProperties properties) {
        this.properties = properties;
    }
    
    @Bean
    FeignBuilderCustomizer feignBuilderCustomizer() {
        return FeignMultiCustomizer.create()
                .withErrorDecoder(errorDecoder())
                .withRequestOptions(feignOptions())
                .withRetryableOAuth2InterceptorForClientRegistration(clientRegistration())
                .composeCustomizersToOne();
    }

    private ClientRegistration clientRegistration() {
        return ClientRegistration.withRegistrationId("citizenmapping")
                .tokenUri(properties.getOauth2TokenUrl())
                .clientId(properties.getOauth2ClientId())
                .clientSecret(properties.getOauth2ClientSecret())
                .authorizationGrantType(new AuthorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS.getValue()))
                .build();
    }

    Request.Options feignOptions() {
        return new Request.Options(
                properties.getConnectTimeout().toMillis(), TimeUnit.MILLISECONDS,
                properties.getReadTimeout().toMillis(), TimeUnit.MILLISECONDS,
            true);
    }

    ErrorDecoder errorDecoder() {
        return new ProblemErrorDecoder("CitizenMappingClient");
    }
}
