package org.evomaster.e2etests.spring.examples.securitytest;

import com.foo.rest.examples.spring.formlogin.FormLoginController;
import com.foo.rest.examples.spring.securitytest.SecurityTestController;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SecurityTestEMTest extends SpringTestBase {


    @BeforeAll
    public static void initClass() throws Exception {

        SpringTestBase.initClass(new SecurityTestController());
    }

    @Test
    public void testRunEM() throws Throwable {


        List<String> args = new ArrayList<String>();

        args.add("--security");
        args.add("true");

        Solution<RestIndividual> solution = initAndRun(args);

        /*
        runTestHandlingFlakyAndCompilation(
                "SecurityTestEM",
                "org.bar.SecurityTestEM",
                1,
                (args) -> {

                    args.add("--security");
                    args.add("true");

                    Solution<RestIndividual> solution = initAndRun(args);

                    //assertTrue(solution.getIndividuals().size() >= 1);

                    //assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/formlogin/openToAll", "openToAll");
                    //assertHasAtLeastOne(solution, HttpVerb.GET, 401, "/api/formlogin/forUsers", null);
                    //assertHasAtLeastOne(solution, HttpVerb.GET, 401, "/api/formlogin/forAdmins", null);
                    //assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/formlogin/forUsers", "forUsers");
                    //assertHasAtLeastOne(solution, HttpVerb.GET, 403, "/api/formlogin/forAdmins", null);
                    //assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/formlogin/forAdmins", "forAdmins");

                });

         */
    }
}
