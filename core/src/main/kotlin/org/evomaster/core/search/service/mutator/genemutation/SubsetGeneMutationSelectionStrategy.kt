package org.evomaster.core.search.service.mutator.genemutation

/**
 * created by manzh on 2020-05-27
 *
 * How weights are used to select genes for mutation.
 * Typically, higher weights mean higher chances to be selected.
 *
 * In theory, this is independent from hypermutation: ie, the NUMBER of genes to mutate.
 *
 * WARNING: currently [DEFAULT] deactivates hypermutation.
 * TODO: shall we update such behavior???
 */
enum class SubsetGeneMutationSelectionStrategy {
    /**
     * No hyper-mutation is applied. So, only one gene is selected to be mutated on average (p=1/n).
     * All genes are treated the same, ie, same weight
     *
     */
    DEFAULT,
    /**
     *  Weight that depends only on the type of the gene
     */
    DETERMINISTIC_WEIGHT,

    /**
     * Weight that is using previous "impact" information of the gene having
     * on the fitness function
     */
    ADAPTIVE_WEIGHT
}