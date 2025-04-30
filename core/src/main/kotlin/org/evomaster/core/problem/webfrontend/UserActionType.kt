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

    DOUBLE_CLICK,
    RIGHT_CLICK,
    HOVER_OVER_ELEMENT,
    VERTICAL_SCROLLING,
    HORIZONTAL_SCROLLING,
    DRAG_AND_DROP,

    CHECK_CHECKBOX,
    UNCHECK_CHECKBOX,
    CHOOSE_RADIO_BUTTON,
    SELECT,
    UPLOAD_FILE,
}