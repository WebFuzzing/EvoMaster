package org.evomaster.resource.rest.generator.implementation.java.service

import org.evomaster.resource.rest.generator.implementation.java.JavaMethod
import org.evomaster.resource.rest.generator.implementation.java.SpringRestAPI
import org.evomaster.resource.rest.generator.model.RestMethod
import org.evomaster.resource.rest.generator.model.ServiceClazz

/**
 * created by manzh on 2019-12-19
 */
abstract class JavaRestMethod (val specification: ServiceClazz, val method : RestMethod): JavaMethod(), SpringRestAPI{

    override fun getName(): String  = Utils.generateRestMethodName(method, specification.resourceName)

}