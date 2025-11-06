package com.foo.graphql.unionWithObject.configuration

import com.foo.graphql.unionWithObject.type.Flower
import com.foo.graphql.unionWithObject.type.Pot
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
