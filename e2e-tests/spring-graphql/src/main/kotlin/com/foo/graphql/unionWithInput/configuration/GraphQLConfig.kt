package com.foo.graphql.unionWithInput.configuration


import com.foo.graphql.unionWithInput.type.Flower
import com.foo.graphql.unionWithInput.type.Pot
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
