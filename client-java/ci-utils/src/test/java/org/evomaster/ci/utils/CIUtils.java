package org.evomaster.ci.utils;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class CIUtils {

    public static void skipIfOnWindows(){
        String os = System.getProperty("os.name");
        assumeTrue(! os.toLowerCase().contains("window"));
    }


    public static void skipIfOnLinux(){
        String os = System.getProperty("os.name");
        assumeTrue(! os.toLowerCase().contains("linux"));
    }

    public static void skipIfOnLinuxOnGA(){
        String os = System.getProperty("os.name");
        assumeTrue(! os.toLowerCase().contains("linux") || !isRunningGA());
    }



    public static boolean isRunningGA(){
        String ci = System.getenv("CI_env");
        return ci != null && ci.trim().equalsIgnoreCase("githubaction");
    }

    /**
     * some tests pass locally on Mac, but fail on GA
     */
    public static void skipIfOnGA(){
        assumeTrue(!CIUtils.isRunningGA());
    }


    public static boolean isRunningOnCircleCI(){
        String ci = System.getenv("CI_env");
        return ci != null && ci.trim().toLowerCase().equals("circleci");
    }

    /**
     *  no idea why some tests are flaky on CircleCI, but perfectly fine on local and on Travis...
     *  as we measure coverage on Travis, then we just skip them if running on CircleCI
     */
    public static void skipIfOnCircleCI(){
        assumeTrue(!CIUtils.isRunningOnCircleCI());
    }


    public static boolean isRunningTravis(){
        String ci = System.getenv("CI_env");
        return ci != null && ci.trim().toLowerCase().equals("travis");
    }

    /**
     *  Some tests pass locally on Mac, Windows and also on CircleCI, but then fail on
     *  Travis... seen this when dealing with spawn processes.
     *  This might be an issue, as we measure coverage on Travis, not CircleCI
     */
    public static void skipIfOnTravis(){
        assumeTrue(!CIUtils.isRunningTravis());
    }
}
