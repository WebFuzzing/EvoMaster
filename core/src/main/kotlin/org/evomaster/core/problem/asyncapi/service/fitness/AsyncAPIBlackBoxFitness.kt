package org.evomaster.core.problem.asyncapi.service.fitness

/**
 * Black-box AsyncAPI fitness.  All targets are derived purely from the
 * AsyncAPI schema, with no SUT-specific oracle and no EM Driver.
 *
 * The full target set + scoring loop lives in [AbstractAsyncAPIFitness] —
 * this subclass exists so the Guice module can bind a black-box-specific
 * type and so future black-box-only behaviour (e.g. fixed-timeout knobs
 * for runs without coverage stabilisation) can be added without disturbing
 * the white-box leaf.
 */
class AsyncAPIBlackBoxFitness : AbstractAsyncAPIFitness()
