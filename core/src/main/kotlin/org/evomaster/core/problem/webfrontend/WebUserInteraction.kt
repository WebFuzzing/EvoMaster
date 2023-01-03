package org.evomaster.core.problem.webfrontend


data class WebUserInteraction(
    val htmlSelector : String,
    val userActionType : UserActionType
    )