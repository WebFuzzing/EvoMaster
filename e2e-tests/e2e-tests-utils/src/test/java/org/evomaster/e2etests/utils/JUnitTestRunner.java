package org.evomaster.e2etests.utils;

import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;


import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Created by arcuri82 on 04-Apr-19.
 */
public class JUnitTestRunner {

    public static TestExecutionSummary runTestsInClass(Class<?> klass){

        SummaryGeneratingListener listener = new SummaryGeneratingListener();

        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClass(klass))
                .build();
        Launcher launcher = LauncherFactory.create();
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);

        return listener.getSummary();
    }
}
