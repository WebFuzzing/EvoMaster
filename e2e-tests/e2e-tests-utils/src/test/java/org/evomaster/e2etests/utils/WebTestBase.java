package org.evomaster.e2etests.utils;

import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.Main;
import org.evomaster.core.problem.webfrontend.WebAction;
import org.evomaster.core.problem.webfrontend.WebIndividual;
import org.evomaster.core.problem.webfrontend.WebResult;
import org.evomaster.core.search.EvaluatedIndividual;
import org.evomaster.core.search.Solution;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class WebTestBase extends EnterpriseTestBase{

    protected static void initClass(EmbeddedSutController controller) throws Exception {
        initClass(controller, new EMConfig());
    }

    protected Solution<WebIndividual> initAndRun(List<String> args){
        return (Solution<WebIndividual>) Main.initAndRun(args.toArray(new String[0]));
    }

    public static void assertHasVisitedUrlPath(Solution<WebIndividual> sol, String... paths){



        Set<String> visited = sol.getIndividuals().stream()
                .flatMap(ind -> {
                            List<WebAction> actions =  ind.getIndividual().seeMainExecutableActions();
                            return ind.seeResults(actions).stream();
                        })
                .filter(r -> r instanceof WebResult && !r.getStopping())
                .flatMap(r -> Arrays.asList(((WebResult) r).getUrlPageStart(), ((WebResult) r).getUrlPageEnd()).stream() )
                .map(url -> {
                    try {
                        return (new URL(url)).getPath();
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toSet());

        List<String> targets = Arrays.asList(paths);

        for(String p : targets){
            assertTrue(visited.contains(p), "Not visited page element '"+p+"'. Current: " + String.join("  ---  ", visited));
        }
    }
}
