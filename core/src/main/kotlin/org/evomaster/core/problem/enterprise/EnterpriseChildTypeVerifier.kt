package org.evomaster.core.problem.enterprise

import org.evomaster.core.mongo.MongoDbAction
import org.evomaster.core.problem.externalservice.HostnameResolutionAction
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.action.ActionComponent
import org.evomaster.core.sql.SqlAction

 class  EnterpriseChildTypeVerifier(
     /**
      * The main action type for this enterprise individual, like REST, GraphQL and RPC actions
      */
     private val mainType : Class<out Action>,
     /**
      * A possible secondary type, for intermediate representation, like Resource Based REST.
      */
    private val secondaryType: Class<out ActionComponent>? = null
) : (Class<*>) -> Boolean {

     private fun isInitializingAction(t: Class<*>) :  Boolean{
         return SqlAction::class.java.isAssignableFrom(t)
                 || MongoDbAction::class.java.isAssignableFrom(t)
                 || HostnameResolutionAction::class.java.isAssignableFrom(t)
     }

     override fun invoke(t: Class<*>): Boolean {
         //TODO use mainType
         return ( EnterpriseActionGroup::class.java.isAssignableFrom(t)
                 || isInitializingAction(t)
                 || (secondaryType != null && secondaryType.isAssignableFrom(t)))
     }
 }