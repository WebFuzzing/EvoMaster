package org.evomaster.core.search.structuralelement.param

import org.evomaster.core.problem.graphql.param.GQInputParam
import org.evomaster.core.problem.graphql.param.GQReturnParam
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.structuralelement.StructuralElementBaseTest

class GQInputParamStructureTest : StructuralElementBaseTest() {

    override fun getStructuralElement(): GQInputParam = GQInputParam("input", StringGene("input"))

    override fun getExpectedChildrenSize(): Int  = 1

}

class GQReturnParamStructureTest : StructuralElementBaseTest() {

    override fun getStructuralElement(): GQReturnParam = GQReturnParam("return", StringGene("return"))

    override fun getExpectedChildrenSize(): Int  = 1

}