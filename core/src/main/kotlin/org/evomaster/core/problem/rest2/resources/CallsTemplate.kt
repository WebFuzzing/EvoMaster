package org.evomaster.core.problem.rest2.resources

class CallsTemplate (
        val template: String,
        val independent : Boolean,
        var size : Int = 1,
        var probability : Double = -1.0,
        var times : Int = 0,

        var sizeAssured : Boolean= false
)
