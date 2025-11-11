package org.evomaster.e2etests.spring.examples.adaptivehypermutation;

import com.foo.rest.examples.spring.adaptivehypermutation.AHypermutationRestController;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;

import java.util.List;

public class AHypermuationTestBase extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        SpringTestBase.initClass(new AHypermutationRestController(0));
    }


    int countExpectedCoveredTargets(Solution<RestIndividual> solution , List<String> msg){
        int count = 0;
        count = countExpected(solution, HttpVerb.POST, 200, "/api/foos/{x}", "B0", count, msg);
        count = countExpected(solution, HttpVerb.POST, 200, "/api/foos/{x}", "B1", count, msg);
        count = countExpected(solution, HttpVerb.POST, 200, "/api/foos/{x}", "B2", count, msg);
        count = countExpected(solution, HttpVerb.POST, 200, "/api/foos/{x}", "B3", count, msg);
        count = countExpected(solution, HttpVerb.POST, 200, "/api/foos/{x}", "B4", count, msg);
        count = countExpected(solution, HttpVerb.POST, 200, "/api/foos/{x}", "B5", count, msg);
        return count;
    }
}
