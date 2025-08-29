package org.evomaster.e2etests.spring.examples.blackbox;

import com.foo.rest.examples.spring.constant.ConstantController;
import org.evomaster.client.java.instrumentation.shared.ClassName;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BlackBoxConstantEMTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        SpringTestBase.initClass(new ConstantController());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testRunEM(boolean bbExperiments) throws Throwable {

        String outputFolder = "BlackBoxConstant";

        List<String> args = getArgsWithCompilation(
                10,
                outputFolder,
                ClassName.get("org.foo.BlackBoxConstant"),
                true);

        args.add("--blackBox");
        args.add("true");
        args.add("--bbTargetUrl");
        args.add(baseUrlOfSut);
        args.add("--bbSwaggerUrl");
        args.add(baseUrlOfSut+"/v2/api-docs");
        args.add("--bbExperiments");
        args.add("" + bbExperiments);

        /*
            Note: here we do not care of actual results.
            We just check that at least one test is generated,
            and that it can be compiled
         */
        Solution<RestIndividual> solution = initAndRun(args);

        assertTrue(solution.getIndividuals().size() >= 1);

        // call swagger is executed successfully
        assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/v2/api-docs","");

        compile(outputFolder);
    }
}