package org.evomaster.core.problem.rest.resource

class CallsTemplate (
        val template: String,
        val independent : Boolean,
        var size : Int = 1,
        var times : Int = 0,
        var sizeAssured : Boolean= !template.contains("POST")
)
