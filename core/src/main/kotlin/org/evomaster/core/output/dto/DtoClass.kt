package org.evomaster.core.output.dto

class DtoClass(
    val name: String,
//    val fields: MutableList<DtoField> = mutableListOf(),
    val fieldsMap: MutableMap<String, DtoField> = mutableMapOf(),
    var hasAdditionalProperties: Boolean = false,
//    var additionalProperties: DtoField? = null
) {

//    fun addField(field: DtoField) {
//        if (field !in fields) fields.add(field)
//    }

//    fun hasField(field: DtoField): Boolean {
//        return fieldsMap.containsKey(field.name)
//    }

    lateinit var additionalPropertiesDtoName: String

    fun addMapField(fieldName: String, field: DtoField) {
        if (!fieldsMap.containsKey(fieldName)) {
            fieldsMap[fieldName] = field
        }
    }




}
