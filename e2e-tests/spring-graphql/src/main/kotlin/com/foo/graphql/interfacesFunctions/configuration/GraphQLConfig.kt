package com.foo.graphql.interfacesFunctions.configuration

import com.foo.graphql.interfacesFunctions.type.FlowerStore
import com.foo.graphql.interfacesFunctions.type.PotStore
import graphql.kickstart.tools.SchemaParserDictionary
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class GraphQLConfig() {
    @Bean
 open   fun schemaParserDictionary(): SchemaParserDictionary {
        return SchemaParserDictionary()
                .add(FlowerStore::class)
                .add(PotStore::class)

    }
}
