package org.evomaster.core.problem.rest.seeding

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.seeding.postman.PostmanParser
import org.evomaster.core.problem.rest.service.RestSampler
import org.evomaster.core.search.service.SearchTimeController
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class AbstractParser(
        private val restSampler: RestSampler
) : Parser {

    @Inject
    protected lateinit var config: EMConfig

    @Inject
    protected lateinit var time : SearchTimeController

    protected val swagger = restSampler.getOpenAPI()

    companion object {
        private val log: Logger = LoggerFactory.getLogger(PostmanParser::class.java)
    }

}