package org.evomaster.core.llm

class DictionarySearchResult(

    val data: Map<String, Set<String>>,

    val missing: Set<String>
)