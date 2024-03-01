package org.evomaster.core.problem.rest.param

import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.optional.OptionalGene


class HeaderParam(name: String, gene: Gene) : Param(name, gene){

    override fun copyContent(): Param {
        return HeaderParam(name, gene.copy())
    }

    fun isInUse() = gene !is OptionalGene || gene.isActive

    fun getRawValue() : String{
        if(!isInUse()){
            throw IllegalStateException("Trying to get raw value of an unused header")
        }

        val s = gene.getValueAsRawString()
        if(!s.contains("\n")){
            return s
        }

        /*
            This was a weird one... got such bug deep inside a JDK class
            sun.net.www.protocol.http.HttpURLConnection.checkMessageHeader
            where throw exception if there is any character after \n which is not a trailing space.
            Seems like in the past was possible to have LN, but it has been deprecated?
            https://www.rfc-editor.org/rfc/rfc7230#section-3.2.4
            although unclear of exceptions.
            anyway, even if could prevent \n in GeneString, this would not work when taint on parsed JSON.
            so, for simplicity we just replace \n with space here.
            FIXME: would need better solution if this becomes problematic
         */
        return s.replace('\n', ' ')
    }
}