package org.evomaster.core.problem.enterprise

import com.webfuzzing.commons.faults.DefinedFaultCategory

object  EmployedOracles {

    private val categoryCodes = usedCategories().map { it.code }.toSet()

    fun hasCategory(code: Int) = categoryCodes.contains(code)

    /**
     * EvoMaster does not necessarily feature all the categories of oracles
     * defined in WFC, although we aim to implement them all in the long run
     */
    fun usedCategories() : List<DefinedFaultCategory> {
        return listOf(
            DefinedFaultCategory.HTTP_STATUS_500 ,
            DefinedFaultCategory.HTTP_STATUS_NO_NON_STANDARD_CODES ,
            DefinedFaultCategory.HTTP_STATUS_NO_201_IF_DELETE ,
            DefinedFaultCategory.HTTP_STATUS_NO_201_IF_GET ,
            DefinedFaultCategory.HTTP_STATUS_NO_201_IF_PATCH ,
            DefinedFaultCategory.HTTP_STATUS_NO_204_IF_CONTENT ,
            DefinedFaultCategory.HTTP_STATUS_NO_413_IF_NO_PAYLOAD ,
            DefinedFaultCategory.HTTP_STATUS_NO_415_IF_NO_PAYLOAD ,
            DefinedFaultCategory.HTTP_STATUS_NO_304_IF_NO_GET_OR_HEAD ,
            DefinedFaultCategory.HTTP_STATUS_NO_401_IF_NO_WWW_AUTHENTICATE ,
            DefinedFaultCategory.HTTP_STATUS_NO_405_IF_NO_ALLOW ,
            DefinedFaultCategory.HTTP_STATUS_NO_205_IF_CONTENT ,
            DefinedFaultCategory.HTTP_STATUS_NO_426_IF_NO_UPGRADE ,
            DefinedFaultCategory.HTTP_NONWORKING_DELETE ,
            DefinedFaultCategory.HTTP_SIDE_EFFECTS_FAILED_MODIFICATION ,
            DefinedFaultCategory.HTTP_REPEATED_CREATE_PUT ,
            DefinedFaultCategory.HTTP_MISLEADING_CREATE_PUT ,
            DefinedFaultCategory.HTTP_PARTIAL_UPDATE_PUT ,
            DefinedFaultCategory.HTTP_NON_IDEMPOTENT_PUT ,
            DefinedFaultCategory.HTTP_INVALID_MERGE_PATCH ,
            DefinedFaultCategory.HTTP_INVALID_LOCATION ,
            DefinedFaultCategory.SCHEMA_INVALID_RESPONSE ,
            DefinedFaultCategory.SCHEMA_INVALID_ALLOW ,
            DefinedFaultCategory.SCHEMA_STATUS_NO_401_IF_NO_AUTH ,
            DefinedFaultCategory.SCHEMA_STATUS_NO_403_IF_NO_401 ,
            DefinedFaultCategory.SCHEMA_STATUS_HAS_406_IF_ACCEPT ,
            DefinedFaultCategory.SCHEMA_STATUS_NO_501_IF_IMPLEMENTED ,
            DefinedFaultCategory.SECURITY_SQL_INJECTION ,
            DefinedFaultCategory.SECURITY_XSS ,
            DefinedFaultCategory.SECURITY_SSRF ,
            DefinedFaultCategory.SECURITY_EXISTENCE_LEAKAGE ,
            DefinedFaultCategory.SECURITY_NOT_RECOGNIZED_AUTHENTICATED ,
            DefinedFaultCategory.SECURITY_WRONG_AUTHORIZATION ,
            DefinedFaultCategory.SECURITY_IGNORE_ANONYMOUS ,
            DefinedFaultCategory.SECURITY_ANONYMOUS_MODIFICATIONS ,
            DefinedFaultCategory.SECURITY_LEAKED_STACK_TRACES ,
            DefinedFaultCategory.SECURITY_HIDDEN_ACCESSIBLE_ENDPOINT
        )
        /*
            Currently skipped:
            SCHEMA_VALIDATION_BYPASS
            SECURITY_MASS_ASSIGNMENT
         */
    }
}