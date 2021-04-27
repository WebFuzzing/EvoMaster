package org.evomaster.core.problem.graphql

import org.evomaster.core.problem.graphql.param.GQReturnParam
import org.evomaster.core.search.gene.*

object GraphQLUtils {

    fun getMutation(returnGene: Gene, a: GraphQLAction): String {
        return returnGene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.BOOLEAN_SELECTION_MODE)
    }

    fun getQuery(returnGene: Gene, a: GraphQLAction): String {
        return returnGene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.BOOLEAN_SELECTION_MODE)
    }

    fun getPrintableInputGenes(printableInputGene: MutableList<String>): String {

        return printableInputGene.joinToString(",").replace("\"", "\\\"")

    }

    fun getPrintableInputGene(inputGenes: List<Gene>): MutableList<String> {
        val printableInputGene = mutableListOf<String>()
        for (gene in inputGenes) {
            if (gene is EnumGene<*> || (gene is OptionalGene && gene.gene is EnumGene<*>)) {
                val i = gene.getValueAsRawString()
                printableInputGene.add("${gene.name} : $i")
            } else {
                if (gene is ObjectGene || (gene is OptionalGene && gene.gene is ObjectGene)) {
                    val i = gene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.GQL_INPUT_MODE)
                    printableInputGene.add(" $i")
                } else {
                    if (gene is ArrayGene<*> || (gene is OptionalGene && gene.gene is ArrayGene<*>)) {
                        val i = gene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.GQL_INPUT_ARRAY_MODE)
                        printableInputGene.add("${gene.name} : $i")
                    } else {
                        val i = gene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.GQL_INPUT_MODE)
                        printableInputGene.add("${gene.name} : $i")
                    }
                }
            }
        }
        return printableInputGene
    }


    fun repairIndividual(ind: GraphQLIndividual){
        ind.seeActions().forEach { a ->
            a.parameters.filterIsInstance<GQReturnParam>().forEach { p ->
                if(p.gene is ObjectGene){
                    GeneUtils.repairBooleanSelection(p.gene)
                }
            }
        }
    }

}