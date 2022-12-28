package apptest;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;

public class CustomWiremockExtension extends ResponseDefinitionTransformer {
    
    private final int wiremockPort;
    
    public CustomWiremockExtension(final int wiremockPort) {
        this.wiremockPort = wiremockPort;
    }
    
    @Override
    public String getName() {
        return "response-template";
    }
    
    @Override
    public boolean applyGlobally() {
        return false;
    }
    
    /**
     * The response from skatteverket will contain the url to where we should send the digital mail.
     * Since the response will need to contain the port for wiremock we need to set it here and then transform the url-part with the url and port towards wiremock.
     */
    @Override
    public ResponseDefinition transform(final Request request, final ResponseDefinition responseDefinition, final FileSource files, final Parameters parameters) {
        return ResponseDefinitionBuilder.like(responseDefinition)
                .but()
                .withBody(responseDefinition
                        .getBody()
                        .replace("https://mm.kivra.com/service/v3", "http://localhost:" + wiremockPort + "/deliversecure"))
                .build();
    }
}
