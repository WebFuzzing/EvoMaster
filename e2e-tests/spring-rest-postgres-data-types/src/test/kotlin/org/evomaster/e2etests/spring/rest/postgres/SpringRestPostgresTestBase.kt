package org.evomaster.e2etests.spring.rest.postgres

import org.evomaster.client.java.controller.EmbeddedSutController
import org.evomaster.e2etests.utils.RestTestBase

/**
 * Created by jgaleotti on 18-Apr-22.
 */
open class SpringRestPostgresTestBase : RestTestBase() {

    companion object {
        @JvmStatic
        fun initKlass(controller: EmbeddedSutController) {
            initClass(controller)
        }
    }
}