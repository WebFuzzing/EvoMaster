package org.evomaster.core.problem.rest2.resources.token.parser

open class RToken{
    protected val originalText : String
    protected val lemma : String
    var assuredVerb : Boolean = false
    var isCollection: Boolean = false
        private set

    constructor(originalText: String, lemma: String, assuredVerb: Boolean = false){
        this.originalText = originalText.toLowerCase()
        this.lemma = lemma.toLowerCase()
        this.assuredVerb = assuredVerb
        this.isCollection = !assuredVerb && lemma != originalText
    }

    fun equals(other : RToken):Boolean{
        return isCollection == other.isCollection && lemma == other.lemma
    }

    fun getKey() : String{
        return lemma.toLowerCase()
    }

    fun isKey(key : String) : Boolean = lemma == key.toLowerCase()

    open fun copy() : RToken{
        return RToken(originalText,lemma, assuredVerb)
    }
}
class PathRToken(
        originalText : String,
        lemma : String,
        val level : Int,
        val isParameter : Boolean,
        assuredVerb : Boolean = false
) : RToken(originalText,lemma, assuredVerb){

    override fun copy() : RToken{
        return PathRToken(originalText,lemma, level, isParameter, assuredVerb)
    }

}

/**
 * @property fromDefinition is used to represent if the token comes from definitions, i.e., type, type of the parameter, name of the parameter
 * @property isType is used to represent that the token carries type name
 * @property isDirect is used to represent that the token comes from field of the parameter or null-type object gene
 *
 * for instance, a token (isType is true, isField is true), it means that the token represents a type of a field
 */
class ActionRToken(
        originalText : String,
        lemma : String,
        var fromDefinition : Boolean,
        var isType: Boolean,
        var isDirect : Boolean = false,
        assuredVerb : Boolean = false
) : RToken(originalText,lemma, assuredVerb){

    val alternativeNames = mutableSetOf<String>()
    val fields = mutableListOf<String>()

    override fun copy() : RToken{
        return ActionRToken(originalText,lemma, fromDefinition, isType, isDirect, assuredVerb).also {
            it.alternativeNames.clear()
            it.alternativeNames.addAll(alternativeNames)
            it.fields.clear()
            it.fields.addAll(fields)
        }
    }
}


class GlobalRToken(
        //originalText : String,
        lemma : String,
        var highestlevel : Int,
        var isType: Boolean
) : RToken(lemma,lemma){

    override fun copy() : RToken{
        return PathRToken(originalText,lemma, highestlevel, isType, assuredVerb)
    }

}