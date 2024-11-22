package org.evomaster.core.search.service.monitor

/**
 * specify field which should be excluded when saving process monitor data
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class ProcessMonitorExcludeField
