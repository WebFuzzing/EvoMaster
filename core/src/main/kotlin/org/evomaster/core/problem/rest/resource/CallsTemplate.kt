package org.evomaster.core.problem.rest.resource

/**
 * this contains a template info for [RestResourceCalls], and the info includes
 * @property template what template is, e.g., POST-GET
 * @property independent whether it is independent with others, e.g.,, GET
 * @property size presents a number of actions for the template under a resources.
 *          the size of same template may vary on different resources due to POST actions for preparing resources
 * @property times to visit the [RestResourceCalls]
 * @property sizeAssured whether the size is checked
 */
class CallsTemplate (
        val template: String,
        val independent : Boolean,
        var size : Int = 1,
        var times : Int = 0,
        var sizeAssured : Boolean= !template.contains("POST")
)
