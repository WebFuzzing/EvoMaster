package org.evomaster.core.search

enum class ActionFilter {
    /**
     * all actions
     */
    ALL,

    /**
     * The main actions that are executable, eg API calls
     */
    MAIN_EXECUTABLE,

    /**
     * actions which are in initialization
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
    NO_SQL,

    /**
     * actions which are External Service related actions
     */
    ONLY_EXTERNAL_SERVICE,

    /**
     * actions which are not external service actions
     */
    NO_EXTERNAL_SERVICE,
}