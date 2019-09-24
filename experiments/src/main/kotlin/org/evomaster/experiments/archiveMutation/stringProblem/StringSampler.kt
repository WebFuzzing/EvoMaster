package org.evomaster.experiments.archiveMutation.stringProblem

import com.google.inject.Inject
import org.evomaster.core.search.service.Sampler
import org.evomaster.experiments.archiveMutation.ArchiveProblemDefinition

/**
 * created by manzh on 2019-09-16
 */
class StringSampler : Sampler<StringIndividual>() {

    @Inject
    lateinit var sp : ArchiveProblemDefinition<StringIndividual>


    override fun sampleAtRandom(): StringIndividual {

        val ind = StringIndividual(
                sp.nGenes,
                this,
                if (config.enableTrackIndividual) mutableListOf<StringIndividual>() else null
        )
        ind.seeGenes().forEach { g -> g.randomize(randomness, false) }

        return ind
    }
}