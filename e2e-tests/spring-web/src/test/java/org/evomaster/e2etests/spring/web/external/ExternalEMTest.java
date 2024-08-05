package org.evomaster.e2etests.spring.web.external;

import com.foo.web.examples.spring.external.ExternalController;
import org.evomaster.test.utils.SeleniumEMUtils;
import org.evomaster.core.problem.webfrontend.WebIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.web.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExternalEMTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        SpringTestBase.initClass(new ExternalController());
    }

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "ExternalEM",
                "org.ExternalEM",
                50,
                (args) -> {

                    Solution<WebIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() > 0);

                    assertHasVisitedUrlPath(solution, "/external/index.html", "/external/a.html");

                    Set<URL> urls = visitedUrls(solution);
                    Set<String> hosts = urls.stream().map(u -> u.getHost()).collect(Collectors.toSet());
                    assertEquals(1, hosts.size());
                    String host = hosts.stream().findFirst().get();
                    assertEquals(SeleniumEMUtils.TESTCONTAINERS_HOST, host);
                }
        );
    }
}
