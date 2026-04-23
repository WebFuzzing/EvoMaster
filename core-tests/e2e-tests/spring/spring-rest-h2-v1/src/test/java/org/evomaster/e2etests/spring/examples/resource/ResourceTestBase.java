package org.evomaster.e2etests.spring.examples.resource;

import com.foo.rest.examples.spring.resource.ResourceRestController;
import org.evomaster.core.problem.rest.data.*;
import org.evomaster.core.search.action.ActionResult;
import org.evomaster.core.search.EvaluatedIndividual;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * created by manzh on 2019-08-12
 */
public class ResourceTestBase extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        SpringTestBase.initClass(new ResourceRestController());
    }

    protected boolean hasAtLeastOneSequence(EvaluatedIndividual<RestIndividual> ind,
                                            HttpVerb[] verbs,
                                            int[] expectedStatusCodes,
                                            String[] paths) {
        assertEquals(verbs.length, expectedStatusCodes.length);
        assertEquals(verbs.length, paths.length);

        boolean[] matched = new boolean[verbs.length];
        Arrays.fill(matched, false);
        List<RestCallAction> actions = ind.getIndividual().seeMainExecutableActions();
        List<ActionResult> actionResults = ind.seeResults(actions);
        Loop:
        for (int i = 0; i < actions.size(); i++) {
            RestCallAction action = actions.get(i);
            int index = getIndexOfFT(matched) + 1;
            if (index == matched.length) break Loop;
            if (action.getVerb() == verbs[index]
                    && action.getPath().isEquivalent(new RestPath(paths[index]))
                    && ((RestCallResult) actionResults.get(i)).getStatusCode() == expectedStatusCodes[index]){
                matched[index] = true;
            }
        }

        return getIndexOfFT(matched) == (matched.length - 1);
    }

    protected int getIndexOfFT(boolean[] matched){
        if (!matched[0]) return -1;
        for (int i = 0; i < matched.length - 1; i++){
            if(!matched[i+1]) return i;
        }
        return matched.length -1;
    }
}
