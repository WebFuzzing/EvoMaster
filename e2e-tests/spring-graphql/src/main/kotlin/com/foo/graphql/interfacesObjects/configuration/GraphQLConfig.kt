package com.foo.graphql.interfacesObjects.configuration

import com.foo.graphql.interfacesObjects.type.AddressFlower
import com.foo.graphql.interfacesObjects.type.AddressStore
import com.foo.graphql.interfacesObjects.type.FlowerStore
import com.foo.graphql.interfacesObjects.type.PotStore
import graphql.kickstart.tools.SchemaParserDictionary
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class GraphQLConfig() {
    @Bean
    open fun schemaParserDictionary(): SchemaParserDictionary {
        return SchemaParserDictionary()
            .add(FlowerStore::class)
            .add(PotStore::class)
            .add(AddressFlower::class)
            .add(AddressStore::class)

    }
}
