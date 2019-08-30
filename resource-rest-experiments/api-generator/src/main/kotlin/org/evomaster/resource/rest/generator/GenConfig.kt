package org.evomaster.resource.rest.generator

import org.evomaster.resource.rest.generator.model.RestMethod

/**
 * created by manzh on 2019-08-14
 */
class GenConfig {

    /**
     * output folder
     */
    var outputFolder = "/Users/mazh001/Documents/GitHub/automated-generated-api/"

    /**
     *
     */
    var outputType = OutputType.MAVEN_MODULE

    enum class OutputType{
        SOURCE,
        MAVEN_MODULE,
        MAVEN_PROJECT,
    }
    var outputContent = OutputContent.CS

    enum class OutputContent{
        EM,
        CS,
        BOTH
    }

    var projectName = "auto-rest-example"
    var csName = "cs"
    var emName = "em"

    var srcFolder = "src/main"

    var csProjectPackage = "org.evomaster.resource.rest.artificial.cs"

    var emProjectPackage = "org.evomaster.resource.rest.artificial.em.controller"

    var language = Format.JAVA_SPRING_SWAGGER

    enum class Format(val srcFolder : String, val resource : String?){
        JAVA_SPRING_SWAGGER("java", "resources")
    }

    var restMethods = listOf(RestMethod.POST, RestMethod.GET_ID, RestMethod.GET_ALL, RestMethod.PUT, RestMethod.DELETE, RestMethod.PATCH_VALUE, RestMethod.PATCH)

    var numOfNodes = 10

    var numOfOneToOne = 0

    var numOfOneToTwo = 0

    var numOfOneToMany = 0

    var numOfTwoToOne = 0

    var numOfManyToOne = 0

    var numOfTwoToTwo = 0

    var numOfManyToMany = 0

    fun getCsOutputFolder() = "${FormatUtil.formatFolder(getCsRootFolder())}$srcFolder/${language.srcFolder}"
    fun getCsResourceFolder() = "${FormatUtil.formatFolder(getCsRootFolder())}$srcFolder/${language.resource}"
    private fun getRootFolder() = "${FormatUtil.formatFolder(outputFolder)}${if (outputContent == OutputContent.BOTH) "$projectName" else ""}"
    fun getCsRootFolder() = "${FormatUtil.formatFolder(getRootFolder())}${if (outputType != OutputType.SOURCE) csName else ""}"
    fun getEmOutputFolder() = "${FormatUtil.formatFolder(getRootFolder())}$emName/$srcFolder/${language.srcFolder}"

}