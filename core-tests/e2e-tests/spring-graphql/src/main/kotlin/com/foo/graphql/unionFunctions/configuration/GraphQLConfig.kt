package com.foo.graphql.unionFunctions.configuration

import com.foo.graphql.unionFunctions.type.Flower
import com.foo.graphql.unionFunctions.type.Pot
import graphql.kickstart.tools.SchemaParserDictionary
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class GraphQLConfig() {
    @Bean
 open   fun schemaParserDictionary(): SchemaParserDictionary {
        return SchemaParserDictionary()
                .add(Flower::class)
                .add(Pot::class)

    }
}
