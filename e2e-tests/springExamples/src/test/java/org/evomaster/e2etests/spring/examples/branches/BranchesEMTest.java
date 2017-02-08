package org.evomaster.e2etests.spring.examples.branches;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foo.rest.examples.spring.branches.BranchesController;
import com.foo.rest.examples.spring.branches.BranchesResponseDto;
import org.evomaster.core.Main;
import org.evomaster.core.problem.rest.RestCallResult;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BranchesEMTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        BranchesController controller = new BranchesController();
        SpringTestBase.initClass(controller);
    }

    @Disabled("Still issues to solve first")
    @Test
    public void testRunEM() {

        String[] args = new String[]{
                "--createTests", "false",
                "--seed", "42",
                "--sutControllerPort", "" + controllerPort,
                "--maxFitnessEvaluations", "200",
                "--stoppingCriterion", "FITNESS_EVALUATIONS"
        };

        Solution<RestIndividual> solution = (Solution<RestIndividual>) Main.initAndRun(args);

        assertTrue(solution.getIndividuals().size() >= 1);

        ObjectMapper mapper = new ObjectMapper();

        //get number of distinct response values
        long n = solution.getIndividuals().stream()
                .flatMap(i -> i.getResults().stream())
                .map(r -> r.getResultValue(RestCallResult.Companion.getBODY()))
                .filter(s -> s != null)
                .map(s -> {
                    try {
                        return mapper.readValue(s, BranchesResponseDto.class);
                    } catch (IOException e) {
                        return null;
                    }
                })
                .filter(b -> b != null)
                .mapToInt(b -> b.value)
                .distinct()
                .count();

        assertEquals(9, n);
    }
}