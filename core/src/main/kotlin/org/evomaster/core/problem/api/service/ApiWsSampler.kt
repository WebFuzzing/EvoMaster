package org.evomaster.core.problem.api.service

import org.evomaster.core.problem.enterprise.service.EnterpriseSampler
import org.evomaster.core.search.Individual
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * abstract sampler for handling API based SUT, such as REST, GraphQL, RPC
 */
abstract class ApiWsSampler<T> : EnterpriseSampler<T>() where T : Individual {


    companion object {
        private val log: Logger = LoggerFactory.getLogger(ApiWsSampler::class.java)
    }



}