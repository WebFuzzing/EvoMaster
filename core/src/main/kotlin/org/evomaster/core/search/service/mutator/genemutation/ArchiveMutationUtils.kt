package org.evomaster.core.search.service.mutator.genemutation

import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.util.ParamUtil
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.Individual
import org.evomaster.core.search.action.ActionFilter
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.service.mutator.EvaluatedMutation
import org.evomaster.core.search.service.mutator.MutatedGeneSpecification
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

/**
 * created by manzh on 2020-07-08
 */
object ArchiveMutationUtils {

    /**
     * save detailed mutated gene over search which is useful for debugging
     */
    fun saveMutatedGene(config: EMConfig, mutatedGenes: MutatedGeneSpecification?, individual: Individual, index : Int, evaluatedMutation : EvaluatedMutation, targets: Set<Int>){
        mutatedGenes?:return
        val path = saveMutationInfo(config)?: return

        val content = mutableListOf<String>()
        content.addAll(mutatedGenes.mutatedGenes.mapIndexed { gindex, geneInfo -> listOf(
                index,
                evaluatedMutation,
                geneInfo.gene?.name,
                geneInfo.previousValue,
                geneInfo.gene?.getValueAsPrintableString(),
                "#${targets.joinToString("#")}",
                geneInfo.actionPosition,
                if (geneInfo.actionPosition!=null)
                    getActionInfo(individual.seeActions(ActionFilter.NO_INIT)[geneInfo.actionPosition])
                else "").joinToString(",")} )

        content.addAll(mutatedGenes.mutatedDbGenes.mapIndexed { gindex, geneInfo -> listOf(
                index,
                evaluatedMutation,
                geneInfo.gene?.name,
                geneInfo.previousValue,
                geneInfo.gene?.getValueAsPrintableString(),
                "#${targets.joinToString("#")}",
                geneInfo.actionPosition,
                if (geneInfo.actionPosition != null)
                    getActionInfo(individual.seeInitializingActions()[geneInfo.actionPosition])
                else "" ).joinToString(",")})

        if (content.isNotEmpty()) {
            Files.write(path, content, StandardOpenOption.APPEND)
        }
    }
    fun saveWeight(config: EMConfig, map : MutableMap<Gene, Double>, index : Int, targets: Set<Int>) {
        if (map.isEmpty()) return
        val path = saveMutationInfo(config)?: return

        val text = "$index, ${targets.joinToString("-")}, weights: ${map.map { "${ParamUtil.getValueGene(it.key).name}:${it.value}" }.joinToString(";")}"
        Files.write(path, listOf(text), StandardOpenOption.APPEND)
    }


    private fun saveMutationInfo(config : EMConfig) : Path?{
        if(!config.saveMutationInfo) return null

        val path = Paths.get(config.mutatedGeneFile)
        if (path.parent != null) Files.createDirectories(path.parent)
        if (Files.notExists(path)) Files.createFile(path)
        return path
    }

    private fun getActionInfo(action : Action) : String{
        return if (action is RestCallAction) action.resolvedPath()
        else action.getName()
    }

}