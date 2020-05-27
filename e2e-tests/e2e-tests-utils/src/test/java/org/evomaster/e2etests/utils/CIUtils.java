package org.evomaster.e2etests.utils;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class CIUtils {

    public static boolean isRunningOnCircleCI(){
        String ci = System.getProperty("CI_env");
        return ci != null && ci.trim().toLowerCase().equals("circleci");
    }

    /**
     *  no idea why some tests are flaky on CircleCI, but perfectly fine on local and on Travis...
     *  as we measure coverage on Travis, then we just skip them if running on CircleCI
     */
    public static void skipIfOnCircleCI(){
        assumeTrue(!CIUtils.isRunningOnCircleCI());
    }
}
