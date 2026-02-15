package org.evomaster.core.output.dto

class DtoClass(
    val name: String,
    val fieldsMap: MutableMap<String, DtoField> = mutableMapOf(),
//    var hasAdditionalProperties: Boolean = false,
    var additionalPropertiesDtoName: String? = null
) {

//    private lateinit var additionalPropertiesDtoName: String

    fun addField(fieldName: String, field: DtoField) {
        if (!fieldsMap.containsKey(fieldName)) {
            fieldsMap[fieldName] = field
        }
    }

    fun hasAdditionalProperties(): Boolean {
        return !additionalPropertiesDtoName.isNullOrEmpty()
    }

}
