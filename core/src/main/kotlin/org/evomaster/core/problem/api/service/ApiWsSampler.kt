package org.evomaster.core.problem.api.service

import com.google.inject.Inject
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionUtils
import org.evomaster.core.database.SqlInsertBuilder
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.enterprise.service.EnterpriseSampler
import org.evomaster.core.remote.SutProblemException
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.search.Individual
import org.evomaster.core.search.service.Sampler
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