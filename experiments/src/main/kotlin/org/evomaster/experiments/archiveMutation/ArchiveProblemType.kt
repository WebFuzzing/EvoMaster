package org.evomaster.experiments.archiveMutation

/**
 * created by manzh on 2019-09-23
 */
enum class ArchiveProblemType {
    /**
     * each two genes are correlated with each other, e.g., A = B + 1
     */
    ALL_DEP_DYNAMIC,
    /**
     * some genes (2/5) are correlated with each other, e.g., A = B + 1.
     * rest of the genes are useless.
     */
    PAR_DEP_DYNAMIC,
    /**
     * all genes are independent with each other.
     */
    ALL_IND_STABLE,
    /**
     * some genes are independent with each other, and others are useless.
     */
    PAR_IND_STABLE
}