package org.evomaster.core.llm.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.evomaster.core.llm.DictionarySearchResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class DictionaryService {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(DictionaryService::class.java)
        private const val location = "/llm_dictionary.jsonl"

        /**
         * WARNING: possibly quite expensive... should only be used for debugging/testing/statistics
         */
        fun loadAll() : Map<String, Set<String>> {

            val data = mutableMapOf<String, Set<String>>()
            val mapper = ObjectMapper()

            val reader = DictionaryService::class.java.getResourceAsStream(location)!!.bufferedReader()

            reader.forEachLine { line ->
                addLineEntry(mapper, line, data)
            }

            return data
        }


        fun searchForNames(names: Collection<String>): DictionarySearchResult {

            if(names.isEmpty()) {
                throw IllegalArgumentException("No name to find is specified")
            }

            val found = mutableMapOf<String, Set<String>>()
            val missing = mutableSetOf<String>()
            val result = DictionarySearchResult(found, missing)
            val mapper = ObjectMapper()

            val reader = DictionaryService::class.java.getResourceAsStream(location)!!.bufferedReader()

            val toFind = names.toList().sorted()
            var index = 0

            reader.lineSequence()
                .takeWhile { index < toFind.size }
                .forEach { line ->

                    //let's avoid parsing whole line with Jackson if we do not have a name-match
                    val startQuote = line.indexOf('"')
                    val endQuote = line.indexOf('"', startQuote+1)
                    val x = line.substring(startQuote+1, endQuote)

                    var handled = false

                    while(!handled && index < toFind.size) {
                        val f = toFind[index]

                        if (f == x) {
                            addLineEntry(mapper, line, found)
                            index++
                            handled = true
                        } else if (x < f) {
                            //nothing to do. f is not found, but could be found in the next lines
                            //recall names and dictionary is alphabetically sorted
                            handled = true
                        } else {
                            missing.add(f)
                            index++
                        }
                    }
            }

            // in case any asked name come alphabetically after the last element in the dictionary
            while(index < toFind.size) {
                missing.add(toFind[index])
                index++
            }

            //would not hold if there are errors in parsing... but then if so we should remove those from dictionary!!!
            assert(names.size == result.data.size + result.missing.size)
            return result
        }

        private fun addLineEntry(
            mapper: ObjectMapper,
            line: String,
            found: MutableMap<String, Set<String>>
        ) {
            val node = mapper.readTree(line)
            if (!node.isObject) {
                log.warn("Not an object: $line")
            } else {
                node.fields().forEach { field ->
                    if (!field.value.isArray) {
                        log.warn("Not containing an array: $line")
                    } else {
                        found[field.key] = mapper.convertValue(field.value, object : TypeReference<Set<String>>() {})
                    }
                }
            }
        }
    }



}