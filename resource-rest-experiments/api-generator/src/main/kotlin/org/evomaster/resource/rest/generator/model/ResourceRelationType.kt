package org.evomaster.resource.rest.generator.model

/**
 * created by manzh on 2019-08-13
 */
enum class ResourceRelationType (val min : Int? = null, val max : Int? = null){
    INDEPENDENT (0, 0),
    OBVIOUS_DEPENDENT (min = 1),
    HIDE_DEPENDENT (min = 1)
}