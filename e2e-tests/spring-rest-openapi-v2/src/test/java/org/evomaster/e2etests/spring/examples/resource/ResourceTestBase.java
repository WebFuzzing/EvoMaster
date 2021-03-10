package org.evomaster.e2etests.spring.examples.resource;

import com.foo.rest.examples.spring.resource.ResourceRestController;
import org.evomaster.core.problem.rest.*;
import org.evomaster.core.search.EvaluatedIndividual;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        assertEquals(expectedStatusCodes.length, verbs.length);
        assertEquals(paths.length, verbs.length);

        boolean[] matched = new boolean[verbs.length];
        Arrays.fill(matched, false);
        List<RestAction> actions = ind.getIndividual().seeRestAction();

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

    protected int getIndexOfFT(boolean[] matched){
        if (!matched[0]) return -1;
        for (int i = 0; i < matched.length - 1; i++){
            if(!matched[i+1]) return i;
        }
        return matched.length -1;
    }
}
