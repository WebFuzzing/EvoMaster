package org.evomaster.core.problem.webfrontend


data class WebUserInteraction(
    val cssSelector : String,
    val userActionType : UserActionType
    )