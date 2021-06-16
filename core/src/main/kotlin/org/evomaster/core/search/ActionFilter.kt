package org.evomaster.core.search

enum class ActionFilter {
    /**
     * all actions
     */
    ALL,

    /**
     * actions which are in initialization, e.g., HttpWsIndividual
     */
    INIT,

    /**
     * actions which are not in initialization
     */
    NO_INIT,

    /**
     * actions which are SQL-related actions
     */
    ONLY_SQL,

    /**
     * actions which are not SQL-related actions
     */
    NO_SQL
}