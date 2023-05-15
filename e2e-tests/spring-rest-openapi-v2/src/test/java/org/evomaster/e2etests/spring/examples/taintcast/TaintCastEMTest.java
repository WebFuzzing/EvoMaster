package org.evomaster.e2etests.spring.examples.taintcast;

import com.foo.rest.examples.spring.taintcast.TaintCastController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TaintCastEMTest extends SpringTestBase{

    @BeforeAll
    public static void initClass() throws Exception {

        TaintCastController taintCastController = new TaintCastController();
        EMConfig config = new EMConfig();
        config.setInstrumentMR_NET(true);
        SpringTestBase.initClass(taintCastController,config);
    }

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "TaintCastEM",
                "org.bar.TaintCastEM",
                1000,
                (args) -> {
                    args.add("--externalServiceIPSelectionStrategy");
                    args.add("USER");
                    args.add("--externalServiceIP");
                    args.add("127.0.0.2");
                    args.add("--instrumentMR_NET");
                    args.add("true");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/taint/cast","OK");
                });
    }
}
