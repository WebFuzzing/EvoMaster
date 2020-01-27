package org.evomaster.core.output.service

enum class Termination(val suffix: String){
        FAULTS("_faults"),
        SUCCESSES("_successes"),
        OTHER("_others"),
        SUMMARY("_fault_representatives"),
        CLUSTERED("_clustered")
}
