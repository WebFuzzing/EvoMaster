package org.evomaster.core.problem.rest

enum class StatusGroup {

    G_1xx, G_2xx, G_3xx, G_4xx, G_5xx;

    fun isInGroup(status: Int?) : Boolean{
        if(status == null){
            return false
        }
        return when(this){
            G_1xx -> status in 100..199
            G_2xx -> status in 200..299
            G_3xx -> status in 300..399
            G_4xx -> status in 400..499
            G_5xx -> status in 500..599
        }
    }

    fun allInGroup(vararg status: Int?) = status.all { isInGroup(it)}
}