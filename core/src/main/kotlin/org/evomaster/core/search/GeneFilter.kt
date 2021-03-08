package org.evomaster.core.search

enum class GeneFilter {
    /**
     * all genes
     */
    ALL,

    /**
     * exclude SQL genes
     */
    NO_SQL,

    /**
     * only include SQL
     */
    ONLY_SQL,

    /**
     * only include SQL for initialization
     */
    ONLY_INIT_SQL
}
