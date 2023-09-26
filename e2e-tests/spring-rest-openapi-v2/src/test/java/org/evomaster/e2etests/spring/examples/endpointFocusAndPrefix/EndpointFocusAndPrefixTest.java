package org.evomaster.e2etests.spring.examples.endpointFocusAndPrefix;

import com.foo.rest.examples.spring.endpointFocusAndPrefix.EndpointFocusAndPrefixController;
import org.evomaster.client.java.instrumentation.shared.ClassName;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertThrows;


public class EndpointFocusAndPrefixTest extends SpringTestBase {

    @BeforeAll
    /*
     */
    public static void initClass() throws Exception {

        SpringTestBase.initClass(new EndpointFocusAndPrefixController());

    }

    @Test
    public void testRunBlackWithoutFocusOrPrefix() {

        String outputFolder = "EndPointFocusAndPrefix";

        List<String> args = getArgsWithCompilation(
                40,
                outputFolder,
                ClassName.get("org.foo.NoEndpointFocusNoEndpointPrefix"),
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

        List<String> pathsToCheck = Arrays.asList();

        // if neither focus nor prefix is provided, then all paths should include empty path as a prefix
        assertAllSolutionsHavePathFocusOrPrefixList(solution, pathsToCheck, false);

        compile(outputFolder);
    }

    @Test
    public void testRunBlackWithFocusNoPrefixTest1() {

        String outputFolder = "EndPointFocusAndPrefix";

        String endpointFocus = "/api/pet";

        List<String> args = getArgsWithCompilation(
                40,
                outputFolder,
                ClassName.get("org.foo.NoEndpointFocusNoEndpointPrefix"),
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

        compile(outputFolder);
    }


}
