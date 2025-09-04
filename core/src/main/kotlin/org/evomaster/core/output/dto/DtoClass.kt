package org.evomaster.core.output.dto

class DtoClass(
    val name: String,
    val fields: MutableList<DtoField> = mutableListOf()) {

    fun addField(field: DtoField) {
        if (field !in fields) fields.add(field)
    }

}
