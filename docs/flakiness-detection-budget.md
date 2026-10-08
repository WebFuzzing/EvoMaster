# Flakiness detection budget and completion reporting

Flakiness detection checks its own `FLAKINESS` phase budget, independently of a
previous `SECURITY` timeout. By default, `useTimeLimitForFlakiness=true` retains
the budget controlled by `extraPhaseBudgetPercentage`.

For experiments that require a fixed search budget followed by all requested
re-executions, add these options to the existing REST experiment command:

```text
--maxTime 1h
--handleFlakiness true
--enableStaticFlakyInference true
--execNumForDetectFlakiness 10
--useTimeLimitForFlakiness false
```

This disables only the phase-wide flakiness timeout. It does not extend the main
search, disable other post-processing budgets, or change request/test timeouts.
An external process deadline can still interrupt the run. A phase budget is a
cooperative check, not a hard cancellation of an in-flight request.

The detector reports static-inference and re-execution summaries, plus one
re-execution summary for each visited individual (zero-based archive index):

- `expected`: individuals for static inference; individuals multiplied by the
  requested repetition count for the aggregate re-execution summary.
- `attempted`: processing/evaluation attempts started.
- `completed`: static processing completed, or a finished execution was recorded
  durably. HTTP errors, timeouts, TCP errors and an explicitly stopped execution
  count as recorded outcomes, not missing work.
- `failed`: attempts without a reliable, finished execution record; not test failures.
- `notAttempted`: expected work that was never started.
- `timedOut`: the detector stopped at a phase budget check.
- `complete`: all expected work completed without stopping for the phase budget.
- `elapsedMs`: elapsed wall-clock duration measured using a monotonic clock.

Null re-evaluations without captured responses must not be interpreted
as recorded executions. Unexpected tool exceptions propagate instead of being silently
ignored; `finally` summaries preserve incomplete progress for ordinary exceptions.
A killed JVM cannot be expected to produce a final summary. No final summary
means completion is unverified, not successful.

Partial re-executions are matched by action source local ID, not list position.
Common HTTP responses are compared even when the execution paths differ. Missing,
extra or reordered actions, incompatible results, changed stopping
behavior and failed HTTP calls are reported separately as partial comparisons. Duplicate IDs
or a truncated path without a stopping result remain invalid records. The diagnostic
log records action IDs and transport failure flags; it does not fabricate a
response for an unexecuted action or treat it as a stable response.

White-box REST evaluation snapshots action results before coverage handling can
return null (including TCP failures). The flakiness detector compares any captured
responses and counts a captured, finished execution even if coverage is null. The observer is scoped to
that evaluation and removed in a finally block. Search and other post-processing
callers retain their original return values and retry behavior. No successful-only
retry is introduced.

Each detector invocation writes a unique `flakiness-observations-*.jsonl` in the
test output folder. One flushed record per attempted repetition retains baseline
and observed action result values, action IDs, stopping flags, planned and unexecuted
steps, coverage availability and recording status. These are the values retained by
EvoMaster, not a raw HTTP capture: existing response-size limits still apply. Files
may contain sensitive response data and should be handled like generated tests.
`partialComparisons` and `exceptionalExecutions` are supplementary counters, not
reasons to reject an otherwise fully recorded run. A test that fails consistently
is not thereby flaky; variability must be assessed across repeated outcomes.

This retains evidence that was previously discarded. It does not repair SUT state
isolation, prevent TCP failures, or guarantee compilable/stable generated suites.

These counters confirm detector processing, not that all generated Java tests
compile or pass. Compilation and the experiment's 100 final test-suite replays
remain separate validation steps. Likewise, completing the detector does not
prove the absence of flakiness outside its observation window.

Targeted regression command (run from the repository root):

```sh
mvn -pl core -am test \
  -Dtest=FlakinessDetectorTest,EMConfigTest,RestCallResultTest,FlakinessInferenceUtilTest \
  -Dsurefire.failIfNoSpecifiedTests=false -DfailIfNoTests=false
```
