package org.evomaster.core.search

import org.evomaster.core.search.gene.collection.EnumGene
import org.junit.jupiter.api.AfterEach

abstract class SearchTestBase {

    @AfterEach
    fun cleanCaches(){
        EnumGene.cleanCache()
    }
}