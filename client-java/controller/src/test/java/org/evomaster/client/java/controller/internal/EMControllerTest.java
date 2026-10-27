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
import org.evomaster.client.java.controller.api.dto.AdditionalInfoDto;
import org.evomaster.client.java.controller.api.dto.EmittedMessageDto;
import org.evomaster.client.java.controller.api.dto.TestResultsDto;
import org.evomaster.client.java.instrumentation.EmittedMessage;
import org.evomaster.client.java.instrumentation.staticstate.AsyncMessageTracker;
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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class EMControllerTest {

    private static final int OK = 200;

    private static final int SERVER_ERROR = 500;

    private static final long SETTLE_MS = 1_000;

    private static final long STABLE_MS = 100;

    /**
     * What the coverage objectives a fake SUT thread reaches are named after.
     */
    private static final String OBJECTIVE_PREFIX = "EMControllerTest_";

    /**
     * Where the actions published here go, and the SUT consumes from.
     */
    private static final String ADDRESS = "EMControllerTest_requests";

    private static final String PAYLOAD = "{\"result\":42}";

    private static final long CONSUMER_TIMEOUT_SECONDS = 10;

    /**
     * What happened, in order, across the driver and the controller's wait.
     */
    private final List<String> events = new ArrayList<>();

    private long now;

    private AsyncApiDriver driver;

    /**
     * Stands in for the SUT's consumer thread: what it receives, and whether it is done with it,
     * is tracked per thread.
     */
    private ExecutorService consumer;

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
        consumer = Executors.newSingleThreadExecutor();
        controller = new EMController(driver, new AsyncApiSettle(() -> now, ms -> {
            events.add("settle " + ms);
            now += ms;
        }));
    }

    @AfterEach
    public void tearDown() {
        //static state, shared with whatever runs next in this JVM
        ExecutionTracer.reset();
        consumer.shutdownNow();
    }

    /**
     * Run on the stand-in for the SUT's consumer thread, and wait for it.
     */
    private void onTheConsumerThread(Runnable runnable) {
        try {
            consumer.submit(runnable).get(CONSUMER_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new IllegalStateException(e);
        }
    }

    private static AsyncApiActionDto untilHandled() {
        AsyncApiActionDto call = new AsyncApiActionDto();
        call.settleMs = SETTLE_MS;
        call.completion = AsyncApiActionDto.MessageCompletionStrategy.HANDLER_RETURN;
        call.address = ADDRESS;
        return call;
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

    private static AsyncApiActionDto untilCoverageStable() {
        AsyncApiActionDto call = new AsyncApiActionDto();
        call.settleMs = SETTLE_MS;
        call.completion = AsyncApiActionDto.MessageCompletionStrategy.COVERAGE_STABLE;
        call.stableMs = STABLE_MS;
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
        assertEquals(AsyncApiReplyDto.SettleEndReason.DEADLINE, reply.settleEndedBy);
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
    public void testASutThatReachesNothingNewIsLetGoOnceCoverageIsStable() {

        Response response = publishCall(untilCoverageStable());

        AsyncApiReplyDto reply = replyIn(response);
        assertEquals(AsyncApiReplyDto.SettleEndReason.COVERAGE_STABLE, reply.settleEndedBy);
        assertEquals(STABLE_MS, reply.settledMs);
    }

    @Test
    public void testWithoutTheAgentCoverageStableWaitsTheWholeWindow() {

        /*
            Nothing is instrumented, so the coverage never moves: that says nothing about whether
            the SUT is done, and must not end the wait early.
         */
        setAgentActive(false);

        Response response = publishCall(untilCoverageStable());

        AsyncApiReplyDto reply = replyIn(response);
        assertEquals(AsyncApiReplyDto.SettleEndReason.DEADLINE, reply.settleEndedBy);
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

        Response response = publishCall(untilCoverageStable());

        AsyncApiReplyDto reply = replyIn(response);
        assertEquals(AsyncApiReplyDto.SettleEndReason.DEADLINE, reply.settleEndedBy);
        assertEquals(SETTLE_MS, reply.settledMs);
    }

    @Test
    public void testTheDriverRunsMarkedAsTheDriver() {

        Response response = publish(null);

        assertEquals(OK, response.getStatus());
        assertEquals(true, driver.markedAsDriver, "the driver published unmarked");
        assertFalse(ExecutionTracer.isExecutingDriverAction(), "the mark outlived the driver");
    }

    @Test
    public void testTheDriverMarkIsClearedWhenTheDriverFails() {

        driver.fails = true;

        Response response = publish(null);

        assertEquals(SERVER_ERROR, response.getStatus());
        assertEquals(true, driver.markedAsDriver, "the driver published unmarked");
        assertFalse(ExecutionTracer.isExecutingDriverAction(), "the mark outlived the failed driver");
    }

    @Test
    public void testASutDoneWithTheMessageIsLetGo() {

        //the SUT takes the message while the driver is still publishing
        driver.onPublish = () -> onTheConsumerThread(() -> AsyncMessageTracker.received(Collections.singletonList(ADDRESS)));

        //and goes back for more while the controller waits
        controller = new EMController(driver, new AsyncApiSettle(() -> now, ms -> {
            now += ms;
            onTheConsumerThread(() -> AsyncMessageTracker.askingForMore(true));
        }));

        Response response = publishCall(untilHandled());

        AsyncApiReplyDto reply = replyIn(response);
        assertEquals(AsyncApiReplyDto.SettleEndReason.HANDLER_RETURN, reply.settleEndedBy);
        assertEquals(AsyncApiSettle.POLL_INTERVAL_MS, reply.settledMs);
    }

    @Test
    public void testWithoutTheAgentHandlerReturnWaitsTheWholeWindow() {

        //what the tracker holds is not the SUT's when nothing is instrumented
        setAgentActive(false);
        driver.onPublish = () -> onTheConsumerThread(() -> {
            AsyncMessageTracker.received(Collections.singletonList(ADDRESS));
            AsyncMessageTracker.askingForMore(true);
        });

        Response response = publishCall(untilHandled());

        AsyncApiReplyDto reply = replyIn(response);
        assertEquals(AsyncApiReplyDto.SettleEndReason.DEADLINE, reply.settleEndedBy);
        assertEquals(SETTLE_MS, reply.settledMs);
    }

    @Test
    public void testASutThatNeverTakesTheMessageIsWaitedOnUntilTheDeadline() {

        Response response = publishCall(untilHandled());

        AsyncApiReplyDto reply = replyIn(response);
        assertEquals(AsyncApiReplyDto.SettleEndReason.DEADLINE, reply.settleEndedBy);
        assertEquals(SETTLE_MS, reply.settledMs);
    }

    @Test
    public void testWhatTheSutEmittedAndConsumedReachesTheTestResults() {

        ExecutionTracer.addEmittedMessage(new EmittedMessage(ADDRESS, PAYLOAD, String.class.getName()));

        Response response = controller.getTestResultsWithIdsInBody(
                Collections.emptyList(), false, false, false, false, mock(HttpServletRequest.class));

        assertEquals(OK, response.getStatus());
        TestResultsDto results = (TestResultsDto) ((WrappedResponseDto<?>) response.getEntity()).data;
        AdditionalInfoDto info = results.additionalInfoList.get(0);

        assertEquals(1, info.emittedMessages.size());
        EmittedMessageDto message = info.emittedMessages.get(0);
        assertEquals(ADDRESS, message.address);
        assertEquals(PAYLOAD, message.payload);
        assertEquals(false, message.payloadTruncated);
        assertEquals(String.class.getName(), message.payloadType);
    }

    /**
     * A driver that publishes nothing, and says it did unless told to fail.
     */
    private class AsyncApiDriver extends EmbeddedSutController {

        private boolean fails = false;

        /**
         * What the SUT does while the driver publishes.
         */
        private Runnable onPublish = () -> {};

        /**
         * Whether the thread publishing was marked as running the driver, when it published.
         */
        private Boolean markedAsDriver = null;

        @Override
        public void executeAsyncApiAction(AsyncApiActionDto dto, AsyncApiReplyDto reply) {
            events.add("publish");
            markedAsDriver = ExecutionTracer.isExecutingDriverAction();
            onPublish.run();
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
