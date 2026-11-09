package org.evomaster.core

import kotlin.annotation.Target
import kotlin.reflect.KClass


/**
 * A method marked with [StartService] in a singleton service will be executed after ALL @PostConstruct have
 * been resolved.
 * If this needs to use another service already started, it must be specified with [dependsOn].
 * Cycles are NOT allowed, and will crash application on startup.
 */
@Target(AnnotationTarget.FUNCTION)
@MustBeDocumented
annotation class StartService(val dependsOn: Array<KClass<*>>)
