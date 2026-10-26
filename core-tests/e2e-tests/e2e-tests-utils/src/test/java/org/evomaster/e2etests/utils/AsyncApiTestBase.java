package org.evomaster.e2etests.utils;

import com.webfuzzing.commons.faults.FaultCategory;
import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiReplyDto;
import org.evomaster.core.Main;
import org.evomaster.core.problem.asyncapi.data.AsyncApiCallResult;
import org.evomaster.core.problem.asyncapi.data.AsyncApiIndividual;
import org.evomaster.core.problem.asyncapi.data.AsyncApiOutcome;
import org.evomaster.core.problem.enterprise.DetectedFault;
import org.evomaster.core.search.Solution;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What an E2E test over an AsyncAPI service needs: to run the search, and to read what
 * publishing to each operation was seen to do.
 */
public class AsyncApiTestBase extends EnterpriseTestBase {

    protected Solution<AsyncApiIndividual> initAndRun(List<String> args) {
        return (Solution<AsyncApiIndividual>) Main.initAndRun(args.toArray(new String[0]));
    }

    /**
     * The result of every message published, across the whole solution.
     */
    protected List<AsyncApiCallResult> allResultsOf(Solution<AsyncApiIndividual> solution) {
        return solution.getIndividuals().stream()
                .flatMap(ind -> ind.evaluatedMainActions().stream())
                .map(e -> (AsyncApiCallResult) e.getResult())
                .collect(Collectors.toList());
    }

    /**
     * The result of every message published to {@code operation}, across the whole solution.
     */
    protected List<AsyncApiCallResult> resultsOf(Solution<AsyncApiIndividual> solution, String operation) {
        return solution.getIndividuals().stream()
                .flatMap(ind -> ind.evaluatedMainActions().stream())
                .filter(e -> e.getAction().getName().equals(operation))
                .map(e -> (AsyncApiCallResult) e.getResult())
                .collect(Collectors.toList());
    }

    /**
     * The declared messages the replies to {@code operation} were recognised as.
     */
    protected Set<String> repliesOf(Solution<AsyncApiIndividual> solution, String operation) {
        return resultsOf(solution, operation).stream()
                .map(AsyncApiCallResult::getReplyMessage)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * The fault categories reported across the whole solution. Read off the action results,
     * which is where the reports count faults from, rather than off the covered targets.
     */
    protected Set<FaultCategory> faultsOf(Solution<AsyncApiIndividual> solution) {
        return solution.getIndividuals().stream()
                .flatMap(ind -> ind.evaluatedMainActions().stream())
                .map(e -> (AsyncApiCallResult) e.getResult())
                .flatMap(r -> r.getFaults().stream())
                .map(DetectedFault::getCategory)
                .collect(Collectors.toSet());
    }

    protected long countOutcome(Solution<AsyncApiIndividual> solution, AsyncApiOutcome outcome) {
        return solution.getIndividuals().stream()
                .flatMap(ind -> ind.evaluatedMainActions().stream())
                .map(e -> (AsyncApiCallResult) e.getResult())
                .filter(r -> r.getOutcome() == outcome)
                .count();
    }

    /**
     * That the controller kept every published message's action open after the driver returned,
     * as a settle window asks it to.
     */
    protected void assertEverySettled(Solution<AsyncApiIndividual> solution) {
        List<AsyncApiCallResult> results = allResultsOf(solution);
        assertTrue(!results.isEmpty(), "With seed " + defaultSeed + ": nothing was published");
        long unsettled = results.stream().filter(r -> r.getSettledMs() == null).count();
        assertTrue(unsettled == 0, "With seed " + defaultSeed + ": " + unsettled + " of " + results.size()
                + " published messages were not kept open after the driver returned");
    }

    /**
     * How many published messages the controller stopped waiting on for {@code end}.
     */
    protected long countSettleEnd(Solution<AsyncApiIndividual> solution, AsyncApiReplyDto.SettleEndReason end) {
        return allResultsOf(solution).stream()
                .filter(r -> r.getSettleEndedBy() == end)
                .count();
    }

    protected void assertReplied(Solution<AsyncApiIndividual> solution, String operation) {
        boolean ok = resultsOf(solution, operation).stream().anyMatch(r -> r.getOutcome() == AsyncApiOutcome.REPLIED);
        assertTrue(ok, "With seed " + defaultSeed + ": no reply to '" + operation + "' was ever received");
    }

    /**
     * That some reply to {@code operation} arrived and matched none of the messages the contract
     * declares for it, which is the one fault only a reply classifier can find.
     */
    protected void assertRepliedWithUndeclaredMessage(Solution<AsyncApiIndividual> solution, String operation) {
        boolean found = resultsOf(solution, operation).stream()
                .anyMatch(r -> r.getOutcome() == AsyncApiOutcome.REPLIED && r.getReplyMessage() == null);
        assertTrue(found, "With seed " + defaultSeed + ": every reply to '" + operation
                + "' matched a declared message, so the undeclared-reply path was never reached");
    }

    protected void assertReplyReached(Solution<AsyncApiIndividual> solution, String operation, String messageId) {
        Set<String> replies = repliesOf(solution, operation);
        assertTrue(replies.contains(messageId),
                "With seed " + defaultSeed + ": '" + operation + "' never replied with '" + messageId + "', only with " + replies);
    }
}
