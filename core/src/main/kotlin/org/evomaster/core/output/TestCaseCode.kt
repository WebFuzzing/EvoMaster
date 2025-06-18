package org.evomaster.core.output

import org.evomaster.core.search.EvaluatedIndividual

/**
 * Represent the generated code for a test case
 */
class TestCaseCode(
    val name: String,
    val evaluatedIndividual: EvaluatedIndividual<*>,
    val code: String,
    val startLine: Int,
    val endLine: Int
) {

    init {
        if(startLine <0){
            throw IllegalArgumentException("startLine must be non-negative")
        }
        if(endLine < 0){
            throw IllegalArgumentException("endLine must be non-negative")
        }
        if(endLine < startLine){
            throw IllegalArgumentException("endLine cannot be less than startLine")
        }
        if(name.isEmpty()){
            throw IllegalArgumentException("name must be non-empty")
        }
        if(code.isEmpty()){
            throw IllegalArgumentException("code must be non-empty")
        }
    }
}