package org.evomaster.core.problem.webfrontend

enum class UserActionType {

    CLICK,

    FILL_TEXT,

    /**
     * Select only a single element in a dropdown <select> element
     */
    SELECT_SINGLE,

    /**
     * Some dropdown <select> elements might allow to select more than one element
     */
    SELECT_MULTI,
}