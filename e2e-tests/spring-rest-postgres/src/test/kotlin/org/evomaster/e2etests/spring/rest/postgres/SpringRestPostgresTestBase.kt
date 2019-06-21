package org.evomaster.e2etests.spring.rest.postgres

import org.evomaster.client.java.controller.EmbeddedSutController
import org.evomaster.e2etests.utils.RestTestBase

/**
 * Created by arcuri82 on 21-Jun-19.
 */
open class SpringRestPostgresTestBase : RestTestBase() {

    companion object {
        @JvmStatic
        fun initKlass(controller: EmbeddedSutController) {
            RestTestBase.initClass(controller)
        }
    }
}