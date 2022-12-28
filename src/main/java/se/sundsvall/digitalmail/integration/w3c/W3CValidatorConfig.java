package se.sundsvall.digitalmail.integration.w3c;

import java.util.concurrent.TimeUnit;

import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import se.sundsvall.dept44.configuration.feign.FeignConfiguration;
import se.sundsvall.dept44.configuration.feign.FeignMultiCustomizer;
import se.sundsvall.dept44.configuration.feign.decoder.ProblemErrorDecoder;

import feign.Request;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;

@Import(FeignConfiguration.class)
public class W3CValidatorConfig {

    private final W3CValidatorProperties properties;

    public W3CValidatorConfig(W3CValidatorProperties properties) {
        this.properties = properties;
    }

    @Bean
    public FeignBuilderCustomizer feignBuilderCustomizer() {
        return FeignMultiCustomizer.create()
                .withErrorDecoder(errorDecoder())
                .withRequestOptions(feignOptions())
                .composeCustomizersToOne();
    }
    
    public ErrorDecoder errorDecoder() {
        return new ProblemErrorDecoder("W3CClient");
    }
    
    @Bean
    RequestInterceptor headerInterceptor() {
        return template -> template.header("Content-Type", "text/html; charset=utf-8");
    }

    Request.Options feignOptions() {
        return new Request.Options(
                properties.getConnectTimeout().toMillis(), TimeUnit.MILLISECONDS,
                properties.getReadTimeout().toMillis(), TimeUnit.MILLISECONDS,
                true
        );
    }
}
