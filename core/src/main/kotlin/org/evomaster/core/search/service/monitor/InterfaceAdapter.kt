package org.evomaster.core.search.service

import com.google.gson.*
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.search.Action
import org.evomaster.core.search.gene.Gene
import java.lang.reflect.Constructor
import java.lang.reflect.Parameter

import java.lang.reflect.Type
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.javaType
import kotlin.reflect.jvm.jvmErasure


class InterfaceAdapter<T : Any> : JsonSerializer<T>, JsonDeserializer<T> {



    companion object {
        private const val CLASSNAME = "CLASSNAME"
        private const val DATA = "DATA"
        private var fake : JsonObject? = null

        //TODO it is required to fix for solving ArrayGene with Generic type
        private fun getFakeJson() : JsonObject{
            return  JsonObject().apply {
                addProperty("maxForRandomizantion", "16")
                addProperty("value", "fake")
                addProperty("minLength", "0")
                addProperty("maxLength", "16")
                addProperty("name", "fakeArrayGene")
            }

        }
    }

    init {
        fake = getFakeJson()
    }

    override fun deserialize(jsonElement: JsonElement, type: Type, jsonDeserializationContext: JsonDeserializationContext): T {
        val jsonObject = jsonElement.asJsonObject

        //FIXME MAN: temp solution for Action
        if(type.typeName.equals(Action::class.java.name) ){
            return jsonDeserializationContext.deserialize(jsonObject, RestCallAction::class.java)
        }
        val klass = getObjectClass((jsonObject.get(CLASSNAME) as JsonPrimitive).asString)

        //FIXME MAN : the solution should be not hardcode as "gene" attribute.
        val data =jsonObject.get(DATA).asJsonObject
        if(containGenProperty(klass)){
            return deserializeGenProperty(data, klass, jsonDeserializationContext) as T
        }
        return jsonDeserializationContext.deserialize(data, klass)
    }

    override fun serialize(jsonElement: T, type: Type, jsonSerializationContext: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject().apply {
            addProperty(CLASSNAME, jsonElement.javaClass.name)
        }
        val data = jsonSerializationContext.serialize(jsonElement)

        if(jsonElement is Gene){
            // FIXME MAN : the solution should be not hardcode as "gene" attribute.
            // For some reasons, some genes are generic type, but some of them are not, such as SQL.
            var dprops = jsonElement::class.declaredMemberProperties.filter {  it.name.equals("gene") }

            if(dprops.size == 1){
                val prop = dprops[0]
                if(prop.visibility== KVisibility.PUBLIC){
                    var obj = prop.getter.call(jsonElement)
                    var geneJsonObject = JsonObject()
                    geneJsonObject.addProperty(CLASSNAME, obj!!.javaClass.name)
                    geneJsonObject.add(DATA, jsonSerializationContext.serialize(obj))
                    data.asJsonObject.add("gene", geneJsonObject)
                }
            }

        }
        jsonObject.add(DATA, data)
        return jsonObject
    }

    private fun getObjectClass(className: String): Class<*> {
        try {
            return Class.forName(className)
        } catch (e: ClassNotFoundException) {
            throw JsonParseException(e.message)
        }
    }


    private fun <T> createEntity(clazz: Class<*> , constructor: Constructor<T>, vararg args: Any) : T {
        if (args.size > 1){
            val seq = manipulateSeqOfArgs(getPropertyies(clazz), constructor.parameters)
            return constructor.newInstance(*Array(seq.size){
                i -> args[seq[i]]
            })

        }else
            return constructor.newInstance(*args)
    }

    //TODO it is dangerous when more than one types of params of constructor are same
    private fun manipulateSeqOfArgs(props: MutableList<KProperty1<out Any, Any?>>, params : Array<out Parameter>): Array<Int>  {

        //TODO MAN: to handle params.size > props.size if possible
        return Array<Int>(params.size){
            i -> props.indexOf( props.first {
            p -> params[i].type.kotlin.isSubclassOf(p.returnType.jvmErasure)
                || params[i].type.kotlin.qualifiedName.equals(p.returnType.jvmErasure.qualifiedName) })
        }
    }

    private fun getPropertyies(klass: Class<*>) : MutableList<KProperty1<out Any, Any?>> {
        val props = mutableListOf<KProperty1<out Any, Any?>>()
        klass.kotlin.allSuperclasses.toMutableList().forEach {
            t-> run{
                props.addAll(t.declaredMemberProperties.toMutableList())
            }
        }
        props.addAll(klass.kotlin.declaredMemberProperties.toMutableList())
        return props
    }

    private fun deserializeGenProperty( data:JsonObject, clazz: Class<*>,jsonDeserializationContext: JsonDeserializationContext) : Any {
        val props = getPropertyies(clazz)
        val elements = Array<Any>(props.size){
            i-> run{
            if(props[i].name.equals("gene")){
                var genJsonObject = data.get(props[i].name).asJsonObject
                val genClazzName = (genJsonObject.get(CLASSNAME) as JsonPrimitive).asString
                val genClazz = getObjectClass(genClazzName)
                if(containGenProperty(genClazz))
                    deserializeGenProperty(genJsonObject.get(DATA).asJsonObject, genClazz, jsonDeserializationContext)
                else if(genClazzName.contains("ArrayGene")){
                    jsonDeserializationContext.deserialize(fake!!, getObjectClass("org.evomaster.core.search.gene.StringGene"))
                }else
                    jsonDeserializationContext.deserialize(genJsonObject.get(DATA), genClazz)
            }else{
                jsonDeserializationContext.deserialize(data.get(props[i].name), props[i].returnType.javaType)
            }
            }
        }
        var con = clazz.constructors.filter { c -> c.parameters!!.size == props.size || c.parameterCount == props.size }

        //FIXME MAN: c.parameters > props.size cannot be handled if it exists
        return createEntity(clazz, if(con.isEmpty()) clazz.constructors.first() else con.first(), *elements)
    }

    private fun containGenProperty(clazz: Class<*>) : Boolean{
        return getPropertyies(clazz).filter { it.name.equals("gene") }.size == 1
    }

}
