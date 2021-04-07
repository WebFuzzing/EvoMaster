package org.evomaster.e2etests.spring.graphql

import org.evomaster.client.java.controller.EmbeddedSutController
import org.evomaster.e2etests.utils.GraphQLTestBase
import org.evomaster.e2etests.utils.RestTestBase
import org.evomaster.e2etests.utils.WsTestBase

/**
 * Created by arcuri82 on 03-Mar-20.
 */
abstract class SpringTestBase : GraphQLTestBase() {

    companion object {
        @JvmStatic
        @Suppress("ACCIDENTAL_OVERRIDE")
        //https://youtrack.jetbrains.com/issue/KT-12993
        fun initClass(controller: EmbeddedSutController) {
            WsTestBase.initClass(controller)
        }
    }
}