package org.evomaster.core.search.gene

import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy

/**
 * A basic gene that contains no internal genes
 */
abstract class SimpleGene(name: String) : Gene(name, mutableListOf()){


    override fun addChild(child: StructuralElement) {
        throw IllegalStateException("BUG in EvoMaster: cannot modify children of childless ${this.javaClass}")
    }

    //TODO delete

    //TODO should it be final? some simple genes seems to use it...
    override fun copyContent(): Gene {
        throw IllegalStateException("Bug in ${this::class.java.simpleName}: copyContent() must not be called on a SimpleGene")
    }

    final override fun candidatesInternalGenes(randomness: Randomness,
                                apc: AdaptiveParameterControl,
            //TODO remove deprecated
                                allGenes: List<Gene>,
                                selectionStrategy: SubsetGeneSelectionStrategy,
                                enableAdaptiveGeneMutation: Boolean,
                                additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): List<Gene>{
        return listOf()
    }
}