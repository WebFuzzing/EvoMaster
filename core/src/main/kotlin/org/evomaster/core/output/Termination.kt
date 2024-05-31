package org.evomaster.core.output

enum class Termination(
        val suffix: String,
        val comment: String
){
        FAULTS("_faults", "This file contains test cases that are likely to indicate faults."),
        SUCCESSES("_successes", "This file contains test cases that represent successful calls."),
        OTHERS("_others", "This file contains test cases that represent failed calls, but not indicative of faults."),
        FAULT_REPRESENTATIVES("_fault_representatives", "This file contains one example of each category of fault. The test cases in this file are a subset of the set of test cases likely to indicate faults."),
        //TODO why is this not part of FAULTS or "OTHERS"?
        EXCEPTIONS("_exceptions", "This file contains test cases which throws exceptions."),
        SEEDING("_seeding", "This file contains test cases generated during seeding."),
        NONE("","")
}