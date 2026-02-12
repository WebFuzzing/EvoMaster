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

    fun getNumberOfFlakyComment(config : EMConfig, testContent: String): Int{
        val commentFlag = if (config.outputFormat.isJavaOrKotlin())
            "//"
        else if (config.outputFormat.isPython()) "#"
        else throw IllegalStateException("Not supported yet")
        return  Regex("$commentFlag Flaky").findAll(testContent).count()
    }
}