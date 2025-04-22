package org.evomaster.core.search.gene.optional

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.root.CompositeFixedGene
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy

/**
 * A wrapper for a gene that might be skipped in the phenotype if not active.
 *
 * Whether it is skipped completely or put to null depends on the concrete gene.
 */
abstract class SelectableWrapperGene(name: String,
                                     val gene: Gene,
                                     var isActive: Boolean = true
): CompositeFixedGene(name, gene) {


    /**
     * In some cases, we might want to prevent this gene from being active
     */
    var selectable = true
        protected set


    fun forbidSelection(){
        selectable = false
        isActive = false
    }

    override fun isMutable(): Boolean {
        return selectable
    }

    @Suppress("BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER")
    override fun <T,K> getWrappedGene(klass: Class<K>, strict: Boolean) : T?  where T : Gene, T: K{
        if(matchingClass(klass,strict)){
            return this as T
        }
        return gene.getWrappedGene(klass)
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {

        if(!gene.initialized && gene.isMutable()){
            //make sure that, if not initialized, to randomize it, to make sure constraints are satisfied
            gene.randomize(randomness, false)
        }

        if(!selectable){
            return
        }

        if (!tryToForceNewValue) {
            isActive = randomness.nextBoolean()
            if(gene.isMutable()) {
                gene.randomize(randomness, false)
            }
        } else {

            if (randomness.nextBoolean() || !gene.isMutable()) {
                isActive = !isActive
            } else {
                gene.randomize(randomness, true)
            }
        }
    }


    override fun mutablePhenotypeChildren(): List<Gene> {

        if (!isActive || !gene.isMutable()) return emptyList()

        return listOf(gene)
    }

    override fun shallowMutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, selectionStrategy: SubsetGeneMutationSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?) : Boolean{

        isActive = !isActive
        if (enableAdaptiveGeneMutation){
            //TODO MAN further check
        }

        return true
    }

    override fun getVariableName() = gene.getVariableName()



    override fun mutationWeight(): Double {
        return 1.0 + gene.mutationWeight()
    }

    override fun isPrintable(): Boolean {
        /*
            A non active gene would end up in being an empty string, that could still be part of the phenotype.
            For example, when dealing with JavaScript, we could have array with empty elements, like
            x = [,,]
            which is different from
            x = [null,null,null]
            as in the former case the values are 'undefined'
         */
        return !isActive || gene.isPrintable()
    }
}