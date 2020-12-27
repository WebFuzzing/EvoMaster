package org.evomaster.core.problem.graphql.schema


data class ofType(var name: String,
                  var kind: __TypeKind,
                  var ofType: ofType) {
}