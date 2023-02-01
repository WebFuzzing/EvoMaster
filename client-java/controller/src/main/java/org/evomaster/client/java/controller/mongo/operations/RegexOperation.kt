package org.evomaster.client.java.controller.mongo.operations

import java.util.regex.Pattern

open class RegexOperation (open val pattern: Pattern, open val options: Array<Char>) : QueryOperation()