package se.sundsvall.digitalmail.integration.skatteverket.sendmail;

public class SendMailHealthIndicator extends se.sundsvall.digitalmail.integration.DigitalMailHealthIndicator {
    
    public SendMailHealthIndicator() {
        super(DigitalMailIntegration.INTEGRATION_NAME);
    }
}
