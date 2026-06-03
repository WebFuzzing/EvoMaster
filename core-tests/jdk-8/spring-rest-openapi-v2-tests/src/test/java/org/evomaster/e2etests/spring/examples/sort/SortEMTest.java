package org.evomaster.e2etests.spring.examples.sort;


import org.evomaster.core.output.naming.NamingStrategy;
import org.evomaster.core.output.naming.NumberedTestCaseNamingStrategy;
import org.evomaster.core.output.naming.TestCaseNamingStrategy;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestCallResult;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.EvaluatedIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.namedresource.NRTestBase;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Note: This class uses the NRTestBase and NamedResourceRest SUT. This is more of a shortcut, since that system seems to
 * have the diversity of test cases needed.
 */

public class SortEMTest extends NRTestBase {

    @Test
    public void testRunEM() throws Throwable {

        String outputFolderName = "SortEM";
        String className = "org.bar.SortEM";

        runTestHandlingFlakyAndCompilation(
                outputFolderName,
                className,
                3_000,
                (args) -> {

                    setOption(args, "namingStrategy", NamingStrategy.DETERMINISTIC.name());

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    //check it has some 500s?
                    assertHasAtLeastOne(solution, HttpVerb.PATCH, 500);
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 500);
                    assertHasAtLeastOne(solution, HttpVerb.POST, 500);

                    List<String> functionNames = getFunctionNames(outputFolderName, className);
                    // 3 initializations (eg @BeforEach), plus 1 per individuals
                    int expected = 3 + solution.getIndividuals().size();
                    assertEquals(expected, functionNames.size());

                    String prefix = NumberedTestCaseNamingStrategy.TEST_NAME_PREFIX;
                    List<String> testNames = functionNames.stream()
                            .filter(it -> it.startsWith(prefix))
                            .collect(Collectors.toList());
                    assertEquals(solution.getIndividuals().size(), testNames.size());

                    for(int i=0; i < testNames.size(); i++){
                        String name = testNames.get(i);
                        int index = Integer.parseInt(
                                name.substring(prefix.length(), name.indexOf("_", prefix.length() + 1))
                        );
                        assertEquals(i, index,
                                "Wrong numbering." +
                                        " Expected: " + i + ", but found " + index + " for test name " + name);
                    }
                });
    }

}
