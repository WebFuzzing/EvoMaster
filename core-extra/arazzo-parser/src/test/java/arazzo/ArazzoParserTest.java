package arazzo;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import com.webfuzzing.arazzo.access.ArazzoAccess;
import com.webfuzzing.arazzo.models.domain.ArazzoSpecifications;
import com.webfuzzing.arazzo.parser.ArazzoParser;
import org.junit.Assert;
import org.junit.Test;

public class ArazzoParserTest {

    private final String BASE_RESOURCE_ARAZZO = "src/test/resources/arazzo";
    private final String BASE_RESOURCE_OPENAPI = "src/test/resources/openapi";

    @Test
    public void shouldParseArazzoYamlSuccessfully() throws Exception {
        String schemaText = ArazzoAccess.readFromDisk(BASE_RESOURCE_ARAZZO + "/arazzo_pet.yaml");
        OpenAPI openAPI = new OpenAPIV3Parser().read(BASE_RESOURCE_OPENAPI + "/openapi_pet.json");

        ArazzoSpecifications arazzo = ArazzoParser.parse(schemaText, openAPI);

        Assert.assertEquals(3, arazzo.getWorkflows().size());
        Assert.assertEquals("Petstore - Apply Coupons", arazzo.getInfo().getTitle());
    }

    @Test
    public void shouldParseArazzoJsonSuccessfully() throws Exception {
        String schemaText = ArazzoAccess.readFromDisk(BASE_RESOURCE_ARAZZO + "/arazzo_pet.json");
        OpenAPI openAPI = new OpenAPIV3Parser().read(BASE_RESOURCE_OPENAPI + "/openapi_pet.json");

        ArazzoSpecifications arazzo = ArazzoParser.parse(schemaText, openAPI);

        Assert.assertEquals(3, arazzo.getWorkflows().size());
        Assert.assertEquals("Petstore - Apply Coupons", arazzo.getInfo().getTitle());
    }

}
