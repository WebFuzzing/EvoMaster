package org.evomaster.experiments.objects

import com.google.inject.Inject
import org.evomaster.core.search.service.Randomness


class ObjProblemDefinition {


    @Inject
    lateinit var randomness : Randomness

    var range = 1000
}