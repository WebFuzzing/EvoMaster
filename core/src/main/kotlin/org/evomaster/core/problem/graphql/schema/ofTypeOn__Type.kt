package org.evomaster.core.problem.graphql.schema


data class ofTypeOn__Type(var name: String,
                          var kind: __TypeKind,
                          var ofType: ofTypeOn__Type) {
}