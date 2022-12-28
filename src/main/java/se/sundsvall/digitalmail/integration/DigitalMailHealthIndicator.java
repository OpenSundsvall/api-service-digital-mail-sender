package se.sundsvall.digitalmail.integration;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;

import java.text.SimpleDateFormat;
import java.util.concurrent.atomic.LongAdder;

public abstract class DigitalMailHealthIndicator implements HealthIndicator {

    protected static final String STATUS_DESCRIPTION = "Status description";
    protected static final String LATEST_ATTEMPT = "Latest attempt";
    protected static final String UP_SINCE_TIME = "UP since";
    protected static final String OUT_OF_SERVICE_SINCE = "OUT_OF_SERVICE since";
    protected static final String INTEGRATION_WORKING = "Integration is working";
    protected static final String INTEGRATION_OUT_OF_SERVICE = "Integration is out of service";
    protected static final String LOG_INTEGRATION_WORKING = "Setting health status on integration {} to UP";
    protected static final String LOG_INTEGRATION_OUT_OF_SERVICE = "Setting health status on integration {} to OUT_OF_SERVICE";
    protected static final String LOG_INTEGRATION_RESET = "Resetting health status on integration {} to UNKNOWN";
    protected static final String SUCCESSFUL_SINCE_UP = "Successful calls since status changed to UP";
    protected static final String FAILED_SICNE_OUT_OF_SERVICE = "Failed calls since status changed to OUT_OF_SERVICE";
    protected static final String DETAILED_MESSAGE = "Detailed message";
    protected static final String STACKTRACE = "Stacktrace";
    protected static final String NEVER_CALLED = "Never called";
    protected static final String STATUS_WAS_RESET = "Status was reset";

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());    //Different name on this one since the other healt indicators extend this class.
    
    private final String integrationName;
    
    private Health currentHealth;
    private final Counter successfulCounter;
    private final Counter failedCounter;
    private final LongAdder successfulSinceUP;
    private final LongAdder failedSinceOutOfService;
    private String upSince;
    private String outOfServiceSince;
    
    protected DigitalMailHealthIndicator(String integrationName) {
        this.integrationName = integrationName;
        currentHealth = Health.unknown()
                .withDetail(STATUS_DESCRIPTION, NEVER_CALLED)
                .build();
        successfulCounter = Metrics.counter("meter.integration." + integrationName + ".successful");
        failedCounter = Metrics.counter("meter.integration." + integrationName + ".failed");
        successfulSinceUP = Metrics.gauge("gauge.integration." + integrationName + ".successfulSinceUp", new LongAdder());
        failedSinceOutOfService = Metrics.gauge("gauge.integration." + integrationName + ".failedSinceDown", new LongAdder());
        
    }
    
    public void resetHealth() {
        LOGGER.info(LOG_INTEGRATION_RESET, integrationName);
        currentHealth = Health.unknown()
                .withDetail(STATUS_DESCRIPTION, NEVER_CALLED)
                .withDetail(STATUS_WAS_RESET, getTimeStamp())
                .build();
    }
    
    public String getIntegrationName() {
        return integrationName;
    }
    
    public void setUpStatus() {
        if (!currentHealth.getStatus().equals(Status.UP)) {
            LOGGER.info(LOG_INTEGRATION_WORKING, integrationName);
            failedSinceOutOfService.reset();
            upSince = getTimeStamp();
        }
        
        successfulCounter.increment();
        successfulSinceUP.increment();
        
        currentHealth = Health.up()
                .withDetail(STATUS_DESCRIPTION, INTEGRATION_WORKING)
                .withDetail(UP_SINCE_TIME, upSince)
                .withDetail(LATEST_ATTEMPT, getTimeStamp())
                .withDetail(SUCCESSFUL_SINCE_UP, successfulSinceUP.intValue())
                .build();
    }
    
    public void setOutOfService(Exception e) {
        setCountersAndTimeStampAndLogForOutOfService();
        currentHealth = Health.outOfService()
                .withDetail(STATUS_DESCRIPTION, INTEGRATION_OUT_OF_SERVICE)
                .withDetail(OUT_OF_SERVICE_SINCE, outOfServiceSince)
                .withDetail(LATEST_ATTEMPT, getTimeStamp())
                .withDetail(FAILED_SICNE_OUT_OF_SERVICE, failedSinceOutOfService.intValue())
                .withException(e)
                .build();
    }
    
    public void setOutOfServiceWithMessage(String message) {
        setCountersAndTimeStampAndLogForOutOfService();
        currentHealth = Health.outOfService()
                .withDetail(STATUS_DESCRIPTION, INTEGRATION_OUT_OF_SERVICE)
                .withDetail(DETAILED_MESSAGE, StringUtils.isEmpty(message) ? "Undefined": message)
                .withDetail(OUT_OF_SERVICE_SINCE, outOfServiceSince)
                .withDetail(LATEST_ATTEMPT, getTimeStamp())
                .withDetail(FAILED_SICNE_OUT_OF_SERVICE, failedSinceOutOfService.intValue())
                .build();
    }
    
    private void setCountersAndTimeStampAndLogForOutOfService() {
        if (!currentHealth.getStatus().equals(Status.OUT_OF_SERVICE)) {
            LOGGER.warn(LOG_INTEGRATION_OUT_OF_SERVICE, integrationName);
            successfulSinceUP.reset();
            outOfServiceSince = getTimeStamp();
        }
        failedCounter.increment();
        failedSinceOutOfService.increment();
    }
    
    private String getTimeStamp() {
        return new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(new java.util.Date());
    }
    
    @Override
    public Health health() {
        return currentHealth;
    }
}
