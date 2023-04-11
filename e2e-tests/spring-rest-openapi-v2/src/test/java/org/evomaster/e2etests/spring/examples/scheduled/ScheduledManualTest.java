package org.evomaster.e2etests.spring.examples.scheduled;


import com.foo.rest.examples.spring.scheduled.ScheduledApplication;
import com.foo.rest.examples.spring.scheduled.ScheduledRest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertTrue;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = ScheduledApplication.class)
public class ScheduledManualTest {

    @Autowired
    private ScheduledRest bean;

    @Test
    public void testScheduled() throws InterruptedException {

        Thread.sleep(10);

        boolean res = ScheduledRest.getValue();

        assertTrue(res);
    }
}
