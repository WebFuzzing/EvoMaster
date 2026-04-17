package org.evomaster.core.problem.rest.arazzo.models

sealed class CriterionType {
    data class Simple(val value: String) : CriterionType()

    data class Complex(val expr: CriterionExpression) : CriterionType()
}