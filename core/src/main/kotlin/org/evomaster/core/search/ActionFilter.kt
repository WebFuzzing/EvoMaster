package org.evomaster.core.search

enum class ActionFilter {
    /**
     * all actions
     */
    ALL,

    /**
     * all rest actions
     */
    REST,

    /**
     * all db actions
     */
    DB,

    /**
     * all action in initialization
     */
    INIT,

    /**
     * exclude all action in initialization
     */
    NO_INIT
}