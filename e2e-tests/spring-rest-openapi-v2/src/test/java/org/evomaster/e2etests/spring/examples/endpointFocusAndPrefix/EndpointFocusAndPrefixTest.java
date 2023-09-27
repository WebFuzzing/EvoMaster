package org.evomaster.e2etests.spring.examples.endpointFocusAndPrefix;

import com.foo.rest.examples.spring.endpointFocusAndPrefix.EndpointFocusAndPrefixController;

import org.evomaster.client.java.instrumentation.shared.ClassName;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.OpenApiAccess;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.PathItem;

import java.util.Collections;
import java.util.List;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class EndpointFocusAndPrefixTest extends SpringTestBase {

    @BeforeAll
    /*
     */
    public static void initClass() throws Exception {
        SpringTestBase.initClass(new EndpointFocusAndPrefixController());
    }

    @Test
    public void testRunBlackboxWithoutFocusOrPrefix() {

        String outputFolder = "EndPointFocusAndPrefix";

        List<String> args = getArgsWithCompilation(
                40,
                outputFolder,
                ClassName.get("org.foo.BlackboxWithoutFocusOrPrefix"),
                true);
        // program arguments for EvoMaster
        args.add("--blackBox");
        args.add("true");
        args.add("--bbTargetUrl");
        args.add(baseUrlOfSut);
        args.add("--bbSwaggerUrl");
        args.add(baseUrlOfSut + "/v2/api-docs");

        // no endpointFocus or endpointPrefix is provided
        Solution<RestIndividual> solution = initAndRun(args);

        // provide empty list to check for focus or prefix and put focusMode to false,
        // which means prefix mode. Every path has the empty path as path prefix.
        List<String> pathsToCheck = Collections.emptyList();

        // if neither focus nor prefix is provided, then all paths should include empty path as a prefix
        assertAllSolutionsHavePathFocusOrPrefixList(solution, pathsToCheck, false);

        // put into the output folder
        compile(outputFolder);
    }

    @Test
    public void testAllPathsInTestWhenFocusOrPrefixNotProvided() {

        String outputFolder = "EndPointFocusAndPrefix";

        List<String> args = getArgsWithCompilation(
                40,
                outputFolder,
                ClassName.get("org.foo.AllPathsInTestWhenFocusOrPrefixNotProvided"),
                true);
        // program arguments for EvoMaster
        args.add("--blackBox");
        args.add("true");
        args.add("--bbTargetUrl");
        args.add(baseUrlOfSut);
        args.add("--bbSwaggerUrl");
        args.add(baseUrlOfSut + "/v2/api-docs");

        // no endpointFocus or endpointPrefix is provided
        Solution<RestIndividual> solution = initAndRun(args);

        // paths to check
        List<String> pathsToCheck = Collections.emptyList();

        // if neither focus nor prefix is provided, then all paths should include empty path as a prefix
        assertAllSolutionsHavePathFocusOrPrefixList(solution, pathsToCheck, false);

        // get all paths from the swagger
        OpenAPI swagger = OpenApiAccess.INSTANCE.getOpenAPIFromURL(baseUrlOfSut + "/v2/api-docs");

        // api paths
        Paths apiPaths = swagger.getPaths();

        // current path item in the API
        PathItem currentPathItem;

        // All paths in the swagger has to be included in tests
        for (String apiPathString: apiPaths.keySet()){

            // current path item
            currentPathItem = apiPaths.get(apiPathString);

            // if the path item is a GET request
            if (currentPathItem.getGet() != null) {
                assertHasAtLeastOne(solution, HttpVerb.GET, 200, apiPathString, null);
            }
            // if the path item is a POST request
            else if (currentPathItem.getPost() != null) {
                assertHasAtLeastOne(solution, HttpVerb.POST, 200, apiPathString, null);
            }
            // if the path item is a PUT request
            else if (currentPathItem.getPut() != null) {
                assertHasAtLeastOne(solution, HttpVerb.PUT, 200, apiPathString, null);
            }
            // if the path item is a DELETE request
            else if (currentPathItem.getDelete() != null) {
                assertHasAtLeastOne(solution, HttpVerb.DELETE, 200, apiPathString, null);
            }
        }
    }

    @Test
    public void testRunBlackboxWithFocusWithoutParameters() {

        String outputFolder = "EndPointFocusAndPrefix";
        String endpointFocus = "/api/pet";

        List<String> args = getArgsWithCompilation(
                40,
                outputFolder,
                ClassName.get("org.foo.BlackboxWithFocusWithoutParameters"),
                true);
        // program arguments for EvoMaster
        args.add("--blackBox");
        args.add("true");
        args.add("--bbTargetUrl");
        args.add(baseUrlOfSut);
        args.add("--bbSwaggerUrl");
        args.add(baseUrlOfSut + "/v2/api-docs");
        args.add("--endpointFocus");
        args.add(endpointFocus);

        // no endpointFocus or endpointPrefix is provided
        Solution<RestIndividual> solution = initAndRun(args);

        // include swagger into possible solutions as /v2/api-docs
        List<String> pathsToCheck = Arrays.asList(endpointFocus, "/v2/api-docs");

        // if neither focus nor prefix is provided, then all paths should include empty path as a prefix
        assertAllSolutionsHavePathFocusOrPrefixList(solution, pathsToCheck, true);

        // There are 2 endpoints with /api/pet, those and the failure case should be included in tests
        // so check that the solution contains 3 elements
        assertEquals(solution.getIndividuals().size(), 3);

        compile(outputFolder);
    }

    @Test
    public void testRunBlackboxWithFocusWithParameters() {

        String outputFolder = "EndPointFocusAndPrefix";
        String endpointFocus = "/api/pet/{petId}";

        List<String> args = getArgsWithCompilation(
                40,
                outputFolder,
                ClassName.get("org.foo.BlackboxWithFocusWithParameters"),
                true);

        // program arguments for EvoMaster
        args.add("--blackBox");
        args.add("true");
        args.add("--bbTargetUrl");
        args.add(baseUrlOfSut);
        args.add("--bbSwaggerUrl");
        args.add(baseUrlOfSut + "/v2/api-docs");
        args.add("--endpointFocus");
        args.add(endpointFocus);

        // no endpointFocus or endpointPrefix is provided
        Solution<RestIndividual> solution = initAndRun(args);

        // include swagger into possible solutions as /v2/api-docs
        List<String> pathsToCheck = Arrays.asList(endpointFocus, "/v2/api-docs");

        // if neither focus nor prefix is provided, then all paths should include empty path as a prefix
        assertAllSolutionsHavePathFocusOrPrefixList(solution, pathsToCheck, true);

        // The solution should include 4 solutions, 3 endpoints and 1 failure case
        assertEquals(solution.getIndividuals().size(), 4);

        compile(outputFolder);
    }

    @Test
    public void testRunBlackboxWithFocusOneEndpoint() {

        String outputFolder = "EndPointFocusAndPrefix";
        String endpointFocus = "/api/store/inventory";

        List<String> args = getArgsWithCompilation(
                40,
                outputFolder,
                ClassName.get("org.foo.BlackboxWithFocusOneEndpoint"),
                true);

        // program arguments for EvoMaster
        args.add("--blackBox");
        args.add("true");
        args.add("--bbTargetUrl");
        args.add(baseUrlOfSut);
        args.add("--bbSwaggerUrl");
        args.add(baseUrlOfSut + "/v2/api-docs");
        args.add("--endpointFocus");
        args.add(endpointFocus);

        // no endpointFocus or endpointPrefix is provided
        Solution<RestIndividual> solution = initAndRun(args);

        // include swagger into possible solutions as /v2/api-docs
        List<String> pathsToCheck = Arrays.asList(endpointFocus, "/v2/api-docs");

        // if neither focus nor prefix is provided, then all paths should include empty path as a prefix
        assertAllSolutionsHavePathFocusOrPrefixList(solution, pathsToCheck, true);

        // The solution should include 2 solutions, 1 endpoints and 1 failure case
        assertEquals(solution.getIndividuals().size(), 2);

        compile(outputFolder);
    }

    @Test
    public void testRunBlackboxWithPrefixWithoutParameters() {

        String outputFolder = "EndPointFocusAndPrefix";
        String endpointPrefix = "/api/user";

        List<String> args = getArgsWithCompilation(
                40,
                outputFolder,
                ClassName.get("org.foo.BlackboxWithPrefixWithoutParameters"),
                true);
        // program arguments for EvoMaster
        args.add("--blackBox");
        args.add("true");
        args.add("--bbTargetUrl");
        args.add(baseUrlOfSut);
        args.add("--bbSwaggerUrl");
        args.add(baseUrlOfSut + "/v2/api-docs");
        args.add("--endpointPrefix");
        args.add(endpointPrefix);

        // no endpointFocus or endpointPrefix is provided
        Solution<RestIndividual> solution = initAndRun(args);

        // include swagger into possible solutions as /v2/api-docs
        List<String> pathsToCheck = Arrays.asList(endpointPrefix, "/v2/api-docs");

        // if neither focus nor prefix is provided, then all paths should include empty path as a prefix
        assertAllSolutionsHavePathFocusOrPrefixList(solution, pathsToCheck, false);

        // The solution should include 8 solutions, 7 endpoints and 1 failure case
        assertEquals(solution.getIndividuals().size(), 8);

        compile(outputFolder);
    }

    @Test
    public void testRunBlackboxWithPrefixWithParameters() {

        String outputFolder = "EndPointFocusAndPrefix";
        String endpointPrefix = "/api/pet/{petId}";

        List<String> args = getArgsWithCompilation(
                40,
                outputFolder,
                ClassName.get("org.foo.BlackboxWithPrefixWithParameters"),
                true);
        // program arguments for EvoMaster
        args.add("--blackBox");
        args.add("true");
        args.add("--bbTargetUrl");
        args.add(baseUrlOfSut);
        args.add("--bbSwaggerUrl");
        args.add(baseUrlOfSut + "/v2/api-docs");
        args.add("--endpointPrefix");
        args.add(endpointPrefix);

        // no endpointFocus or endpointPrefix is provided
        Solution<RestIndividual> solution = initAndRun(args);

        // include swagger into possible solutions as /v2/api-docs
        List<String> pathsToCheck = Arrays.asList(endpointPrefix, "/v2/api-docs");

        // if neither focus nor prefix is provided, then all paths should include empty path as a prefix
        assertAllSolutionsHavePathFocusOrPrefixList(solution, pathsToCheck, false);

        // The solution should include 5 solutions, 4 endpoints and 1 failure case
        assertEquals(solution.getIndividuals().size(), 5);

        compile(outputFolder);
    }

    @Test
    public void testRunBlackboxFocusNonExistingFocusValidPrefix() {

        String outputFolder = "EndPointFocusAndPrefix";

        String endpointFocus = "/api/store";

        List<String> args = getArgsWithCompilation(
                40,
                outputFolder,
                ClassName.get("org.foo.BlackboxFocusNonExistingFocusValidPrefix"),
                true);
        // program arguments for EvoMaster
        args.add("--blackBox");
        args.add("true");
        args.add("--bbTargetUrl");
        args.add(baseUrlOfSut);
        args.add("--bbSwaggerUrl");
        args.add(baseUrlOfSut + "/v2/api-docs");
        args.add("--endpointFocus");
        args.add(endpointFocus);

        // check for IllegalArgumentException
        /*
        try {
            initAndRun(args);
        }
        catch (Exception e) {
            assertTrue(e.getCause().toString().contains(IllegalArgumentException.class.getName()));
        }

         */
        // check for IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> initAndRun(args));
    }

    @Test
    public void testRunBlackboxPrefixNonExistingFocusValidPrefix() {

        String outputFolder = "EndPointFocusAndPrefix";

        String endpointPrefix = "/api/store";

        List<String> args = getArgsWithCompilation(
                40,
                outputFolder,
                ClassName.get("org.foo.BlackboxPrefixNonExistingFocusValidPrefix"),
                true);
        // program arguments for EvoMaster
        args.add("--blackBox");
        args.add("true");
        args.add("--bbTargetUrl");
        args.add(baseUrlOfSut);
        args.add("--bbSwaggerUrl");
        args.add(baseUrlOfSut + "/v2/api-docs");
        args.add("--endpointPrefix");
        args.add(endpointPrefix);

        // no endpointFocus or endpointPrefix is provided
        Solution<RestIndividual> solution = initAndRun(args);

        // include swagger into possible solutions as /v2/api-docs
        List<String> pathsToCheck = Arrays.asList(endpointPrefix, "/v2/api-docs");

        // if neither focus nor prefix is provided, then all paths should include empty path as a prefix
        assertAllSolutionsHavePathFocusOrPrefixList(solution, pathsToCheck, false);

        // The solution should include 5 solutions, 4 endpoints and 1 failure case
        assertEquals(solution.getIndividuals().size(), 5);

        compile(outputFolder);

    }

    @Test
    public void testRunBlackboxNonExistingFocusNonExistingPrefix() {

        String outputFolder = "EndPointFocusAndPrefix";
        String endpointPrefix = "/api/ab/s1";

        List<String> args = getArgsWithCompilation(
                40,
                outputFolder,
                ClassName.get("org.foo.BlackboxNonExistingFocusNonExistingPrefix"),
                true);
        // program arguments for EvoMaster
        args.add("--blackBox");
        args.add("true");
        args.add("--bbTargetUrl");
        args.add(baseUrlOfSut);
        args.add("--bbSwaggerUrl");
        args.add(baseUrlOfSut + "/v2/api-docs");
        args.add("--endpointPrefix");
        args.add(endpointPrefix);

        // check for IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> initAndRun(args));
    }

    @Test
    public void testRunBlackboxPrefixNonExistingPrefix() {

        String outputFolder = "EndPointFocusAndPrefix";
        String endpointPrefix = "/api/store/inventory/in";

        List<String> args = getArgsWithCompilation(
                40,
                outputFolder,
                ClassName.get("org.foo.BlackboxPrefixNonExistingPrefix"),
                true);
        // program arguments for EvoMaster
        args.add("--blackBox");
        args.add("true");
        args.add("--bbTargetUrl");
        args.add(baseUrlOfSut);
        args.add("--bbSwaggerUrl");
        args.add(baseUrlOfSut + "/v2/api-docs");
        args.add("--endpointPrefix");
        args.add(endpointPrefix);

        // check for IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> initAndRun(args));

    }

@Test
public void testRunBlackboxBothFocusAndPrefix() {

        String outputFolder = "EndPointFocusAndPrefix";
        String endpoint = "/api/store/order";

        List<String> args = getArgsWithCompilation(
                40,
                outputFolder,
                ClassName.get("org.foo.BlackboxBothFocusAndPrefix"),
                true);

        // program arguments for EvoMaster
        args.add("--blackBox");
        args.add("true");
        args.add("--bbTargetUrl");
        args.add(baseUrlOfSut);
        args.add("--bbSwaggerUrl");
        args.add(baseUrlOfSut + "/v2/api-docs");
        args.add("--endpointPrefix");
        args.add(endpoint);
        args.add("--endpointFocus");
        args.add(endpoint);

        // check for IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> initAndRun(args));

    }
}
