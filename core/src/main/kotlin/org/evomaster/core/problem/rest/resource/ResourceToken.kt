package org.evomaster.core.problem.rest.resource

/**
 * this is used for text and name analysis with nl parser
 * @property originalText is original text before processing the analysis
 * @property lemma of [originalText]
 * @property assuredVerb assuredVerb indicates whether the token is a verb.
 * In our case, the tokens in a path is not a complete sentence, so a word may be a noun or a verb.
 * so we use assuredVerb to present that the word must be a verb.
 */
open class RToken(
    val originalText : String,
    protected val lemma : String,
    var assuredVerb : Boolean = false
){
    var isCollection: Boolean =  !assuredVerb && lemma != originalText
        private set

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

/**
 * token on resource path
 * @property level presents a level of the token on the path, e.g., /A/{a}/B/{b}, and the level of A and a is 0, B and b are 1
 * @property isParameter presents whether the token refers to a parameter, e.g., a and b are parameters for /A/{a}/B/{b}
 * @property segment presents a segment in a front of the token, e.g., /A/{a}/B/{b}/C/D/{d}, the segment of {d} is "C-D".
 *          Regarding how the segment is generated, please conduct the [parsePathTokens] of [ParserUtil]
 * @property nearestParamLevel presents a level of parameter which is in front of the token and is closest to the token
 */
class PathRToken(
        originalText : String,
        lemma : String,
        val level : Int,
        val isParameter : Boolean,
        assuredVerb : Boolean = false,
        var segment : String = "",
        var nearestParamLevel : Int = -1
) : RToken(originalText,lemma, assuredVerb){

    val subTokens = mutableListOf<PathRToken>()

    override fun copy() : RToken{
        val copy = PathRToken(originalText,lemma, level, isParameter, assuredVerb)
        copy.subTokens.addAll(subTokens.map { it.copy() as PathRToken })
        return copy
    }

    fun isStar() : Boolean = originalText.contains("*")

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
