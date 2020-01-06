package org.evomaster.e2etests.spring.examples.resource;

import org.evomaster.core.problem.rest.*;
import org.evomaster.core.search.Action;
import org.evomaster.core.search.EvaluatedIndividual;
import org.evomaster.core.search.Solution;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * created by manzh on 2019-08-12
 */
public class ResourceDependencyEMTest extends ResourceTestBase {


    @Disabled("Started to fail since update to OpenApi V3")
    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "none",
                "none",
                1_000,
                false,
                (args) -> {
                    args.add("--heuristicsForSQL");
                    args.add("false");
                    args.add("--generateSqlDataWithSearch");
                    args.add("false");
                    args.add("--extractSqlExecutionInfo");
                    args.add("true");

                    args.add("--maxTestSize");
                    args.add("4");

                    args.add("--exportDependencies");
                    args.add("true");

                    String dependencies = "target/dependencyInfo/dependencies.csv";

                    args.add("--dependencyFile");
                    args.add(dependencies);

                    args.add("--resourceSampleStrategy");
                    args.add("EqualProbability");

                    args.add("--probOfSmartSampling");
                    args.add("1.0");
                    args.add("--doesApplyNameMatching");
                    args.add("false");

                    args.add("--probOfEnablingResourceDependencyHeuristics");
                    args.add("1.0");
                    args.add("--structureMutationProbability");
                    args.add("1.0");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assert(Files.exists(Paths.get(dependencies)));

                    boolean anyDBExecution = solution.getIndividuals().stream().anyMatch(
                            s -> s.getFitness().isAnyDatabaseExecutionInfo()
                    );
                    assertTrue(anyDBExecution);

                    boolean ok = solution.getIndividuals().stream().anyMatch(
                            s -> hasAtLeastOneSequence(s, new HttpVerb[]{HttpVerb.POST, HttpVerb.POST}, new int[]{201, 201}, new String[]{"/api/rd","/api/rpR"}) ||
                                    hasAtLeastOneSequence(s, new HttpVerb[]{HttpVerb.GET, HttpVerb.POST}, new int[]{200, 201}, new String[]{"/api/rd/{rdId}","/api/rpR"})

                    );

                    assertTrue(ok);
                }, 3);
    }


    protected boolean hasAtLeastOneSequence(EvaluatedIndividual<RestIndividual> ind,
                                            HttpVerb[] verbs,
                                            int[] expectedStatusCodes,
                                            String[] paths) {
        assertTrue(verbs.length == expectedStatusCodes.length);
        assertTrue(verbs.length == paths.length);

        boolean[] matched = new boolean[verbs.length];
        Arrays.fill(matched, false);
        List<RestAction> actions = ind.getIndividual().seeActions();

        Loop:
        for (int i = 0; i < actions.size(); i++) {
            RestAction action = actions.get(i);
            if (action instanceof RestCallAction){
                int index = getIndexOfFT(matched) + 1;
                if (index == matched.length) break Loop;
                if (((RestCallAction) action).getVerb() == verbs[index]
                        && ((RestCallAction) action).getPath().isEquivalent(new RestPath(paths[index]))
                        && ((RestCallResult) ind.getResults().get(i)).getStatusCode() == expectedStatusCodes[index]){
                    matched[index] = true;
                }
            }
        }

        return getIndexOfFT(matched) == (matched.length - 1);
    }

    private int getIndexOfFT(boolean[] matched){
        if (!matched[0]) return -1;
        for (int i = 0; i < matched.length - 1; i++){
            if(!matched[i+1]) return i;
        }
        return matched.length -1;
    }
}
