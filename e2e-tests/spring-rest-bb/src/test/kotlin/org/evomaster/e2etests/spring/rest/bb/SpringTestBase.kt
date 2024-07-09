package org.evomaster.e2etests.spring.rest.bb


import org.evomaster.core.output.OutputFormat
import org.evomaster.e2etests.utils.RestTestBase


abstract class SpringTestBase : RestTestBase(){


    protected fun addBlackBoxOptions(
        args: MutableList<String>,
        outputFormat: OutputFormat
    ){
        setOption(args, "blackBox", "true")
        setOption(args, "bbTargetUrl", baseUrlOfSut)
        setOption(args, "bbSwaggerUrl", "$baseUrlOfSut/v3/api-docs")
        setOption(args, "problemType", "REST")
        setOption(args, "outputFormat", outputFormat.toString())

        //this is deprecated
        setOption(args, "bbExperiments", "false")
    }
}