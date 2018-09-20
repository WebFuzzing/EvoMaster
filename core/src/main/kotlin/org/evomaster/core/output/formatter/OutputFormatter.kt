package org.evomaster.core.output.formatter

import com.google.gson.GsonBuilder
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

/**
 * @javatypes: manzhang
 * @date: 27/08/2018
 * @description: this class can be extended for supporting different styles of outputs (i.e., test cases),
 *                 currently only json is supported with string input
 */
open abstract class OutputFormatter (val name: String) {

    companion object {
        private var formatters = mutableMapOf<String, OutputFormatter>()

        //this function can be used to find the proper formatter
        fun findFormatter(type: String): OutputFormatter? {
            return formatters.get(type)
        }

        fun registerFormatter(formatter: OutputFormatter){
            formatters.put(formatter.name, formatter)
        }

        fun getFormatters():List<OutputFormatter>?{
            if(formatters.size > 0)
                return formatters.values.toList();
            return null
        }

        val JSON_FORMATTER = object : OutputFormatter("JSON_FORMATTER"){
            val gson = GsonBuilder().setPrettyPrinting().create()

            override fun isValid(content: String): Boolean{
                try{
                    gson.fromJson(content, Object::class.java)
                    return true
                }catch (e : JsonSyntaxException ) {
                    return false
                }

            }
            override fun getFormatted(content: String): String{
                if(this.isValid(content)){
                    return gson.toJson(gson.fromJson(content, Object::class.java))
                }
                throw MismatchedFormatException(this, content)
            }
        }
        init {
            registerFormatter(JSON_FORMATTER);
        }


    }

    abstract fun isValid(content: String): Boolean
    abstract fun getFormatted(content: String): String


}
