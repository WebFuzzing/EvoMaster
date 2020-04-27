package org.evomaster.resource.rest.generator

/**
 * created by manzh on 2019-08-14
 */
object FormatUtil {

    fun upperFirst(text: String) = upper(text, 0)

    fun lowerFirst(text: String) = lower(text, 0)

    fun formatResourceOnPath(name : String) = "${lowerFirst(name)}s"

    fun formatResourceIdAsPathParam(name: String, idName: String) = "${lowerFirst(name)}${upperFirst(idName)}"


    private fun upper(text : String, index : Int) = text.replaceRange(index, index+1, text[index].toUpperCase().toString())

    private fun lower(text : String, index : Int) = text.replaceRange(index, index+1, text[index].toLowerCase().toString())

    fun formatFolder(path : String) = System.getProperty("file.separator").run { if (path.endsWith(this)) path else "$path$this" }

}

