package org.evomaster.core.problem.rest.service

import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.search.service.Sampler


class RestSampler : Sampler<RestIndividual>() {

    override fun sampleAtRandom(): RestIndividual {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}