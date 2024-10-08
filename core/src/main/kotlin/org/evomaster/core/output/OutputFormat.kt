package org.evomaster.core.output

/**
 * Specify in which format the output test cases should be written to
 */
enum class OutputFormat {

    /**
     *  Use the format specified in the SUT driver.
     *
     *  Note: this should be kept in sync with:
     *  org.evomaster.client.java.controller.api.dto.SutInfoDto.OutputFormat
     *  but DEFAULT
     */
    DEFAULT,
    JAVA_JUNIT_5,
    JAVA_JUNIT_4,
    KOTLIN_JUNIT_4,
    KOTLIN_JUNIT_5,
    JS_JEST,
    //CSHARP_XUNIT, //no longer supported, but there is still legacy code not removed
    PYTHON_UNITTEST
    ;

    fun isJava() = this.name.startsWith("java_", true)

    fun isKotlin() = this.name.startsWith("kotlin_", true)

    fun isJavaScript() = this.name.startsWith("js_", true)

    fun isJavaOrKotlin() = isJava() || isKotlin()

    fun isJUnit5() = this.name.endsWith("junit_5", true)

    fun isJUnit4() = this.name.endsWith("junit_4", true)

    fun isJUnit() = this.name.contains("_junit_", true)

    @Deprecated("No longer supported")
    fun isCsharp() = this.name.startsWith("csharp",ignoreCase = true)

    fun isPython() = this.name.startsWith("python_", true)

}
