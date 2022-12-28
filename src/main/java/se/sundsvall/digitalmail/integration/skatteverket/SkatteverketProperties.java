package se.sundsvall.digitalmail.integration.skatteverket;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "integration.skatteverket")
public class SkatteverketProperties {
    
    @Value("#{'${integration.skatteverket.supported-suppliers}'.split(',')}")
    private List<String> supportedSuppliers;
    
    private boolean shouldUseKeystore;
    private String recipientUrl;
    private String notificationUrl;
    private String keyStoreLocation;
    private String keyStorePassword;

    private long connectTimeout;
    private long readTimeout;
}
