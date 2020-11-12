package org.evomaster.e2etests.spring.examples.sort;

import org.evomaster.core.Main;
import org.evomaster.core.output.TestCase;
import org.evomaster.core.output.TestSuiteOrganizer;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestCallResult;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.EvaluatedIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.namedresource.NRTestBase;
import org.junit.jupiter.api.Test;

import java.util.OptionalInt;
import java.util.function.Predicate;

import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Note: This class uses the NRTestBase and NamedResourceRest SUT. This is more of a shortcut, since that system seems to
 * have the diversity of test cases needed.
 */

public class SortEMTest extends NRTestBase {

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "SortEM",
                "org.bar.SortEM",
                3_000,
                (args) -> {

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    //check it has some 500s?
                    assertHasAtLeastOne(solution, HttpVerb.PATCH, 500);
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 500);
                    assertHasAtLeastOne(solution, HttpVerb.POST, 500);

                    TestSuiteOrganizer organizer = new TestSuiteOrganizer();

                    List<TestCase> tclist = organizer.sortTests(solution, true);

                    //Iterator<TestCase> iterator = tclist.iterator();
                    //TestCase current, previous = iterator.next();
                    /*
                    while(iterator.hasNext()){
                        current = iterator.next();
                        // Check that a TC with 500 in the name does not follow a TC without a 500 in the name (500 should be first).

                        if(current.getName().contains("500")){
                            assertTrue(previous.getName().contains("500"));
                        }
                        previous = current;
                     }
                     */

                    Iterator<EvaluatedIndividual<RestIndividual>> iterator = solution.getIndividuals().iterator();
                    EvaluatedIndividual<RestIndividual> current, previous = iterator.next();



                    while(iterator.hasNext()){
                        current = iterator.next();

                        if (current.getResults().stream()
                                .filter(w -> w instanceof RestCallResult)
                                .anyMatch(r -> ((RestCallResult) r).getStatusCode() == 500)) {

                            assertTrue(previous.getResults().stream()
                                    .filter(w -> w instanceof RestCallResult)
                                    .anyMatch(r -> ((RestCallResult) r).getStatusCode() == 500));
                        }



                        // Check that the current "priority code" is less than the previous priority code

                        OptionalInt currentPrioCode = current.getResults().stream()
                                .filter(w -> w instanceof RestCallResult)
                                .mapToInt(w -> ((RestCallResult) w).getStatusCode())
                                .map(w -> w % 500)
                                .min();

                        OptionalInt previousPrioCode = previous.getResults().stream()
                                .filter(w -> w instanceof RestCallResult)
                                .mapToInt(w -> ((RestCallResult) w).getStatusCode())
                                .map(w -> w % 500)
                                .min();

                        assertTrue(currentPrioCode.getAsInt() >= previousPrioCode.getAsInt());
                        previous = current;

                }

                });
    }

}
