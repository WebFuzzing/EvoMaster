package org.evomaster.core.problem.httpws.auth

class PlaceHolderResolver(
    val name: String,
    /**
     * Map from (key) placeholder to (value) newly created string that will replace it
     */
    val placeHolders: Map<String, String>
)