package org.evomaster.core.utils

import joptsimple.AbstractOptionSpec
import joptsimple.NonOptionArgumentSpec
import joptsimple.OptionException
import joptsimple.OptionSet
import org.evomaster.core.config.ConfigProblemException
import org.evomaster.core.config.ConfigsFromFile
import kotlin.reflect.KMutableProperty
import kotlin.reflect.jvm.javaType

object CliUtils {

    fun modifiedOptions(options: OptionSet, cff: ConfigsFromFile?) : Set<String>{

        val detected  = OptionSet::class.java.getDeclaredField("detectedOptions")
            .apply { setAccessible(true) }
            .get(options) as Map<String,AbstractOptionSpec<*>>

        val names = detected.filter { it.value !is NonOptionArgumentSpec }.keys

        return if(cff == null) {
            names.toSet()
        } else {
            names.toMutableSet().plus(cff.configs.keys)
        }
    }

    fun updateProperty(options: OptionSet, m: KMutableProperty<*>) {
        //update value, but only if it was in the specified options.
        //WARNING: without this check, it would reset to default for fields not in "options"
        if (!options.has(m.name)) {
            return
        }

        val opt = try{
            options.valueOf(m.name)?.toString()
        } catch (e: OptionException){
            throw  ConfigProblemException("Error in parsing configuration option '${m.name}'. Library message: ${e.message}")
        } ?: throw ConfigProblemException("Value not found for property '${m.name}'")

        updateValue(opt, m)
    }

    private fun updateValue(optionValue: String, m: KMutableProperty<*>) {

        val returnType = m.returnType.javaType as Class<*>

        /*
                TODO: ugly checks. But not sure yet if can be made better in Kotlin.
                Could be improved with isSubtypeOf from 1.1?
                http://stackoverflow.com/questions/41553647/kotlin-isassignablefrom-and-reflection-type-checks
             */
        try {
            if (Integer.TYPE.isAssignableFrom(returnType)) {
                m.setter.call(this, Integer.parseInt(optionValue))

            } else if (java.lang.Long.TYPE.isAssignableFrom(returnType)) {
                m.setter.call(this, java.lang.Long.parseLong(optionValue))

            } else if (java.lang.Double.TYPE.isAssignableFrom(returnType)) {
                m.setter.call(this, java.lang.Double.parseDouble(optionValue))

            } else if (java.lang.Boolean.TYPE.isAssignableFrom(returnType)) {
                m.setter.call(this, parseBooleanStrict(optionValue))

            } else if (java.lang.String::class.java.isAssignableFrom(returnType)) {
                m.setter.call(this, optionValue)

            } else if (returnType.isEnum) {
                val valueOfMethod = returnType.getDeclaredMethod("valueOf",
                    java.lang.String::class.java)
                m.setter.call(this, valueOfMethod.invoke(null, optionValue))

            } else {
                throw IllegalStateException("BUG: cannot handle type $returnType")
            }
        } catch (e: Exception) {
            throw ConfigProblemException("Failed to handle property '${m.name}': ${e.message}")
        }
    }

    private fun parseBooleanStrict(s: String?) : Boolean{
        if(s==null){
            throw IllegalArgumentException("value is 'null'")
        }
        if(s.equals("true", true)) return true
        if(s.equals("false", true)) return false
        throw IllegalArgumentException("Invalid boolean value: $s")
    }
}