package org.evomaster.core.search.service

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.externalservice.httpws.service.HttpWsExternalServiceHandler
import org.evomaster.core.problem.webfrontend.service.BrowserController

/**
 * Global state used in the search.
 * Each gene should be able to access this shared, global state.
 *
 * Implementation detail: this is not implemented as static state singleton,
 * due to all issues related to such design pattern.
 * It is not injected with Guice either, due to how we sample and clone individuals,
 * which are not injectable services.
 *
 * This instance is created only once per search (using Guice), and added manually
 * each time an individual is sampled and cloned.
 */
class SearchGlobalState {

    @Inject
    lateinit var randomness: Randomness
        private set

    @Inject
    lateinit var config: EMConfig
        private set

    @Inject
    lateinit var time : SearchTimeController
        private set

    @Inject
    lateinit var apc: AdaptiveParameterControl
        private set

    @Inject
    lateinit var spa: StringSpecializationArchive

    @Inject(optional = true)
    lateinit var browser: BrowserController

    @Inject
    lateinit var externalServiceHandler: HttpWsExternalServiceHandler
}
