package org.evomaster.client.java.controller.internal;

import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.api.dto.ActionDto;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.api.dto.WrappedResponseDto;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiActionDto;
import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiReplyDto;
import org.evomaster.client.java.controller.problem.AsyncApiProblem;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.instrumentation.InstrumentingAgent;
import org.evomaster.client.java.sql.DbSpecification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class EMControllerTest {

    private static final int OK = 200;

    private static final int SERVER_ERROR = 500;

    private static final long SETTLE_MS = 1_000;

    private static final long QUIET_MS = 100;

    /**
     * What the coverage objectives a fake SUT thread reaches are named after.
     */
    private static final String OBJECTIVE_PREFIX = "EMControllerTest_";

    /**
     * What happened, in order, across the driver and the controller's wait.
     */
    private final List<String> events = new ArrayList<>();

    private long now;

    private AsyncApiDriver driver;

    private EMController controller;

    /**
     * An embedded controller only reads the instrumentation once the agent is active. Unit tests
     * run without it, and only need the shared ExecutionTracer, so the flag is set by hand.
     */
    private static void setAgentActive(boolean active) {
        try {
            Field flag = InstrumentingAgent.class.getDeclaredField("active");
            flag.setAccessible(true);
            flag.setBoolean(null, active);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @BeforeEach
    public void activateTheAgent() {
        setAgentActive(true);
    }

    @AfterEach
    public void deactivateTheAgent() {
        setAgentActive(false);
    }

    @BeforeEach
    public void init() {
        events.clear();
        now = 0;
        driver = new AsyncApiDriver();
        driver.newTest();
        controller = new EMController(driver, new AsyncApiSettle(() -> now, ms -> {
            events.add("settle " + ms);
            now += ms;
        }));
    }

    @AfterEach
    public void tearDown() {
        //static state, shared with whatever runs next in this JVM
        ExecutionTracer.reset();
    }

    private Response publish(Long settleMs) {
        AsyncApiActionDto call = new AsyncApiActionDto();
        call.settleMs = settleMs;
        return publishCall(call);
    }

    private Response publishCall(AsyncApiActionDto call) {
        ActionDto dto = new ActionDto();
        dto.index = 0;
        dto.asyncApiCall = call;
        return controller.newAction(dto, false, mock(HttpServletRequest.class));
    }

    private static AsyncApiActionDto untilQuiet() {
        AsyncApiActionDto call = new AsyncApiActionDto();
        call.settleMs = SETTLE_MS;
        call.completion = AsyncApiActionDto.Completion.COVERAGE_QUIESCENCE;
        call.quietMs = QUIET_MS;
        return call;
    }

    private static AsyncApiReplyDto replyIn(Response response) {
        return (AsyncApiReplyDto) ((WrappedResponseDto<?>) response.getEntity()).data;
    }

    @Test
    public void testTheWaitStartsOnceTheDriverHasReturned() {

        Response response = publish(300L);

        assertEquals(OK, response.getStatus());
        assertEquals(2, events.size(), events.toString());
        assertEquals("publish", events.get(0));
        assertEquals("settle 300", events.get(1));

        AsyncApiReplyDto reply = replyIn(response);
        assertTrue(reply.published);
        assertEquals(300L, reply.settledMs);
        assertEquals(AsyncApiReplyDto.SettleEnd.DEADLINE, reply.settleEndedBy);
    }

    @Test
    public void testNoWaitIsDoneWhenNoneIsAskedFor() {

        Response response = publish(null);

        assertEquals(OK, response.getStatus());
        assertEquals(1, events.size(), events.toString());
        assertNull(replyIn(response).settledMs);
    }

    @Test
    public void testAMessageThatWasNotPublishedIsNotWaitedOn() {

        /*
            A broken setup, not a finding about the service: there is nothing it could be
            handling, so the test stops straight away rather than after a wait.
         */
        driver.fails = true;

        Response response = publish(300L);

        assertEquals(SERVER_ERROR, response.getStatus());
        assertEquals(1, events.size(), events.toString());

        AsyncApiReplyDto reply = replyIn(response);
        assertFalse(reply.published);
        assertNull(reply.settledMs);
    }

    @Test
    public void testAMessageTheDriverSaysWasNotPublishedIsNotWaitedOn() {

        /*
            A driver can also report a failed publish without throwing, or leave published unset.
            Either way there is nothing the service could be handling.
         */
        for (Boolean published : new Boolean[]{false, null}) {
            events.clear();
            driver = new AsyncApiDriver() {
                @Override
                public void executeAsyncApiAction(AsyncApiActionDto dto, AsyncApiReplyDto reply) {
                    events.add("publish");
                    reply.published = published;
                }
            };
            driver.newTest();
            controller = new EMController(driver, new AsyncApiSettle(() -> now, ms -> {
                events.add("settle " + ms);
                now += ms;
            }));

            Response response = publish(300L);

            assertEquals(OK, response.getStatus(), "published=" + published);
            assertEquals(1, events.size(), "published=" + published + ": " + events);

            AsyncApiReplyDto reply = replyIn(response);
            assertEquals(published, reply.published);
            assertNull(reply.settledMs, "published=" + published);
        }
    }

    @Test
    public void testTheServiceStillRecordsCoverageWhileTheActionIsKeptOpen() {

        /*
            The kill switch stops the SUT's threads from recording anything. It is turned off
            when the action starts, and has to stay off for the whole wait, or the wait would be
            for nothing.
         */
        ExecutionTracer.setKillSwitch(true);

        controller = new EMController(driver, new AsyncApiSettle(() -> now, ms -> {
            events.add("kill switch " + ExecutionTracer.isKillSwitch());
            now += ms;
        }));

        publish(300L);

        assertEquals("kill switch false", events.get(1));
    }

    @Test
    public void testASutThatReachesNothingNewIsLetGoOnceQuiet() {

        Response response = publishCall(untilQuiet());

        AsyncApiReplyDto reply = replyIn(response);
        assertEquals(AsyncApiReplyDto.SettleEnd.QUIESCENCE, reply.settleEndedBy);
        assertEquals(QUIET_MS, reply.settledMs);
    }

    @Test
    public void testWithoutTheAgentQuiescenceWaitsTheWholeWindow() {

        /*
            Nothing is instrumented, so the coverage never moves: that says nothing about whether
            the SUT is done, and must not end the wait early.
         */
        setAgentActive(false);

        Response response = publishCall(untilQuiet());

        AsyncApiReplyDto reply = replyIn(response);
        assertEquals(AsyncApiReplyDto.SettleEnd.DEADLINE, reply.settleEndedBy);
        assertEquals(SETTLE_MS, reply.settledMs);
    }

    @Test
    public void testASutStillReachingNewCodeIsWaitedOnUntilTheDeadline() {

        /*
            Stands in for the service's consumer thread, which keeps reaching new objectives
            while the controller waits.
         */
        controller = new EMController(driver, new AsyncApiSettle(() -> now, ms -> {
            now += ms;
            ExecutionTracer.executedNumericComparison(OBJECTIVE_PREFIX + now, 0.5, 1, 0.5);
        }));

        Response response = publishCall(untilQuiet());

        AsyncApiReplyDto reply = replyIn(response);
        assertEquals(AsyncApiReplyDto.SettleEnd.DEADLINE, reply.settleEndedBy);
        assertEquals(SETTLE_MS, reply.settledMs);
    }

    /**
     * A driver that publishes nothing, and says it did unless told to fail.
     */
    private class AsyncApiDriver extends EmbeddedSutController {

        private boolean fails = false;

        @Override
        public void executeAsyncApiAction(AsyncApiActionDto dto, AsyncApiReplyDto reply) {
            events.add("publish");
            if (fails) {
                throw new IllegalStateException("no broker");
            }
            reply.published = true;
            reply.replyExpected = false;
        }

        @Override
        public String startSut() {
            return null;
        }

        @Override
        public boolean isSutRunning() {
            return true;
        }

        @Override
        public void stopSut() {
        }

        @Override
        public String getPackagePrefixesToCover() {
            return null;
        }

        @Override
        public void resetStateOfSUT() {
        }

        @Override
        public List<AuthenticationDto> getInfoForAuthentication() {
            return null;
        }

        @Override
        public List<DbSpecification> getDbSpecifications() {
            return null;
        }

        @Override
        public ProblemInfo getProblemInfo() {
            return AsyncApiProblem.fromSchemaText("asyncapi: 3.0.0");
        }

        @Override
        public SutInfoDto.OutputFormat getPreferredOutputFormat() {
            return SutInfoDto.OutputFormat.JAVA_JUNIT_5;
        }
    }
}
