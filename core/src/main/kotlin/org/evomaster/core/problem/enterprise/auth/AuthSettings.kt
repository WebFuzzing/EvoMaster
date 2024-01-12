package org.evomaster.core.problem.enterprise.auth


/**
 * Class used to store and handle all Auth Info for the current SUT
 */
class AuthSettings(authInfos: List<AuthenticationInfo> = listOf()) {

    private val auths = mutableListOf<AuthenticationInfo>()
    init {
        //TODO check unique names

        auths.addAll(authInfos)
    }

    fun addInfo(info: AuthenticationInfo){
        if(auths.any { it.name == info.name }){
            throw IllegalArgumentException("Auth configuration with name '${info.name} already exists")
        }
        auths.add(info)
    }

    fun <T : AuthenticationInfo> getOfType(klass: Class<T>) = auths.filterIsInstance(klass)


    fun <T : AuthenticationInfo> isNotEmpty(klass: Class<T>) = auths.any { klass.isAssignableFrom(it.javaClass) }

    fun isNotEmpty() = auths.isNotEmpty()

    fun size() = auths.size
}