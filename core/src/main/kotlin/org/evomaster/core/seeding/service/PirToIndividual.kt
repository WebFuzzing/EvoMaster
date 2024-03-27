package org.evomaster.core.seeding.service

import com.google.inject.Inject
import org.evomaster.core.problem.rest.service.AbstractRestSampler
import org.evomaster.core.search.service.Randomness

/**
 * From Pojo Internal Representation (PIR), possibly derived from a textual representation (eg in JSON) of a test case,
 * do create an individual, based on actual schema of the SUT.
 * Note: we can assume that schema might change, and so the "old" test cases might not fully match the constraint
 * in the current schema. Also, text might be manually modified by users.
 * In other words, we can assume errors in parsing... in such cases, we should NOT crash EM, but rather issue warning
 * messages.
 *
 * TODO should consider if this should be made a "service"
 *
 * TODO this will be the core of new "seeding" mechanism, eg seed from previous runs or manual edits from users,
 * but for now we just need something basic to enable integration tests
 */
abstract class PirToIndividual {

    @Inject
    protected lateinit var randomness: Randomness

}