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
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertThrows;


public class EndpointFocusAndPrefixTest extends SpringTestBase {

    private final String swaggerURLSample = "https://petstore.swagger.io/v2/swagger.json";


    @BeforeAll
    /* An empty method since the test is going to use petstore API on
       So not initiating controller for this test
     */
    public static void initClass() throws Exception {

        SpringTestBase.initClass(new EndpointFocusAndPrefixController());

    }

    /*
    Black box test with existing endpointFocus
     */
    @Test
    public void testRunBlackBoxWithFocus() {

        String outputFolder = "EndpointFocus";
        String endpointFocus = "/pet";

        List<String> args = getArgsWithCompilation(
                10,
                outputFolder,
                ClassName.get("org.foo.EndpointFocus"),
                true);
        // program arguments for EvoMaster
        args.add("--blackBox");
        args.add("true");
        args.add("--bbSwaggerUrl");
        args.add(swaggerURLSample);
        // endpointFocus
        args.add("--endpointFocus");
        args.add(endpointFocus);

        Solution<RestIndividual> solution = initAndRun(args);

        String pathToSearch = "/v2" + endpointFocus;

        assertAllSolutionsHavePathFocus(solution, pathToSearch);

        compile(outputFolder);

    }

    /*
    If an endpointFocus which does not exist in the swagger is provided,
    the program has to throw an IllegalArgumentException and stop
     */
    @Test
    public void testRunBlackBoxWithPrefix() {

        String outputFolder = "EndpointPrefix";
        String endpointPrefix = "/pet";

        List<String> args = getArgsWithCompilation(
                10,
                outputFolder,
                ClassName.get("org.foo.EndpointPrefix"),
                true);
        // program arguments for EvoMaster
        args.add("--blackBox");
        args.add("true");
        args.add("--bbSwaggerUrl");
        args.add(swaggerURLSample);
        // endpointFocus
        args.add("--endpointPrefix");
        args.add(endpointPrefix);


        Solution<RestIndividual> solution = initAndRun(args);


        String pathToSearch = "/v2" + endpointPrefix;

        assertAllSolutionsHavePathPrefix(solution, pathToSearch);

        compile(outputFolder);

    }

    @Test
    void testNonExistingFocus() {

        String a = null;

        String outputFolder = "EndpointFocus";
        String endpointFocusNonExisting = "/nopet";

        List<String> args = getArgsWithCompilation(
                10,
                outputFolder,
                ClassName.get("org.foo.EndpointFocus"),
                true);

        // program arguments for EvoMaster
        args.add("--blackBox");
        args.add("true");
        args.add("--bbSwaggerUrl");
        args.add(swaggerURLSample);
        // endpointFocus
        args.add("--endpointFocus");
        args.add(endpointFocusNonExisting);

        assertThrows(InvocationTargetException.class,
                ()-> {
                    // This run should throw exception
                    initAndRun(args);
                });


    }

    @Test
    void testNonExistingPrefix() {

        String a = null;

        String outputFolder = "EndpointFocus";
        String endpointPrefixNonExisting = "/nopet";

        List<String> args = getArgsWithCompilation(
                10,
                outputFolder,
                ClassName.get("org.foo.EndpointFocus"),
                true);

        // program arguments for EvoMaster
        args.add("--blackBox");
        args.add("true");
        args.add("--bbSwaggerUrl");
        args.add(swaggerURLSample);
        // endpointFocus
        args.add("--endpointPrefix");
        args.add(endpointPrefixNonExisting);

        assertThrows(IllegalArgumentException.class,
                ()-> {
                    // This run should throw exception
                    initAndRun(args);
                });


    }

    @Test
    void testBothFocusAndPrefixProvided() {

        String a = null;

        String outputFolder = "EndpointFocus";
        String endpointPrefix = "/pet";
        String endpointFocus = "/user";

        List<String> args = getArgsWithCompilation(
                10,
                outputFolder,
                ClassName.get("org.foo.EndpointFocus"),
                true);

        // program arguments for EvoMaster
        args.add("--blackBox");
        args.add("true");
        args.add("--bbSwaggerUrl");
        args.add(swaggerURLSample);
        // endpointFocus
        args.add("--endpointPrefix");
        args.add(endpointPrefix);
        args.add("--endpointFocus");
        args.add(endpointFocus);

        assertThrows(IllegalArgumentException.class,
                ()-> {
                    // This run should throw exception
                    initAndRun(args);
                });


    }
}
