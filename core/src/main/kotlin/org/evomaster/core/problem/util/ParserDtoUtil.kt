package org.evomaster.core.problem.util

import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.core.Lazy
import org.evomaster.core.problem.rest.RestActionBuilderV3
import org.evomaster.core.search.gene.Gene

object ParserDtoUtil {

    fun getOrParseDtoWithSutInfo(infoDto: SutInfoDto) : Map<String, Gene>{
        /*
            need to get all for handling `ref`
         */
        val names = infoDto.unitsInfoDto?.parsedDtos?.keys?.toList()?:return emptyMap()
        val schemas = names.map { infoDto.unitsInfoDto.parsedDtos[it]!! }
        //TODO need to check: referType is same with the name?
        val genes = RestActionBuilderV3.createObjectGeneForDTOs(names, schemas, names)
        Lazy.assert { names.size == genes.size }
        return names.mapIndexed { index, s -> s to genes[index] }.toMap()
    }

}