package org.evomaster.core.search.service

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.search.Individual
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class FlakinessDetector<T: Individual> {

    companion object{
        private val log : Logger = LoggerFactory.getLogger(FlakinessDetector::class.java)
    }

    @Inject
    private lateinit var config: EMConfig
}