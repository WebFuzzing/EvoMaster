package org.evomaster.e2etests.spring.examples.sort;

import org.evomaster.core.Main;
import org.evomaster.core.output.TestCase;
import org.evomaster.core.output.TestSuiteOrganizer;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.namedresource.NRTestBase;
import org.junit.jupiter.api.Test;

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

                    TestSuiteOrganizer organizer = new TestSuiteOrganizer();

                    List<TestCase> tclist = organizer.sortTests(solution, true);

                    Iterator<TestCase> iterator = tclist.iterator();
                    TestCase current, previous = iterator.next();
                    while(iterator.hasNext()){
                        current = iterator.next();
                        // Check that a TC with 500 in the name does not follow a TC without a 500 in the name (500 should be first).
                        if(current.getName().contains("500")){
                            assertTrue(previous.getName().contains("500"));
                        }
                        // Check that a TC with 400 in the name is preceded by another 400 or by a 500.
                        if(current.getName().contains("400")){
                            assertTrue(previous.getName().contains("400") || previous.getName().contains("500"));
                        }

                        //Check that neighbouring TC with the same code are sorted by number of actions
                        if((current.getName().contains("500") && previous.getName().contains("500")) ||
                                (current.getName().contains("400") && previous.getName().contains("400")) ||
                                (current.getName().contains("200") && previous.getName().contains("200"))
                        ) {
                            assertTrue(current.getTest().evaluatedActions().size() >= previous.getTest().evaluatedActions().size());
                        }

                }

                });
    }

}
