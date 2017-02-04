package org.evomaster.core.output

/**
 * Specify in which format the output test cases should be written to
 */
enum class OutputFormat {

    JAVA_JUNIT_5,
    JAVA_JUNIT_4
    ;

    /*
        TODO:
        KOTLIN_JUNIT_5
        JAVA_TESTNG
        KOTLIN_TESTNG

        and in the future, also support other languages,
        eg JavaScript
     */


    fun isJava() = this.name.startsWith("java", true)

    fun isKotlin() = this.name.startsWith("kotlin", true)

    fun isJavaOrKotlin() = isJava() || isKotlin()

    fun isJUnit5() = this.name.endsWith("junit_5", true)

    fun isJUnit4() = this.name.endsWith("junit_4", true)

    fun isJUnit() = this.name.contains("_junit_", true)
}