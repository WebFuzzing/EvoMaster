package org.evomaster.core.search

/**
 * Besides typical code coverage and HTTP coverage metrics, there could be other interesting coverage criteria
 * to consider.
 * Those will need their own customize "collectors" to compute.
 */
interface AdditionalTargetCollector {

    /**
     * Tell the collector that a new test is going to be evaluated now, so it can prepare its own datastructures.
     *
     * [testId] identifier for executed test
     */
    fun goingToStartExecutingNewTest(testId: String)

    /**
     * A test case could be composed of several actions, with index from 0 to N-1.
     * If such info is useful or needed to be collected for the TargetInfo, this method is called before each
     * action in a test is executed, with its index.
     */
    fun reportActionIndex(testId: String, actionIndex: Int)

    /**
     * The test case execution has been completed, and now its results can be collected.
     * Note: there is no need whatsoever to collect [TargetInfo] values for target that have
     * heuristic scores of 0, as those will always be ignored by EvoMaster's internal search engine.
     */
    fun testFinishedCollectResult(testId: String) : List<TargetInfo>

    /**
     *  Would this collector work if test cases are run in parallel?
     *  if not, then we cannot use it when running tests in parallel.
     *
     *  Note: most likely, in most cases, this will return false.
     */
    fun supportConcurrency() : Boolean
}