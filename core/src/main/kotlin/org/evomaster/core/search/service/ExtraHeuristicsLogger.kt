package org.evomaster.core.search.service

import com.google.inject.Inject
import org.evomaster.client.java.controller.api.dto.HeuristicEntryDto
import org.evomaster.core.EMConfig
import java.nio.file.Files
import java.nio.file.Paths
import javax.annotation.PostConstruct

/**
 * Service used to write to disk info on the extra heuristics
 */
class ExtraHeuristicsLogger {

    @Inject
    private lateinit var config: EMConfig

    @Inject
    private lateinit var time: SearchTimeController

    @PostConstruct
    private fun postConstruct() {
        if (config.writeExtraHeuristicsFile) {

            val path = getFilePath()

            Files.createDirectories(path.parent)

            Files.deleteIfExists(path)
            Files.createFile(path)

            path.toFile().appendText("testCounter,actionId,value,id,type,objective,group\n")
        }
    }

    private fun getFilePath() = Paths.get(config.extraHeuristicsFile).toAbsolutePath()


    fun writeHeuristics(list: List<HeuristicEntryDto>, actionId: Int) {
        if (!config.writeExtraHeuristicsFile) {
            return
        }

        val counter = time.evaluatedIndividuals


        val lines = list.map {
            val group = if (it.type == HeuristicEntryDto.Type.SQL)
                /*
                    TODO: ideally here we would remove all constants from the
                    SQL commands (eg, strings and numbers), to group together commands
                    with same structure, regardless of the used variables.
                    But, considering current SQL parser we have, it would be a lot
                    of work for not much gain (as only used for analyzing results,
                    and no impact on search)
                 */
                "-"
            else "-"

            "$counter,$actionId,${it.value},\"${it.id}\",${it.type},${it.objective},$group\n"
        }.joinToString("")

        getFilePath().toFile().appendText(lines)
    }
}