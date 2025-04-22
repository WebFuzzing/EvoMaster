package org.evomaster.core.output

/**
 * Represent the generated code for a test suite
 */
class TestSuiteCode(
    val testSuiteName: String,
    val testSuitePath: String,
    val code: String,
    val tests: List<TestCaseCode>
) {

    init {
        if(testSuiteName.isEmpty()){
            throw IllegalArgumentException("testSuiteName must be non-empty")
        }
        if(testSuitePath.isEmpty()){
            throw IllegalArgumentException("testSuitePath must be non-empty")
        }
        if(code.isEmpty()){
            throw IllegalArgumentException("code must be non-empty")
        }
        for(i in 0 until tests.size-1){
            val current = tests[i]
            val next = tests[i+1]
            if(current.endLine >= next.startLine){
                throw IllegalArgumentException("test at index $i ends after ${i+1} starts: ${current.endLine} >= ${next.startLine}")
            }
        }
    }
}