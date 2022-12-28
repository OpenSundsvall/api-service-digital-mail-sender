package apptest;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.digitalmail.DigitalMail;

@WireMockAppTestSuite(files = "classpath:/DigitalMailIT/", classes = DigitalMail.class)
public class DigitalMailIT extends AbstractAppTest {
    
    //Autowire the wiremockserver to be able to get the port.
    @Autowired WireMockServer wireMockServer;
    
    private static final String SERVICE_PATH = "/sendDigitalMail/";
    
    private int wiremockPort;
    @BeforeEach
    void setup() {
        this.wiremockPort = wireMockServer.getOptions().portNumber();
    }
    
    @Test
    void test1_successfulRequest_shouldReturnDistributionId() throws Exception {
        setupCall()
                .withExtensions(new CustomWiremockExtension(wiremockPort))
                .withServicePath(SERVICE_PATH)
                .withRequest("request.json")
                .withHttpMethod(HttpMethod.POST)
                .withExpectedResponseStatus(HttpStatus.OK)
                .withExpectedResponse("expected.json")
                .sendRequestAndVerifyResponse()
                .verifyAllStubs();
    }
}
