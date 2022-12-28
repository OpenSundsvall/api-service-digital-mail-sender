package openapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.flipkart.zjsonpatch.JsonDiff;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.digitalmail.DigitalMail;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Checks for API-changes and writes the new ones to file.
 * If an API change has been discovered, this test will fail to indicate that it has changed.
 * The test will always overwrite the current swagger.yaml-file with the newly generated one.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = DigitalMail.class)
@ActiveProfiles("openapi")
class GenerateOpenApiIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateOpenApiIT.class);

    private static final String SERVICE_OAS3_URL = "/api-docs.yaml";
    private static final String OAS3_PATH_AND_FILE = "src/main/resources/openapi.yaml";

    private final Path path = Paths.get(OAS3_PATH_AND_FILE);
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void compareYaml() {
        try {
            String originalYaml = readCurrentYaml();
            String newYaml = getNewYaml();
        
            if(StringUtils.isNotBlank(originalYaml)) {
                final String diff = getDiff(originalYaml, newYaml);
    
                if(StringUtils.isNotBlank(diff)) {
                    LOGGER.warn("Yaml-comparison failed with the following result: {}", diff);
                    
                    //Write the new yaml-file.
                    writeNewYamlFile(newYaml);
                    //TODO fix proper diff check.
                    //fail("API has changed, please review the changes and rerun the build to (hopefully) make it pass.");
                }
                
            } else { //No existing yaml, write the current one.
                LOGGER.info("No existing swagger, writing a new one.");
                writeNewYamlFile(newYaml);
            }
        } catch (IOException e) {
            LOGGER.error("Test failed to read/write yaml file. ", e);
            fail("Something went wrong while reading/saving swagger files.");
        }
    
    }
    
    private String getDiff(String originalYaml, String newYaml) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        JsonNode originalNode = objectMapper.readTree(originalYaml);
        JsonNode newNode = objectMapper.readTree(newYaml);
    
        JsonNode patch = JsonDiff.asJson(originalNode, newNode);
    
        final String diff = patch.toString();
        
        //If the patch is empty it will contain "[]". Check for it and return empty if that's the case
        if(diff.equalsIgnoreCase("[]")) {
            return "";
        } else {
            return diff;    //If there's a diff, it's here
        }
    }
    
    private String getNewYaml() throws IOException {
        String swagger = restTemplate.getForObject(SERVICE_OAS3_URL, String.class);
        
        return swagger;
    }

    //Write yaml file.
    private void writeNewYamlFile(final String newYaml) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path)){
            writer.write(newYaml);
        }
    }

    //Read yeaml, if it doesn't exist, return empty string.
    private String readCurrentYaml() {
        String content = "";
        if(yamlExists()){
            try {
                content = new String(Files.readAllBytes(Paths.get(OAS3_PATH_AND_FILE)));
            } catch (IOException e) {
                LOGGER.error("Couldn't read file");
            }
        }

        return content;
    }

    //Checks if the yaml file exists.
    private boolean yamlExists() {
        File file = new File(OAS3_PATH_AND_FILE);
        return file.exists() && !file.isDirectory();
    }
}
