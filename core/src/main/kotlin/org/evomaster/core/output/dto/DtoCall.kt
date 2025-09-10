package org.evomaster.core.output.dto

class DtoCall(
    val varName: String,
    val objectCalls: List<String>
) {

    fun addCalls(acum: MutableList<String>) {
        acum.addAll(objectCalls)
    }

}
