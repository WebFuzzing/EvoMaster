package org.evomaster.e2etests.dw.examples.positiveinteger;

import org.evomaster.core.Main;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestCallResult;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.EvaluatedIndividual;
import org.evomaster.core.search.Individual;
import org.evomaster.core.search.Solution;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PIEMTest extends PITestBase {

    private boolean hasAtLeastOne(EvaluatedIndividual<RestIndividual> ind,
                                  HttpVerb verb,
                                  int expectedStatusCode){

        List<Integer> index = ind.getIndividual().getIndexOfHttpCalls(verb);
        for(int i : index){
            String statusCode = ind.getResults().get(i).getResultValue(
                    RestCallResult.Companion.getSTATUS_CODE());
            if(statusCode.equals(""+expectedStatusCode)){
                return true;
            }
        }
        return false;
    }

    @Test
    public void testRunEM(){

        String[] args = new String[]{
                "--createTests", "false",
                "--seed", "42",
                "--sutControllerPort", "" + controllerPort,
                "--maxFitnessEvaluations", "20"
        };

        Solution<RestIndividual> solution = (Solution<RestIndividual>) Main.initAndRun(args);

        assertTrue(solution.getIndividuals().size() >= 1);

        assertTrue(solution.getIndividuals().stream().anyMatch(
                ind -> hasAtLeastOne(ind, HttpVerb.GET, 200)));

        assertTrue(solution.getIndividuals().stream().anyMatch(
                ind -> hasAtLeastOne(ind, HttpVerb.POST, 200)));
    }


    @Test
    public void testCreateTest() {

        String[] args = new String[]{
                "--createTests", "true",
                "--seed", "42",
                "--sutControllerPort", "" + controllerPort,
                "--maxFitnessEvaluations", "20"
        };

       Main.initAndRun(args);

       //TODO check file
    }
}
