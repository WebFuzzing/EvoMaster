package org.evomaster.core.output

import org.evomaster.core.EMConfig

abstract class WriterTestBase {


    open fun getConfig(format: OutputFormat): EMConfig {
        val config = EMConfig()
        config.outputFormat = format
        config.testTimeout = -1
        config.addTestComments = false
        return config
    }
}