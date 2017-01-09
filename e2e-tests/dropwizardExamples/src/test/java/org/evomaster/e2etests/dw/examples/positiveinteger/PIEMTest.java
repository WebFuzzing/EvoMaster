package org.evomaster.e2etests.dw.examples.positiveinteger;

import org.evomaster.core.Main;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PIEMTest extends PITestBase {

    @Disabled
    @Test
    public void testRunEM(){

        String[] args = new String[]{
                "--seed", "42",
                "--sutControllerPort", "" + controllerPort
        };

        Solution<RestIndividual> solution = (Solution<RestIndividual>) Main.initAndRun(args);

        assertTrue(solution.getIndividuals().size() >= 1);
        //TODO check fitness
    }
}
