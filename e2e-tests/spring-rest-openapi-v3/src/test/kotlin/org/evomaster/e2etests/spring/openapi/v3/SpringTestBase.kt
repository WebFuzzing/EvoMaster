package org.evomaster.e2etests.spring.openapi.v3

import org.evomaster.client.java.controller.EmbeddedSutController
import org.evomaster.e2etests.utils.RestTestBase

/**
 * Created by arcuri82 on 03-Mar-20.
 */
abstract class SpringTestBase : RestTestBase() {

    companion object {
        @JvmStatic
        @Suppress("ACCIDENTAL_OVERRIDE")
        //https://youtrack.jetbrains.com/issue/KT-12993
        fun initClass(controller: EmbeddedSutController) {
            RestTestBase.initClass(controller)
        }
    }
}