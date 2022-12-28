package se.sundsvall.digitalmail.integration.skatteverket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.zalando.logbook.Logbook;
import se.sundsvall.dept44.configuration.webservicetemplate.WebServiceTemplateBuilder;
import se.sundsvall.digitalmail.integration.skatteverket.interceptor.SoapMessageSizeInterceptor;

import java.time.Duration;

@Configuration
public class SkatteverketClientConfig {
    
    private final Logbook logbook;
    private final SkatteverketProperties properties;
    private final SoapMessageSizeInterceptor soapMessageSizeInterceptor;
    
    public SkatteverketClientConfig(final SkatteverketProperties properties, final Logbook logbook, final SoapMessageSizeInterceptor soapMessageSizeInterceptor) {
        this.logbook = logbook;
        this.properties = properties;
        this.soapMessageSizeInterceptor = soapMessageSizeInterceptor;
    }
    
    //Separate the beans since we don't want the "reachable" one to get intercepted for the size-check.
    @Bean("skatteverket-sendmail-webservice-template")
    public WebServiceTemplate notificationWebserviceTemplate() {
        final WebServiceTemplateBuilder builder = new WebServiceTemplateBuilder()
                .withConnectTimeout(Duration.ofMillis(properties.getConnectTimeout()))
                .withReadTimeout(Duration.ofMillis(properties.getReadTimeout()))
                .withLogbook(logbook)
                .withPackageToScan("se.gov.minameddelanden.schema");
    
        if(properties.isShouldUseKeystore()) {
            builder.withKeyStoreFileLocation(properties.getKeyStoreLocation())
                    .withKeyStorePassword(properties.getKeyStorePassword());
        }
        
        //Since we need to set the url dynamically we won't set the base url here.
        return builder
                .withClientInterceptor(soapMessageSizeInterceptor)
                .build();
    }
    
    @Bean("skatteverket-isreachable-webservice-template")
    public WebServiceTemplate recipientWebserviceTemplate() {
        final WebServiceTemplateBuilder builder = new WebServiceTemplateBuilder()
                .withConnectTimeout(Duration.ofMillis(properties.getConnectTimeout()))
                .withReadTimeout(Duration.ofMillis(properties.getReadTimeout()))
                .withLogbook(logbook)
                .withPackageToScan("se.gov.minameddelanden.schema");
    
        if(properties.isShouldUseKeystore()) {
            builder.withKeyStoreFileLocation(properties.getKeyStoreLocation())
                    .withKeyStorePassword(properties.getKeyStorePassword());
        }
        return builder
                .withBaseUrl(properties.getRecipientUrl())
                .build();
    }
}
