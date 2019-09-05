package org.evomaster.resource.rest.generator.template

/**
 * created by manzh on 2019-08-20
 */
class RegisterType (constantTypes : ConstantTypeScript, clazz : Set<String>){

    private val types : MutableMap<String, String> = mutableMapOf()

    init {
        constantTypes.getAllCommonTypes().forEach { (t, u) ->
            registerType(t.name, u)
        }

        constantTypes.getTypes().forEach { (t, u) ->
            registerType(t, u)
        }

        clazz.forEach {
            registerType(it, it)
        }
    }

    private fun registerType(key : String, value: String){
        if (types.containsKey(key))
            throw IllegalArgumentException("duplicate type ${key}")
        types.putIfAbsent(key, value)
    }

    fun getType(type : String) : String{
        validType(type)
        return types.getValue(type)
    }

    fun validType(type: String) {
        if (!types.containsKey(type))
            throw IllegalArgumentException("$type does not exist")
    }
}