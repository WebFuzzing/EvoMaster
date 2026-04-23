package org.evomaster.e2etests.spring.graphql.blackbox;

import com.foo.graphql.base.BaseController;
import org.evomaster.client.java.instrumentation.shared.ClassName;
import org.evomaster.core.problem.graphql.GraphQLIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.graphql.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class GQLBlackBoxBaseEMTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        SpringTestBase.initClass(new BaseController());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testRunEM(boolean bbExperiments) throws Throwable {

        String outputFolder = "GQLBlackBoxBase";

        List<String> args = getArgsWithCompilation(
                10,
                outputFolder,
                ClassName.get("org.foo.BlackBoxConstant"),
                true);

        args.add("--problemType");
        args.add("GRAPHQL");
        args.add("--blackBox");
        args.add("true");
        args.add("--bbTargetUrl");
        args.add(baseUrlOfSut+"/graphql");
        args.add("--bbExperiments");
        args.add("" + bbExperiments);

        /*
            Note: here we do not care of actual results.
            We just check that at least one test is generated,
            and that it can be compiled
         */
        Solution<GraphQLIndividual> solution = initAndRun(args);

        assertTrue(solution.getIndividuals().size() >= 1);

        compile(outputFolder);
    }
}