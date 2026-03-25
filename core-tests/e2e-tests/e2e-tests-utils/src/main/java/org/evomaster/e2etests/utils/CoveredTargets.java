package org.evomaster.e2etests.utils;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * During the search, we can evaluate what EvoMaster can cover based on the HTTP responses we get (eg for APIs).
 * For example, for each endpoint in REST, we can have assertions like assertHasAtLeastOne, to see which status code
 * and response body are returned.
 *
 * This works during the search, but NOT for checking if the generated tests actually can cover these same things.
 * In the E2E tests, we automatically compile and run the generated tests, and, if any fails, then the E2E test fails.
 * But what if due to bugs only few tests are generated and some are skipped?
 * Or if tests are there, but they execute no HTTP call at all and have no assertion?
 * Unfortunately, we would not be able to see these problems.
 *
 * The solution is to use this class CoveredTargets, directly in the source code of the tested APIs, to mark coverage targets.
 * These are saved in a global static state (which needs to be reset at each test execution, which is done automatically
 * in our E2E scaffolding).
 * After the search, but before executing the generated tests, we reset the state.
 * Once the generated tests are executed, we can check this global state to see if all expected targets have been covered
 * by the executed tests.
 *
 * A nice property here is that, although the API is run in the same JVM of the JUnit of the E2E, so that we can easily
 * access the global static state directly, the generated tests do not need to.
 * For example, they could be run in a separated process, like what we need to do when running tests generated in
 * Python and JavaScript.
 * This will work just fine ;-)
 *
 * Adding CoveredTargets in E2E, and verify them, is a bit of work, though.
 * So, we don't do it for all E2E tests.
 * For example, we don't for white-box tests, and just do it for black-box (at least at this point in time).
 * The reason is that there we check different programming language outputs (eg Python and JS), which we do not in WB.
 *
 * However, even in BB tests where we use CoveredTargets, we still do want to have assertions like assertHasAtLeastOne
 * on the HTTP responses of the archive generated during the search, before outputting test files.
 * The reason is that, if there are issues during the search, we can find those earlier, before we compile and run the generated
 * tests. It makes debugging easier (eg, you would know the issue is during the search, and not on how the test files are
 * generated).
 */
public class CoveredTargets {

    private static final Set<String> coveredTargets = new CopyOnWriteArraySet<>();

    public static void reset(){
        coveredTargets.clear();
    }

    public static void cover(String target){
        coveredTargets.add(target);
    }

    public static boolean isCovered(String target){
        return coveredTargets.contains(target);
    }

    public static boolean areCovered(Collection<String> targets) {
        return coveredTargets.containsAll(targets);
    }

    public static int numberOfCoveredTargets(){
        return coveredTargets.size();
    }
}
