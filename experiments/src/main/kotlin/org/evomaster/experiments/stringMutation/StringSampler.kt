package org.evomaster.experiments.stringMutation

import com.google.inject.Inject
import org.evomaster.core.search.service.Sampler

/**
 * created by manzh on 2019-09-16
 */
class StringSampler : Sampler<StringIndividual>() {

    @Inject
    lateinit var spd : StringProblemDefinition


    override fun sampleAtRandom(): StringIndividual {

        val ind = StringIndividual(
                spd.nTargets,
                this,
                if (config.enableTrackIndividual) mutableListOf<StringIndividual>() else null
        )
        ind.seeGenes().forEach { g -> g.randomize(randomness, false) }

        return ind
    }
}