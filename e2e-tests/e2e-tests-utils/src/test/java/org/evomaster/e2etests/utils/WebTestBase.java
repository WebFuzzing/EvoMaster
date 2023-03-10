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

    public static void assertNoHtmlErrors(Solution<WebIndividual> sol){

        assertTrue(
                sol.getIndividuals().stream()
                        .flatMap(ind -> {
                            List<WebAction> actions =  ind.getIndividual().seeMainExecutableActions();
                            return ind.seeResults(actions).stream();
                        })
                        .allMatch(r -> !Boolean.FALSE.equals(((WebResult)r).getValidHtml()))
        );
    }

    public static void assertHasAnyHtmlErrors(Solution<WebIndividual> sol){

        assertTrue(
                sol.getIndividuals().stream()
                        .flatMap(ind -> {
                            List<WebAction> actions =  ind.getIndividual().seeMainExecutableActions();
                            return ind.seeResults(actions).stream();
                        })
                        .anyMatch(r -> Boolean.FALSE.equals(((WebResult)r).getValidHtml()))
        );
    }

    public static Set<URL> visitedUrls(Solution<WebIndividual> sol){

        return sol.getIndividuals().stream()
                .flatMap(ind -> {
                    List<WebAction> actions =  ind.getIndividual().seeMainExecutableActions();
                    return ind.seeResults(actions).stream();
                })
                .filter(r -> r instanceof WebResult)
                .flatMap(r ->
                        !r.getStopping()
                                ? Arrays.asList(((WebResult) r).getUrlPageStart(), ((WebResult) r).getUrlPageEnd()).stream()
                                : Arrays.asList(((WebResult) r).getUrlPageStart()).stream()
                )
                .map(url -> {
                    try {
                        return (new URL(url));
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toSet());
    }

    public static void assertHasVisitedUrlPath(Solution<WebIndividual> sol, String... paths){

        Set<String> visited = visitedUrls(sol).stream()
                .map(url -> {
                        return url.getPath();
                })
                .collect(Collectors.toSet());

        List<String> targets = Arrays.asList(paths);

        for(String p : targets){
            assertTrue(visited.contains(p), "Not visited page element '"+p+"'. Current: " + String.join("  ---  ", visited));
        }
    }
}
